package uk.co.extraspecialstudio.esn;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * NeoForge 26.1 implementation of {@link EsnChannel} backed by {@link PayloadRegistrar}.
 * Bidirectional packets register a server handler and a client handler separately.
 * Serverbound sends use {@link ClientPacketDistributor}.
 */
final class NeoEsnChannel implements EsnChannel {
    private final String ownerModId;
    private final String protocolVersion;
    private final List<PendingRegistration<?>> pending = new ArrayList<>();
    private final Map<Class<?>, Binding<?>> byClass = new ConcurrentHashMap<>();
    private boolean flushed;

    NeoEsnChannel(String ownerModId, String protocolVersion) {
        this.ownerModId = ownerModId;
        this.protocolVersion = protocolVersion;
    }

    void init() {
        // Payload types are flushed on RegisterPayloadHandlersEvent.
    }

    void flush(RegisterPayloadHandlersEvent event) {
        if (flushed) {
            return;
        }
        PayloadRegistrar registrar = event.registrar(protocolVersion);
        for (PendingRegistration<?> registration : pending) {
            registration.apply(registrar);
        }
        flushed = true;
        EsnMod.LOGGER.debug("[ESN] Flushed {} packet type(s) for {}", pending.size(), ownerModId);
    }

    @Override
    public String ownerModId() {
        return ownerModId;
    }

    @Override
    public String protocolVersion() {
        return protocolVersion;
    }

    @Override
    public <T> void register(
        String path,
        Class<T> type,
        BiConsumer<T, FriendlyByteBuf> encoder,
        Function<FriendlyByteBuf, T> decoder,
        BiConsumer<T, Supplier<EsnClientContext>> clientHandler,
        BiConsumer<T, Supplier<EsnServerContext>> serverHandler
    ) {
        if (flushed) {
            throw new IllegalStateException(
                "Cannot register ESN packet after RegisterPayloadHandlersEvent: "
                    + ownerModId + "/" + path);
        }
        if (clientHandler == null && serverHandler == null) {
            throw new IllegalArgumentException("At least one of clientHandler/serverHandler required");
        }
        Identifier id = EsnChannels.packetId(ownerModId, path);
        CustomPacketPayload.Type<EsnEnvelope> payloadType = new CustomPacketPayload.Type<>(id);
        Binding<T> binding = new Binding<>(type, payloadType, encoder, decoder, clientHandler, serverHandler);
        byClass.put(type, binding);
        pending.add(new PendingRegistration<>(binding));
        EsnMod.LOGGER.debug("[ESN] {}.{} queued as payload {}", ownerModId, path, id);
    }

    @Override
    public void sendToServer(Object packet) {
        ClientPacketDistributor.sendToServer(envelope(packet));
    }

    @Override
    public void sendTo(ServerPlayer player, Object packet) {
        PacketDistributor.sendToPlayer(player, envelope(packet));
    }

    @Override
    public void sendTo(Collection<ServerPlayer> players, Object packet) {
        EsnEnvelope envelope = envelope(packet);
        for (ServerPlayer player : players) {
            if (player != null) {
                PacketDistributor.sendToPlayer(player, envelope);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private EsnEnvelope envelope(Object packet) {
        if (packet == null) {
            throw new IllegalArgumentException(
                "Packet type not registered on ESN channel " + ownerModId + ": null");
        }
        Binding<?> binding = byClass.get(packet.getClass());
        if (binding == null) {
            throw new IllegalArgumentException(
                "Packet type not registered on ESN channel " + ownerModId + ": "
                    + packet.getClass().getName());
        }
        return ((Binding<Object>) binding).toEnvelope(packet);
    }

    private record PendingRegistration<T>(Binding<T> binding) {
        void apply(PayloadRegistrar registrar) {
            StreamCodec<RegistryFriendlyByteBuf, EsnEnvelope> codec = StreamCodec.of(
                (buf, envelope) -> binding.encoder.accept(binding.cast(envelope.packet()), buf),
                buf -> binding.toEnvelope(binding.decoder.apply(buf))
            );
            if (binding.clientHandler != null && binding.serverHandler != null) {
                registrar.playBidirectional(binding.payloadType, codec, binding::handleServer, binding::handleClient);
            } else if (binding.serverHandler != null) {
                registrar.playToServer(binding.payloadType, codec, binding::handleServer);
            } else {
                registrar.playToClient(binding.payloadType, codec, binding::handleClient);
            }
        }
    }

    private record Binding<T>(
        Class<T> packetClass,
        CustomPacketPayload.Type<EsnEnvelope> payloadType,
        BiConsumer<T, FriendlyByteBuf> encoder,
        Function<FriendlyByteBuf, T> decoder,
        BiConsumer<T, Supplier<EsnClientContext>> clientHandler,
        BiConsumer<T, Supplier<EsnServerContext>> serverHandler
    ) {
        EsnEnvelope toEnvelope(T packet) {
            return new EsnEnvelope(payloadType, packet);
        }

        @SuppressWarnings("unchecked")
        T cast(Object packet) {
            return (T) packet;
        }

        void handleServer(EsnEnvelope envelope, IPayloadContext ctx) {
            T packet = cast(envelope.packet());
            ctx.enqueueWork(() -> {
                if (serverHandler != null && ctx.player() instanceof ServerPlayer sender) {
                    serverHandler.accept(packet, () -> new EsnServerContext(sender));
                }
            });
        }

        void handleClient(EsnEnvelope envelope, IPayloadContext ctx) {
            T packet = cast(envelope.packet());
            ctx.enqueueWork(() -> {
                if (clientHandler != null) {
                    clientHandler.accept(packet, () -> new EsnClientContext(Minecraft.getInstance()));
                }
            });
        }
    }

    private record EsnEnvelope(CustomPacketPayload.Type<EsnEnvelope> payloadType, Object packet)
        implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return payloadType;
        }
    }
}
