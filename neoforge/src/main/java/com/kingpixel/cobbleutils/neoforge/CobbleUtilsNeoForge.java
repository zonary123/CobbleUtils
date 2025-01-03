package com.kingpixel.cobbleutils.neoforge;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.neoforged.fml.common.Mod;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(CobbleUtils.MOD_ID)
public class CobbleUtilsNeoForge {

  public CobbleUtilsNeoForge() {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.schedule(CobbleUtils::init, 10, TimeUnit.SECONDS);
  }


}
