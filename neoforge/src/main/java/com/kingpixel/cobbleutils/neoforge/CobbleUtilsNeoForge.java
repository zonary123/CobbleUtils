package com.kingpixel.cobbleutils.neoforge;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.neoforged.fml.common.Mod;

@Mod(CobbleUtils.MOD_ID) public final class CobbleUtilsNeoForge {
  public CobbleUtilsNeoForge() {
    // Run our common setup.
    CobbleUtils.init();
  }
}
