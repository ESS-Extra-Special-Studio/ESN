package uk.co.extraspecialstudio.esn;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Extra Special Network — shared packet / channel fabric.
 * No lobby, wave, airdrop, or HUD types belong here.
 */
@Mod(EsnMod.MOD_ID)
public final class EsnMod {
    public static final String MOD_ID = "esn";
    public static final Logger LOGGER = LogManager.getLogger();

    public EsnMod() {
        LOGGER.info("Extra Special Network (ESN) loaded");
    }
}
