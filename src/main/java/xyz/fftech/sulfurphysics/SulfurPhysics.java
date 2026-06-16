package xyz.fftech.sulfurphysics;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SulfurPhysics implements ModInitializer {
    public static final String MOD_ID = "sulfur-physics";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Sulfur Physics placeholder loaded; server-side sulfur cube physics is not implemented yet.");
    }
}
