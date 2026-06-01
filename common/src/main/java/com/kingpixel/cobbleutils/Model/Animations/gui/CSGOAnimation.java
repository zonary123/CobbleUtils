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

public class CSGOAnimation extends Animation {

  private static final int[] spinSlots = {10, 11, 12, 13, 14, 15, 16};
  private static final ThreadLocalRandom random = ThreadLocalRandom.current();
  private static final int currentIndex = 0;

  private static final int totalCycles = 50;
  private static final int startSpinSpeed = 40;
  private static final double decayFactor = 1.1;

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    runCSGOBatched(player, allRewards, obtained, onComplete);
  }

  private static void runCSGOBatched(ServerPlayerEntity player, List<ItemStack> allRewards,
                                     List<ItemStack> obtained, Runnable onComplete) {
    if (obtained.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    int batchSize = Math.min(2, obtained.size());
    List<ItemStack> batchRewards = obtained.subList(0, batchSize);

    startCSGO(player, allRewards, batchRewards, onComplete);
  }

  public static void startCSGO(ServerPlayerEntity player, List<ItemStack> showAllRewards, List<ItemStack> showRewards, Runnable onComplete) {
    if (showAllRewards.isEmpty() || showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }
    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f);

      try {
        int M = showRewards.size();
        ChestTemplate template = ChestTemplate.builder(M * 3).build();
        fillGuiWithGlass(template, M);

        ItemStack[][] currentItems = new ItemStack[M][spinSlots.length];
        for (int s = 0; s < M; s++) {
          for (int i = 0; i < spinSlots.length; i++) {
            currentItems[s][i] = showAllRewards.get((currentIndex + i + s * 3) % showAllRewards.size());
          }
        }

        int rewardCycle = totalCycles - 4;
        int spinSpeed = startSpinSpeed;
        GooeyPage page = GooeyPage.builder()
          .template(template)
          .build();

        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
        for (int i = 0; i < totalCycles; i++) {
          double progress = (double) i / (totalCycles - 1);
          double dynamicDecayFactor = i >= totalCycles - 10 ? 1.2 : decayFactor;
          int spinSpeedNew = (int) (startSpinSpeed * Math.pow(dynamicDecayFactor, progress * 15));
          spinSpeed = (int) Math.max(10, Math.abs(spinSpeedNew - spinSpeed) < 5
            ? spinSpeedNew
            : spinSpeed + Math.signum(spinSpeedNew - spinSpeed) * 5);

          for (int s = 0; s < M; s++) {
            shiftItemsLeft(currentItems[s], showAllRewards);

            if (i == rewardCycle) {
              ItemStack reward = showRewards.get(s);
              currentItems[s][spinSlots.length - 1] = reward;
            }
          }

          final ItemStack[][] finalItems = new ItemStack[M][spinSlots.length];
          for (int s = 0; s < M; s++) {
            finalItems[s] = currentItems[s].clone();
          }

          CobbleUtils.server.execute(() -> {
            for (int s = 0; s < M; s++) {
              int base = s * 27;
              for (int j = 0; j < spinSlots.length; j++) {
                template.set(base + spinSlots[j], guiButtons(finalItems[s][j]));
              }
            }
          });
          player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, player.getSoundCategory(), 1.0f, 1.0f);

          Thread.sleep(spinSpeed);
        }
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

  public static void start(ServerPlayerEntity player, List<ItemStack> showAllRewards, List<ItemStack> showRewards) {
    startCSGO(player, showAllRewards, showRewards, null);
  }

  private static void fillGuiWithGlass(ChestTemplate template, int M) {
    ItemStack grayGlassPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
    ItemStack limeGlassPane = new ItemStack(Items.LIME_STAINED_GLASS_PANE);

    for (int s = 0; s < M; s++) {
      int base = s * 27;
      for (int i = 0; i < 27; i++) {
        if (i == 4 || i == 22) {
          template.set(base + i, guiButtons(limeGlassPane));
        } else {
          template.set(base + i, guiButtons(grayGlassPane));
        }
      }
    }
  }

  private static void shiftItemsLeft(ItemStack[] currentItems, List<ItemStack> showAllRewards) {
    for (int i = 0; i < currentItems.length - 1; i++) {
      currentItems[i] = currentItems[i + 1];
    }

    currentItems[currentItems.length - 1] = showAllRewards.get(random.nextInt(showAllRewards.size()));
  }

  private static @Nullable Button guiButtons(ItemStack item) {
    return GooeyButton.builder()
      .display(item)
      .build();
  }
}
