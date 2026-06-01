package com.kingpixel.cobbleutils.Model.Animations.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LootboxAnimation extends Animation {

  private static final int boxSlot = 13;

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    startLootbox(player, obtained, onComplete);
  }

  public static void startLootbox(ServerPlayerEntity player, List<ItemStack> showRewards, Runnable onComplete) {
    if (showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, player.getSoundCategory(), 0.8f, 0.8f);

      try {
        ChestTemplate template = ChestTemplate.builder(3).build();
        fillBorders(template);

        GooeyPage page = GooeyPage.builder()
          .template(template)
          .build();

        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));

        ItemStack boxItem = new ItemStack(Items.ENDER_CHEST);
        ItemStack openBoxItem = new ItemStack(Items.CHEST);

        int totalTicks = 12;
        for (int i = 0; i < totalTicks; i++) {
          ItemStack state = (i % 2 == 0) ? boxItem : openBoxItem;
          CobbleUtils.server.execute(() -> template.set(boxSlot, guiButtons(state)));
          player.playSoundToPlayer(SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE, player.getSoundCategory(), 0.5f, 1.5f);
          Thread.sleep(200);
        }

        player.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), player.getSoundCategory(), 0.8f, 1.2f);
        player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 0.7f, 1.0f);

        CobbleUtils.server.execute(() -> {
          template.set(boxSlot, guiButtons(showRewards.get(0)));
          highlightBorders(template);
        });

        Thread.sleep(1800);

        CobbleUtils.server.execute(() -> {
          UIManager.closeUI(player);
          if (onComplete != null) onComplete.run();
        });

      } catch (InterruptedException e) {
        e.printStackTrace();
        if (onComplete != null) CobbleUtils.server.execute(onComplete);
      }
    });
  }

  private static void fillBorders(ChestTemplate template) {
    ItemStack borderGlass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
    Button border = GooeyButton.builder().display(borderGlass).build();

    for (int i = 0; i < 27; i++) {
      if (i != boxSlot) {
        template.set(i, border);
      }
    }
  }

  private static void highlightBorders(ChestTemplate template) {
    ItemStack limeGlass = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
    Button border = GooeyButton.builder().display(limeGlass).build();

    for (int i = 0; i < 27; i++) {
      if (i != boxSlot) {
        template.set(i, border);
      }
    }
  }

  private static @Nullable Button guiButtons(ItemStack item) {
    return GooeyButton.builder()
      .display(item)
      .build();
  }
}
