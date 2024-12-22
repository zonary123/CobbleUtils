package com.kingpixel.cobbleutils.events.features;

import com.kingpixel.cobbleutils.events.ScaleEvent;

/**
 * @author Carlos Varas Alonso - 20/07/2024 13:13
 */
public class FeaturesRegister {
  public static void register() {
    PokemonBoss.register();
    PokerusEvents.register();
    ScaleEvent.register();
  }
}
