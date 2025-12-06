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

  @Override
  public void onPreLaunch() {
    FabricLibraryManager libraryManager = new FabricLibraryManager(LOGGER, MOD_ID);
    libraryManager.configureFromJSON();
  }
}
