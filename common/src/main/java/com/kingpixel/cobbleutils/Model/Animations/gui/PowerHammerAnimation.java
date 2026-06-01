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

public class PowerHammerAnimation extends Animation {

  private static final int hammerSlot = 49;
  private static final int[] towerSlots = {40, 31, 22, 13, 4};

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    startHammer(player, obtained, onComplete);
  }

  public static void startHammer(ServerPlayerEntity player, List<ItemStack> showRewards, Runnable onComplete) {
    if (showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f);

      try {
        ChestTemplate template = ChestTemplate.builder(6).build();
        fillBorders(template);

        GooeyPage page = GooeyPage.builder()
          .template(template)
          .build();

        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));

        ItemStack ironHammer = new ItemStack(Items.IRON_AXE);
        CobbleUtils.server.execute(() -> template.set(hammerSlot, guiButtons(ironHammer)));
        Thread.sleep(600);

        player.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, player.getSoundCategory(), 1.0f, 0.6f);
        player.playSoundToPlayer(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), player.getSoundCategory(), 0.5f, 0.5f);
        Thread.sleep(200);

        ItemStack redGlass = new ItemStack(Items.RED_STAINED_GLASS_PANE);

        for (int step = 0; step < towerSlots.length; step++) {
          final int slot = towerSlots[step];
          final ItemStack display = (step == towerSlots.length - 1) ? showRewards.get(0) : redGlass;

          CobbleUtils.server.execute(() -> template.set(slot, guiButtons(display)));

          if (step == towerSlots.length - 1) {
            player.playSoundToPlayer(SoundEvents.BLOCK_BELL_USE, player.getSoundCategory(), 1.0f, 1.5f);
            player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 0.8f, 1.0f);
          } else {
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), player.getSoundCategory(), 0.8f, (float) (0.5 + step * 0.3));
          }
          Thread.sleep(300);
        }

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
    Button border = GooeyButton.builder().display(borderGlass).build();

    for (int i = 0; i < 54; i++) {
      if (i != hammerSlot) {
        boolean isTowerSlot = false;
        for (int ts : towerSlots) {
          if (i == ts) {
            isTowerSlot = true;
            break;
          }
        }
        if (!isTowerSlot) {
          template.set(i, border);
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
