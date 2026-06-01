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

public class MysteryChestAnimation extends Animation {

  private static final int[] chestSlots = {11, 13, 15};

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    startMystery(player, obtained, onComplete);
  }

  public static void startMystery(ServerPlayerEntity player, List<ItemStack> showRewards, Runnable onComplete) {
    if (showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f);

      try {
        ChestTemplate template = ChestTemplate.builder(3).build();
        fillBorders(template);

        final boolean[] opened = {false};

        for (int i = 0; i < chestSlots.length; i++) {
          int slot = chestSlots[i];
          ItemStack reward = showRewards.get(i % showRewards.size());

          ItemStack closedChest = new ItemStack(Items.CHEST);
          closedChest.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e[ Mystery Chest ]"));

          Button chestButton = GooeyButton.builder()
            .display(closedChest)
            .onClick(action -> {
              if (opened[0]) return;
              opened[0] = true;

              player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f);
              player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 0.7f, 1.0f);

              ItemStack openChest = new ItemStack(Items.ENDER_CHEST);
              openChest.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&a[ Opened! ]"));

              CobbleUtils.server.execute(() -> {
                template.set(slot, GooeyButton.builder().display(openChest).build());
              });

              CobbleUtils.runAsync(() -> {
                try {
                  Thread.sleep(800);
                  CobbleUtils.server.execute(() -> {
                    template.set(slot, GooeyButton.builder().display(reward).build());
                  });
                  Thread.sleep(1500);
                } catch (InterruptedException ignored) {}

                CobbleUtils.server.execute(() -> {
                  UIManager.closeUI(player);
                  if (onComplete != null) onComplete.run();
                });
              });
            })
            .build();

          template.set(slot, chestButton);
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

    for (int i = 0; i < 27; i++) {
      boolean isChestSlot = false;
      for (int cs : chestSlots) {
        if (i == cs) {
          isChestSlot = true;
          break;
        }
      }
      if (!isChestSlot) {
        template.set(i, border);
      }
    }
  }
}
