package com.kingpixel.cobbleutils.features.breeding.events;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import com.kingpixel.cobbleutils.features.breeding.Breeding;
import kotlin.Unit;

/**
 * @author Carlos Varas Alonso - 08/08/2024 15:39
 */
public class NationalityPokemon {
  public static void register() {
    CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, (evt) -> {
      if (!CobbleUtils.breedconfig.isMethodmasuda()) return Unit.INSTANCE;
      Breeding.UserInfo userinfo = Breeding.playerCountry.get(evt.getPlayer().getUuid());
      if (userinfo == null) return Unit.INSTANCE;
      evt.getPokemon().getPersistentData().putString(CobbleUtilsTags.COUNTRY_TAG, userinfo.country());
      return Unit.INSTANCE;
    });
  }
}
