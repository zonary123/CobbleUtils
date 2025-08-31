package com.kingpixel.cobbleutils.fabric;

import net.byteflux.libby.FabricLibraryManager;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class CobbleUtilsFabricPreLaunch implements PreLaunchEntrypoint {
    private static final String MOD_ID = "cobbleutils";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Path DATA_DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);

    @Override
    public void onPreLaunch() {
        var modContainer = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        FabricLibraryManager libraryManager = new FabricLibraryManager(LOGGER, DATA_DIRECTORY, modContainer);
        libraryManager.configureFromJSON();
    }
}
