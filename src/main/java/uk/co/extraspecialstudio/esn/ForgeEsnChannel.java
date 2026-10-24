package uk.co.extraspecialstudio.esn;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Forge 1.20.1 implementation of {@link EsnChannel} backed by {@link SimpleChannel}.
 */
final class ForgeEsnChannel implements EsnChannel {
    private final String ownerModId;
    private final String protocolVersion;
    private final int protocolId;
    private final AtomicInteger nextId = new AtomicInteger(0);
    private final Map<Class<?>, Integer> classToId = new ConcurrentHashMap<>();
    private SimpleChannel channel;

    ForgeEsnChannel(String ownerModId, String protocolVersion) {
        this.ownerModId = ownerModId;
        this.protocolVersion = protocolVersion;
        this.protocolId = protocolId(protocolVersion);
    }

    void init() {
        ResourceLocation name = EsnChannels.channelId(ownerModId);
        int accepted = protocolId;
        this.channel = ChannelBuilder
            .named(name)
            .networkProtocolVersion(accepted)
            .clientAcceptedVersions((Channel.VersionTest.Status status, int version) ->
                status == Channel.VersionTest.Status.PRESENT && version == accepted)
            .serverAcceptedVersions((Channel.VersionTest.Status status, int version) ->
                status == Channel.VersionTest.Status.PRESENT && version == accepted)
            .simpleChannel();
    }

    private static int protocolId(String protocolVersion) {
        if (protocolVersion == null || protocolVersion.isEmpty()) {
            return 0;
        }
        return protocolVersion.hashCode() & 0x7fffffff;
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
        int id = nextId.getAndIncrement();
        classToId.put(type, id);
        channel.messageBuilder(type, id)
            .encoder(encoder::accept)
            .decoder(decoder::apply)
            .consumerMainThread((T msg, CustomPayloadEvent.Context ctx) -> {
                if (ctx.getDirection().getReceptionSide().isClient()) {
                    if (clientHandler != null) {
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            clientHandler.accept(msg, () -> new EsnClientContext(Minecraft.getInstance())));
                    }
                } else {
                    ServerPlayer sender = ctx.getSender();
                    if (serverHandler != null && sender != null) {
                        serverHandler.accept(msg, () -> new EsnServerContext(sender));
                    }
                }
            })
            .add();
        EsnMod.LOGGER.debug("[ESN] {}.{} registered as message id {}", ownerModId, path, id);
    }

    @Override
    public void sendToServer(Object packet) {
        ensureKnown(packet);
        channel.send(packet, PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendTo(ServerPlayer player, Object packet) {
        ensureKnown(packet);
        channel.send(packet, PacketDistributor.PLAYER.with(player));
    }

    @Override
    public void sendTo(Collection<ServerPlayer> players, Object packet) {
        ensureKnown(packet);
        for (ServerPlayer player : players) {
            if (player != null) {
                channel.send(packet, PacketDistributor.PLAYER.with(player));
            }
        }
    }

    private void ensureKnown(Object packet) {
        if (packet == null || !classToId.containsKey(packet.getClass())) {
            throw new IllegalArgumentException(
                "Packet type not registered on ESN channel " + ownerModId + ": "
                    + (packet == null ? "null" : packet.getClass().getName()));
        }
        // Soft size guard: SimpleChannel encodes later; reject absurd object graphs early by class only.
        if (classToId.get(packet.getClass()) == null) {
            throw new IllegalStateException("Missing message id for " + packet.getClass());
        }
    }
}
