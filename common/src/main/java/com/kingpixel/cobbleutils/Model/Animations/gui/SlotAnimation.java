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

public class SlotAnimation extends Animation {

  private static final int leftSlot = 12;
  private static final int centerSlot = 13;
  private static final int rightSlot = 14;

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    runSlotBatched(player, allRewards, obtained, onComplete);
  }

  private static void runSlotBatched(ServerPlayerEntity player, List<ItemStack> allRewards,
                                     List<ItemStack> obtained, Runnable onComplete) {
    if (obtained.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    int batchSize = Math.min(6, obtained.size());
    List<ItemStack> batchRewards = obtained.subList(0, batchSize);

    startSlot(player, allRewards, batchRewards, onComplete);
  }

  public static void startSlot(ServerPlayerEntity player, List<ItemStack> showAllRewards, List<ItemStack> showRewards, Runnable onComplete) {
    if (showAllRewards.isEmpty() || showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f);
      ThreadLocalRandom random = ThreadLocalRandom.current();

      try {
        int N = showRewards.size();
        int M = N <= 3 ? 1 : 2;
        ChestTemplate template = ChestTemplate.builder(M * 3).build();
        fillGuiWithGrayGlass(template, M);

        GooeyPage page = GooeyPage.builder()
          .template(template)
          .build();

        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));

        ItemStack[][] finals = new ItemStack[M][3];
        for (int s = 0; s < M; s++) {
          for (int r = 0; r < 3; r++) {
            int rewardIdx = s * 3 + r;
            if (rewardIdx < N) {
              finals[s][r] = showRewards.get(rewardIdx);
            } else {
              finals[s][r] = showRewards.get(random.nextInt(showRewards.size()));
            }
          }
        }

        int totalTicks = 45;
        for (int t = 0; t < totalTicks; t++) {
          int delay = 50 + (t * 5);

          final int tickIndex = t;
          final ItemStack[][] displays = new ItemStack[M][3];

          for (int s = 0; s < M; s++) {
            if (tickIndex < 20) {
              displays[s][0] = showAllRewards.get(random.nextInt(showAllRewards.size()));
            } else if (tickIndex == 20) {
              displays[s][0] = finals[s][0];
              player.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, player.getSoundCategory(), 0.6f, 1.5f);
            } else {
              displays[s][0] = null;
            }

            if (tickIndex < 30) {
              displays[s][1] = showAllRewards.get(random.nextInt(showAllRewards.size()));
            } else if (tickIndex == 30) {
              displays[s][1] = finals[s][1];
              player.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, player.getSoundCategory(), 0.6f, 1.5f);
            } else {
              displays[s][1] = null;
            }

            if (tickIndex < 40) {
              displays[s][2] = showAllRewards.get(random.nextInt(showAllRewards.size()));
            } else if (tickIndex == 40) {
              displays[s][2] = finals[s][2];
              player.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, player.getSoundCategory(), 0.6f, 1.5f);
            } else {
              displays[s][2] = null;
            }
          }

          CobbleUtils.server.execute(() -> {
            for (int s = 0; s < M; s++) {
              int base = s * 27;
              if (displays[s][0] != null) template.set(base + leftSlot, guiButtons(displays[s][0]));
              if (displays[s][1] != null) template.set(base + centerSlot, guiButtons(displays[s][1]));
              if (displays[s][2] != null) template.set(base + rightSlot, guiButtons(displays[s][2]));
            }
          });

          player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), player.getSoundCategory(), 0.8f, 1.2f);
          Thread.sleep(delay);
        }

        CobbleUtils.server.execute(() -> highlightBorder(template, M));
        player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 0.8f, 1.0f);

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

  private static void fillGuiWithGrayGlass(ChestTemplate template, int M) {
    ItemStack grayGlassPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
    for (int s = 0; s < M; s++) {
      int base = s * 27;
      for (int i = 0; i < 27; i++) {
        if (i != leftSlot && i != centerSlot && i != rightSlot) {
          template.set(base + i, guiButtons(grayGlassPane));
        }
      }
    }
  }

  private static void highlightBorder(ChestTemplate template, int M) {
    ItemStack limeGlassPane = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
    int[] border = {2, 3, 4, 5, 6, 11, 15, 20, 21, 22, 23, 24};
    for (int s = 0; s < M; s++) {
      int base = s * 27;
      for (int slot : border) {
        template.set(base + slot, guiButtons(limeGlassPane));
      }
    }
  }

  private static @Nullable Button guiButtons(ItemStack item) {
    return GooeyButton.builder()
      .display(item)
      .build();
  }
}
