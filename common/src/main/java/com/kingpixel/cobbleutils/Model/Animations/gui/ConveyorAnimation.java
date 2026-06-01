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

public class ConveyorAnimation extends Animation {

  private static final int[] beltSlots = {9, 10, 11, 12, 13, 14, 15, 16, 17};
  private static final int clawSlot = 4;

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    startConveyor(player, allRewards, obtained, onComplete);
  }

  public static void startConveyor(ServerPlayerEntity player, List<ItemStack> showAllRewards, List<ItemStack> showRewards, Runnable onComplete) {
    if (showAllRewards.isEmpty() || showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f);
      ThreadLocalRandom random = ThreadLocalRandom.current();

      try {
        ChestTemplate template = ChestTemplate.builder(3).build();
        fillBorders(template);

        GooeyPage page = GooeyPage.builder()
          .template(template)
          .build();

        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));

        int totalTicks = 30;
        int delay = 80;

        for (int i = 0; i < totalTicks; i++) {
          final ItemStack[] beltItems = new ItemStack[beltSlots.length];
          for (int k = 0; k < beltSlots.length; k++) {
            if (i == totalTicks - 1 && k == 4) {
              beltItems[k] = showRewards.get(0);
            } else {
              beltItems[k] = showAllRewards.get((i + k) % showAllRewards.size());
            }
          }

          CobbleUtils.server.execute(() -> {
            for (int k = 0; k < beltSlots.length; k++) {
              template.set(beltSlots[k], guiButtons(beltItems[k]));
            }
          });

          player.playSoundToPlayer(SoundEvents.BLOCK_DISPENSER_DISPENSE, player.getSoundCategory(), 0.5f, 1.5f);
          Thread.sleep(delay);
        }

        player.playSoundToPlayer(SoundEvents.BLOCK_PISTON_EXTEND, player.getSoundCategory(), 0.8f, 1.2f);
        ItemStack clawItem = new ItemStack(Items.PISTON);
        CobbleUtils.server.execute(() -> template.set(clawSlot, guiButtons(clawItem)));
        Thread.sleep(300);

        player.playSoundToPlayer(SoundEvents.ENTITY_ARROW_HIT_PLAYER, player.getSoundCategory(), 0.8f, 1.5f);
        player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 0.7f, 1.0f);

        Thread.sleep(1200);

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
    ItemStack yellowGlass = new ItemStack(Items.YELLOW_STAINED_GLASS_PANE);

    for (int i = 0; i < 27; i++) {
      boolean isBeltSlot = false;
      for (int bs : beltSlots) {
        if (i == bs) {
          isBeltSlot = true;
          break;
        }
      }
      if (!isBeltSlot) {
        if (i == clawSlot) {
          template.set(i, guiButtons(yellowGlass));
        } else {
          template.set(i, guiButtons(borderGlass));
        }
      }
    }
  }

  private static @Nullable Button guiButtons(ItemStack item) {
    return GooeyButton.builder()
      .display(item)
      .build();
  }
}
