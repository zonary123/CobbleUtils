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

public class DiceAnimation extends Animation {

  private static final int totalCycles = 30;
  private static final int startSpinSpeed = 40;
  private static final double decayFactor = 1.12;

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    startDice(player, allRewards, obtained, onComplete);
  }

  public static void startDice(ServerPlayerEntity player, List<ItemStack> showAllRewards, List<ItemStack> showRewards, Runnable onComplete) {
    if (showAllRewards.isEmpty() || showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f);
      ThreadLocalRandom random = ThreadLocalRandom.current();

      try {
        ChestTemplate template = ChestTemplate.builder(3).build();
        fillGuiWithGrayGlass(template);

        GooeyPage page = GooeyPage.builder()
          .template(template)
          .build();

        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));

        int[] targets = getTargetSlots(showRewards.size());
        int spinSpeed = startSpinSpeed;

        for (int i = 0; i < totalCycles; i++) {
          double progress = (double) i / (totalCycles - 1);
          int spinSpeedNew = (int) (startSpinSpeed * Math.pow(decayFactor, progress * 16));
          spinSpeed = Math.max(10, spinSpeedNew);

          final ItemStack[] finalTempRewards = new ItemStack[targets.length];
          for (int t = 0; t < targets.length; t++) {
            if (i == totalCycles - 1) {
              finalTempRewards[t] = showRewards.get(t % showRewards.size());
            } else {
              finalTempRewards[t] = showAllRewards.get(random.nextInt(showAllRewards.size()));
            }
          }

          CobbleUtils.server.execute(() -> {
            for (int t = 0; t < targets.length; t++) {
              template.set(targets[t], guiButtons(finalTempRewards[t]));
            }
          });

          player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), player.getSoundCategory(), 0.8f, 1.2f);

          Thread.sleep(spinSpeed);
        }

        CobbleUtils.server.execute(() -> highlightBorder(template, targets));
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

  private static int[] getTargetSlots(int n) {
    if (n <= 1) return new int[]{13};
    if (n == 2) return new int[]{12, 14};
    if (n == 3) return new int[]{11, 13, 15};
    if (n == 4) return new int[]{11, 12, 14, 15};
    if (n == 5) return new int[]{10, 11, 13, 15, 16};
    if (n == 6) return new int[]{10, 11, 12, 14, 15, 16};
    return new int[]{10, 11, 12, 13, 14, 15, 16};
  }

  private static void fillGuiWithGrayGlass(ChestTemplate template) {
    ItemStack grayGlassPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
    for (int i = 0; i < 27; i++) {
      template.set(i, guiButtons(grayGlassPane));
    }
  }

  private static void highlightBorder(ChestTemplate template, int[] targets) {
    ItemStack limeGlassPane = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
    for (int i = 0; i < 27; i++) {
      boolean isTarget = false;
      for (int t : targets) {
        if (i == t) {
          isTarget = true;
          break;
        }
      }
      if (!isTarget) {
        template.set(i, guiButtons(limeGlassPane));
      }
    }
  }

  private static @Nullable Button guiButtons(ItemStack item) {
    return GooeyButton.builder()
      .display(item)
      .build();
  }
}
