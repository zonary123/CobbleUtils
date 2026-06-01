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
import java.util.concurrent.ThreadLocalRandom;

public class WheelOfFortuneAnimation extends Animation {

  private static final int[] wheelSlots = {13, 23, 33, 32, 31, 21, 11, 12};
  private static final int pointerSlot = 4;

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    startWheel(player, allRewards, obtained, onComplete);
  }

  public static void startWheel(ServerPlayerEntity player, List<ItemStack> showAllRewards, List<ItemStack> showRewards, Runnable onComplete) {
    if (showAllRewards.isEmpty() || showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f);
      ThreadLocalRandom random = ThreadLocalRandom.current();

      try {
        ChestTemplate template = ChestTemplate.builder(5).build();
        fillBorders(template);

        GooeyPage page = GooeyPage.builder()
          .template(template)
          .build();

        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));

        int totalTicks = 35;
        int currentWheelOffset = 0;
        int delay = 50;

        for (int i = 0; i < totalTicks; i++) {
          double progress = (double) i / (totalTicks - 1);
          delay = (int) (50 + Math.pow(progress * 15, 2.0));

          currentWheelOffset = (currentWheelOffset + 1) % wheelSlots.length;
          final int offset = currentWheelOffset;

          final ItemStack[] itemsToDraw = new ItemStack[wheelSlots.length];
          for (int k = 0; k < wheelSlots.length; k++) {
            if (i == totalTicks - 1 && k == 0) {
              itemsToDraw[k] = showRewards.get(0);
            } else {
              itemsToDraw[k] = showAllRewards.get((offset + k) % showAllRewards.size());
            }
          }

          CobbleUtils.server.execute(() -> {
            for (int k = 0; k < wheelSlots.length; k++) {
              template.set(wheelSlots[k], guiButtons(itemsToDraw[k]));
            }
          });

          player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), player.getSoundCategory(), 0.8f, 1.2f);
          Thread.sleep(delay);
        }

        highlightPointer(template);
        player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 0.7f, 1.0f);
        Thread.sleep(1500);

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
    ItemStack pointerGlass = new ItemStack(Items.RED_STAINED_GLASS_PANE);

    for (int i = 0; i < 45; i++) {
      if (i == pointerSlot) {
        template.set(i, guiButtons(pointerGlass));
      } else {
        boolean isWheelSlot = false;
        for (int ws : wheelSlots) {
          if (i == ws) {
            isWheelSlot = true;
            break;
          }
        }
        if (!isWheelSlot) {
          template.set(i, guiButtons(borderGlass));
        }
      }
    }
  }

  private static void highlightPointer(ChestTemplate template) {
    ItemStack limeGlass = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
    template.set(pointerSlot, guiButtons(limeGlass));
  }

  private static @Nullable Button guiButtons(ItemStack item) {
    return GooeyButton.builder()
      .display(item)
      .build();
  }
}
