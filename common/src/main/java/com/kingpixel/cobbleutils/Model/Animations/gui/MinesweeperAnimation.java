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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MinesweeperAnimation extends Animation {

  private static final int[] mineSlots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    startMinesweeper(player, obtained, onComplete);
  }

  public static void startMinesweeper(ServerPlayerEntity player, List<ItemStack> showRewards, Runnable onComplete) {
    if (showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f);

      try {
        ChestTemplate template = ChestTemplate.builder(5).build();
        fillBorders(template);

        int totalMines = mineSlots.length;
        AtomicInteger revealedCount = new AtomicInteger(0);
        final boolean[] completed = {false};

        Runnable finishTask = () -> {
          if (!completed[0]) {
            completed[0] = true;
            player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 0.7f, 1.0f);
            try {
              Thread.sleep(1500);
            } catch (InterruptedException ignored) {}
            CobbleUtils.server.execute(() -> {
              UIManager.closeUI(player);
              if (onComplete != null) onComplete.run();
            });
          }
        };

        for (int i = 0; i < totalMines; i++) {
          int slot = mineSlots[i];
          ItemStack rewardItem = showRewards.get(i % showRewards.size());

          ItemStack cover = new ItemStack(Items.LIGHT_GRAY_CONCRETE);
          cover.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e[ Safe Tile ]"));

          Button mineButton = GooeyButton.builder()
            .display(cover)
            .onClick(action -> {
              template.set(slot, GooeyButton.builder().display(rewardItem).build());
              player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), player.getSoundCategory(), 0.8f, 1.4f);
              player.playSoundToPlayer(SoundEvents.BLOCK_GRASS_BREAK, player.getSoundCategory(), 0.5f, 1.0f);

              int count = revealedCount.incrementAndGet();
              if (count >= 5) {
                CobbleUtils.runAsync(finishTask);
              }
            })
            .build();

          template.set(slot, mineButton);
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

  private static void fillBorders(ChestTemplate template) {
    ItemStack borderGlass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
    Button border = GooeyButton.builder().display(borderGlass).build();

    for (int i = 0; i < 45; i++) {
      boolean isMineSlot = false;
      for (int ms : mineSlots) {
        if (i == ms) {
          isMineSlot = true;
          break;
        }
      }
      if (!isMineSlot) {
        template.set(i, border);
      }
    }
  }
}
