package com.kingpixel.cobbleutils.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.SoundUtil;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * @author Carlos Varas Alonso - 29/06/2024 19:11
 */
public class ShinyTokenPcUI {
  public static GooeyPage getMenuShinyTokenPc(ServerPlayerEntity player) {
    try {
      return UIUtils.createPagePc(Cobblemon.INSTANCE.getStorage().getPC(player), actionpokemon -> {
          try {
            if (CobbleUtils.config.isShinyTokenBlacklisted(actionpokemon.getPokemon()))
              return;
            if (!actionpokemon.getPokemon().getShiny()) {
              SoundUtil.playSound(CobbleUtils.language.getSoundopen(), actionpokemon.getAction().getPlayer());
              UIManager.openUIForcefully(actionpokemon.getAction().getPlayer(), ShinyTokenUI.confirmShiny(player,
                actionpokemon.getPokemon()));

            }


          } catch (NoPokemonStoreException e) {
            throw new RuntimeException(e);
          }
        },
        actionclose -> UIManager.openUIForcefully(actionclose.getPlayer(),
          Objects.requireNonNull(ShinyTokenUI.openmenu(player))),
        CobbleUtils.language.getTitlepc());
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
    return null;
  }
}
