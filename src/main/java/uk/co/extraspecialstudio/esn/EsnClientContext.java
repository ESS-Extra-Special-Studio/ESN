package uk.co.extraspecialstudio.esn;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.world.World;

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
    public ClientPlayerEntity player() {
        return minecraft.player;
    }

    @Nullable
    public World level() {
        return minecraft.level;
    }
}
