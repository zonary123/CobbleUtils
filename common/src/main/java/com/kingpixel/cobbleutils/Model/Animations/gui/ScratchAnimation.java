package com.kingpixel.cobbleutils.Model.Animations.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.*;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ScratchAnimation extends Animation {

  private static final int[] prizeSlots = {10, 11, 12, 13, 14, 15, 16};

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    startScratch(player, obtained, onComplete);
  }

  public static void startScratch(ServerPlayerEntity player, List<ItemStack> showRewards, Runnable onComplete) {
    if (showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f);

      try {
        ChestTemplate template = ChestTemplate.builder(3).build();
        fillGuiBorders(template);

        int totalPrizes = prizeSlots.length;
        AtomicInteger scratchedCount = new AtomicInteger(0);
        final boolean[] completed = {false};

        Runnable finishTask = () -> {
          if (!completed[0]) {
            completed[0] = true;
            player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 0.7f, 1.0f);
            try {
              Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
            CobbleUtils.server.execute(() -> {
              UIManager.closeUI(player);
              if (onComplete != null) onComplete.run();
            });
          }
        };

        for (int i = 0; i < totalPrizes; i++) {
          int slot = prizeSlots[i];
          ItemStack rewardItem = showRewards.get(i % showRewards.size());

          ItemStack scratchCover = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
          scratchCover.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e[ Scratch Here! ]"));

          Button scratchButton = GooeyButton.builder()
            .display(scratchCover)
            .onClick(action -> {
              template.set(slot, GooeyButton.builder().display(rewardItem).build());
              player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), player.getSoundCategory(), 0.8f, 1.4f);
              player.playSoundToPlayer(SoundEvents.BLOCK_GRASS_BREAK, player.getSoundCategory(), 0.5f, 1.0f);
              
              int count = scratchedCount.incrementAndGet();
              if (count >= totalPrizes) {
                CobbleUtils.runAsync(finishTask);
              }
            })
            .build();

          template.set(slot, scratchButton);
        }

        GooeyPage page = GooeyPage.builder()
          .template(template)
          .build();

        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));

      } catch (Exception e) {
        e.printStackTrace();
        if (onComplete != null) CobbleUtils.server.execute(onComplete);
      }
    });
  }

  private static void fillGuiBorders(ChestTemplate template) {
    ItemStack grayGlassPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
    Button borderButton = GooeyButton.builder().display(grayGlassPane).build();

    for (int i = 0; i < 27; i++) {
      boolean isPrizeSlot = false;
      for (int ps : prizeSlots) {
        if (i == ps) {
          isPrizeSlot = true;
          break;
        }
      }
      if (!isPrizeSlot) {
        template.set(i, borderButton);
      }
    }
  }
}
