package uk.co.extraspecialstudio.esn;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Forge 1.16.5 implementation of {@link EsnChannel} backed by {@link SimpleChannel}.
 */
final class ForgeEsnChannel implements EsnChannel {
    private final String ownerModId;
    private final String protocolVersion;
    private final AtomicInteger nextId = new AtomicInteger(0);
    private final Map<Class<?>, Integer> classToId = new ConcurrentHashMap<>();
    private SimpleChannel channel;

    ForgeEsnChannel(String ownerModId, String protocolVersion) {
        this.ownerModId = ownerModId;
        this.protocolVersion = protocolVersion;
    }

    void init() {
        ResourceLocation name = EsnChannels.channelId(ownerModId);
        this.channel = NetworkRegistry.newSimpleChannel(
            name,
            () -> protocolVersion,
            protocolVersion::equals,
            protocolVersion::equals
        );
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
        BiConsumer<T, PacketBuffer> encoder,
        Function<PacketBuffer, T> decoder,
        BiConsumer<T, Supplier<EsnClientContext>> clientHandler,
        BiConsumer<T, Supplier<EsnServerContext>> serverHandler
    ) {
        int id = nextId.getAndIncrement();
        classToId.put(type, id);
        channel.registerMessage(
            id,
            type,
            encoder::accept,
            decoder::apply,
            (msg, ctxSupplier) -> {
                NetworkEvent.Context ctx = ctxSupplier.get();
                ctx.enqueueWork(() -> {
                    if (ctx.getDirection().getReceptionSide().isClient()) {
                        if (clientHandler != null) {
                            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                                clientHandler.accept(msg, () -> new EsnClientContext(Minecraft.getInstance())));
                        }
                    } else {
                        ServerPlayerEntity sender = ctx.getSender();
                        if (serverHandler != null && sender != null) {
                            serverHandler.accept(msg, () -> new EsnServerContext(sender));
                        }
                    }
                });
                ctx.setPacketHandled(true);
            }
        );
        EsnMod.LOGGER.debug("[ESN] {}.{} registered as message id {}", ownerModId, path, id);
    }

    @Override
    public void sendToServer(Object packet) {
        ensureKnown(packet);
        channel.sendToServer(packet);
    }

    @Override
    public void sendTo(ServerPlayerEntity player, Object packet) {
        ensureKnown(packet);
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @Override
    public void sendTo(Collection<ServerPlayerEntity> players, Object packet) {
        ensureKnown(packet);
        for (ServerPlayerEntity player : players) {
            if (player != null) {
                channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    private void ensureKnown(Object packet) {
        if (packet == null || !classToId.containsKey(packet.getClass())) {
            throw new IllegalArgumentException(
                "Packet type not registered on ESN channel " + ownerModId + ": "
                    + (packet == null ? "null" : packet.getClass().getName()));
        }
        if (classToId.get(packet.getClass()) == null) {
            throw new IllegalStateException("Missing message id for " + packet.getClass());
        }
    }
}
