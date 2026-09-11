package uk.co.extraspecialstudio.esn;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** Client-side receive context for ESN packets. */
public final class EsnClientContext {
    private final Minecraft minecraft;

    public EsnClientContext(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public Minecraft minecraft() {
        return minecraft;
    }

    @Nullable
    public LocalPlayer player() {
        return minecraft.player;
    }

    @Nullable
    public Level level() {
        return minecraft.level;
    }
}
