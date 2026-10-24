package uk.co.extraspecialstudio.esn;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;

/** Server-side receive context for ESN packets. */
public final class EsnServerContext {
    private final ServerPlayerEntity sender;

    public EsnServerContext(ServerPlayerEntity sender) {
        this.sender = sender;
    }

    public ServerPlayerEntity sender() {
        return sender;
    }

    public ServerWorld level() {
        return sender.getLevel();
    }

    public MinecraftServer server() {
        return sender.getServer();
    }
}
