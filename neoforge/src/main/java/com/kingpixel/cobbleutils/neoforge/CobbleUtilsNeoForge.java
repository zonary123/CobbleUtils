package com.kingpixel.cobbleutils.neoforge;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CobbleUtils.MOD_ID)
public class CobbleUtilsNeoForge {

  public CobbleUtilsNeoForge(IEventBus modBus) {
    CobbleUtils.init();
  }
}
