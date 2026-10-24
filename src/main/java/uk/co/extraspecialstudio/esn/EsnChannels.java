package uk.co.extraspecialstudio.esn;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

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
     * <p>
     * Call from the consumer mod constructor (before {@link RegisterPayloadHandlersEvent}).
     * Packet {@link EsnChannel#register} calls are queued and flushed during that event.
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
            NeoEsnChannel channel = new NeoEsnChannel(id, protocolVersion);
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

    static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        for (EsnChannel channel : CHANNELS.values()) {
            if (channel instanceof NeoEsnChannel neo) {
                neo.flush(event);
            }
        }
    }

    static ResourceLocation packetId(String ownerModId, String path) {
        String ns = ownerModId.toLowerCase(java.util.Locale.ROOT);
        String p = path.toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
        return new ResourceLocation(ns, p);
    }
}
