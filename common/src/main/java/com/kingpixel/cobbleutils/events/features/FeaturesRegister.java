package com.kingpixel.cobbleutils.events.features;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.ScaleEvent;

/**
 * @author Carlos Varas Alonso - 20/07/2024 13:13
 */
public class FeaturesRegister {
  public static void register() {
    if (CobbleUtils.config.isBoss()) {
      PokemonBoss.register();
    }
    if (CobbleUtils.config.getPokerus().isActive()) {
      PokerusEvents.register();
    }
    if (CobbleUtils.config.isRandomsize()) {
      ScaleEvent.register();
    }
  }
}
