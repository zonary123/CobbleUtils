package com.kingpixel.cobbleutils.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.UIUtils;
import com.kingpixel.cobbleutils.util.Utils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * @author Carlos Varas Alonso - 28/06/2024 20:09
 */
public class ShinyTokenUI {
  public static GooeyPage openmenu(ServerPlayerEntity player) {

    PlayerPartyStore partyStore = Cobblemon.INSTANCE.getStorage().getParty(player);

    ChestTemplate templateBuilder = ChestTemplate.builder(4).build();

    for (int i = 0; i < partyStore.size(); i++) {
      GooeyButton slot;
      Pokemon pokemon = partyStore.get(i);
      slot = UIUtils.createButtonPokemon(pokemon, action -> {
        if (pokemon == null)
          return;
        if (CobbleUtils.config.isShinyTokenBlacklisted(pokemon))
          return;
        if (!pokemon.getShiny()) {
          UIManager.openUIForcefully(player, confirmShiny(player, pokemon));
        }
      });
      int row = i / 3;
      int col = i % 3 + 3;
      templateBuilder.set(row + 1, col, slot);
    }
    templateBuilder.set(0, 4,
      CobbleUtils.language.getItemPc().getButton(action -> UIManager.openUIForcefully(action.getPlayer(),
        ShinyTokenPcUI.getMenuShinyTokenPc(action.getPlayer()))));

    GooeyButton fill = GooeyButton.builder()
      .display(Utils.parseItemId(CobbleUtils.config.getFill()))
      .with(DataComponentTypes.CUSTOM_NAME, Text.empty())
      .build();

    templateBuilder.fill(fill);

    Text title = AdventureTranslator.toNative(CobbleUtils.language.getTitlemenushiny());

    GooeyPage page = GooeyPage.builder().template(templateBuilder)
      .title(title)
      .build();

    UIManager.openUIForcefully(player, page);
    return page;
  }

  public static GooeyPage confirmShiny(ServerPlayerEntity player, Pokemon pokemon) {
    GooeyButton confirm = UIUtils.getConfirmButton(action -> {
      pokemon.setShiny(true);
      player.getMainHandStack().decrement(1);
      UIManager.closeUI(player);
    });

    GooeyButton buttonPokemon = UIUtils.createButtonPokemon(pokemon, (action) -> {
    });

    GooeyButton cancel = UIUtils.getCancelButton(action -> openmenu(player));

    return GooeyPage.builder()
      .template(new ChestTemplate.Builder(3)
        .set(1, 2, confirm)
        .set(1, 6, cancel)
        .set(1, 4, buttonPokemon)
        .fill(GooeyButton.builder().display(Utils.parseItemId(CobbleUtils.config.getFill()))
          .with(DataComponentTypes.CUSTOM_NAME, Text.empty())
          .build())
        .build())
      .title(AdventureTranslator.toNative(CobbleUtils.language.getTitlemenushinyoperation())).build();
  }

  public static boolean haveShinyToken(ServerPlayerEntity player) {
    return false;
  }

  public static void openmenu(PlayerEntity player) {
  }
}
