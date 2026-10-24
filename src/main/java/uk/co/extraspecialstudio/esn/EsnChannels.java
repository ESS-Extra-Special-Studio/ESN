package uk.co.extraspecialstudio.esn;

import net.minecraft.util.ResourceLocation;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link EsnChannel} instances keyed by owning mod id.
 */
public final class EsnChannels {
    private static final Map<String, EsnChannel> CHANNELS = new ConcurrentHashMap<>();

    private EsnChannels() {
    }

    /**
     * Register (or return existing) channel for {@code ownerModId}.
     * Protocol must match on client and server.
     */
    public static EsnChannel register(String ownerModId, String protocolVersion) {
        Objects.requireNonNull(ownerModId, "ownerModId");
        Objects.requireNonNull(protocolVersion, "protocolVersion");
        return CHANNELS.compute(ownerModId, (id, existing) -> {
            if (existing != null) {
                if (!existing.protocolVersion().equals(protocolVersion)) {
                    throw new IllegalStateException(
                        "ESN channel '" + id + "' already registered with protocol "
                            + existing.protocolVersion() + ", not " + protocolVersion);
                }
                return existing;
            }
            ForgeEsnChannel channel = new ForgeEsnChannel(id, protocolVersion);
            channel.init();
            EsnMod.LOGGER.info("[ESN] Channel registered for {} (protocol {})", id, protocolVersion);
            return channel;
        });
    }

    public static EsnChannel get(String ownerModId) {
        EsnChannel channel = CHANNELS.get(ownerModId);
        if (channel == null) {
            throw new IllegalStateException("No ESN channel registered for mod id: " + ownerModId);
        }
        return channel;
    }

    /** Channel SimpleChannel name under the {@code esn} namespace (lowercase-safe). */
    static ResourceLocation channelId(String ownerModId) {
        String path = ("ch_" + ownerModId).toLowerCase(Locale.ROOT).replace(' ', '_');
        ResourceLocation id = ResourceLocation.tryParse(EsnMod.MOD_ID + ":" + path);
        if (id == null) {
            throw new IllegalArgumentException("Invalid ESN channel id for ownerModId: " + ownerModId);
        }
        return id;
    }

    static ResourceLocation packetId(String ownerModId, String path) {
        String ns = ownerModId.toLowerCase(Locale.ROOT);
        String p = path.toLowerCase(Locale.ROOT).replace(' ', '_');
        ResourceLocation id = ResourceLocation.tryParse(ns + ":" + p);
        if (id == null) {
            throw new IllegalArgumentException("Invalid ESN packet id: " + ns + ":" + p);
        }
        return id;
    }
}
