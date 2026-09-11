package uk.co.extraspecialstudio.esn;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Extra Special Network — shared packet / channel fabric.
 * No lobby, wave, airdrop, or HUD types belong here.
 */
@Mod(EsnMod.MOD_ID)
public final class EsnMod {
    public static final String MOD_ID = "esn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EsnMod() {
        LOGGER.info("Extra Special Network (ESN) loaded");
    }
}
