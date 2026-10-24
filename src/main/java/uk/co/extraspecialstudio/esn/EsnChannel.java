package uk.co.extraspecialstudio.esn;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A named network channel owned by a consumer mod (e.g. {@code radiotowers}).
 */
public interface EsnChannel {

    String ownerModId();

    String protocolVersion();

    /**
     * Register a packet type on this channel.
     *
     * @param path          path under the owner mod namespace (e.g. {@code lobby_invite})
     * @param type          packet class
     * @param encoder       write packet
     * @param decoder       read packet
     * @param clientHandler run on client (logical); may be null if server-only
     * @param serverHandler run on server with the sending player; may be null if client-only
     */
    <T> void register(
        String path,
        Class<T> type,
        BiConsumer<T, PacketBuffer> encoder,
        Function<PacketBuffer, T> decoder,
        BiConsumer<T, Supplier<EsnClientContext>> clientHandler,
        BiConsumer<T, Supplier<EsnServerContext>> serverHandler
    );

    void sendToServer(Object packet);

    void sendTo(ServerPlayerEntity player, Object packet);

    void sendTo(Collection<ServerPlayerEntity> players, Object packet);
}
