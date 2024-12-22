package com.kingpixel.cobbleutils.forge;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(CobbleUtils.MOD_ID)
public class CobbleUtilsForge {
  public CobbleUtilsForge() {
    //CobbleUtils.init();
  }

  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  @SubscribeEvent
  public static void onLoadComplete(FMLLoadCompleteEvent event) {
    // Delay execution by 10 seconds (adjust as needed)
    scheduler.schedule(() -> {
      CobbleUtils.init();
    }, 15, TimeUnit.SECONDS);
  }

}