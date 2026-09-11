package uk.co.extraspecialstudio.esn;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

/** Server-side receive context for ESN packets. */
public final class EsnServerContext {
    private final ServerPlayer sender;

    public EsnServerContext(ServerPlayer sender) {
        this.sender = sender;
    }

    public ServerPlayer sender() {
        return sender;
    }

    public ServerLevel level() {
        return sender.serverLevel();
    }

    public MinecraftServer server() {
        return sender.getServer();
    }
}
