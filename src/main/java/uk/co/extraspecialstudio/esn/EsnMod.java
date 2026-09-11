package uk.co.extraspecialstudio.esn;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Extra Special Network — shared packet / channel fabric.
 * No lobby, wave, airdrop, or HUD types belong here.
 */
@Mod(EsnMod.MOD_ID)
public final class EsnMod {
    public static final String MOD_ID = "esn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EsnMod(IEventBus modEventBus) {
        modEventBus.addListener(EsnChannels::onRegisterPayloadHandlers);
        LOGGER.info("Extra Special Network (ESN) loaded");
    }
}
