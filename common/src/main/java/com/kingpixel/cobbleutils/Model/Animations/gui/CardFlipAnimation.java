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

public class CardFlipAnimation extends Animation {

  private static final int[] cardSlots = {11, 13, 15};

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    startCardFlip(player, obtained, onComplete);
  }

  public static void startCardFlip(ServerPlayerEntity player, List<ItemStack> showRewards, Runnable onComplete) {
    if (showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f);

      try {
        ChestTemplate template = ChestTemplate.builder(3).build();
        fillBorders(template);

        int totalCards = cardSlots.length;
        AtomicInteger flippedCount = new AtomicInteger(0);
        final boolean[] completed = {false};

        Runnable finishTask = () -> {
          if (!completed[0]) {
            completed[0] = true;
            player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 0.7f, 1.0f);
            try {
              Thread.sleep(1800);
            } catch (InterruptedException ignored) {}
            CobbleUtils.server.execute(() -> {
              UIManager.closeUI(player);
              if (onComplete != null) onComplete.run();
            });
          }
        };

        for (int i = 0; i < totalCards; i++) {
          int slot = cardSlots[i];
          ItemStack rewardItem = showRewards.get(i % showRewards.size());

          ItemStack cover = new ItemStack(Items.PAPER);
          cover.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e[ Mystery Card ]"));

          Button cardButton = GooeyButton.builder()
            .display(cover)
            .onClick(action -> {
              player.playSoundToPlayer(SoundEvents.ITEM_BOOK_PAGE_TURN, player.getSoundCategory(), 0.8f, 1.2f);
              CobbleUtils.runAsync(() -> {
                try {
                  ItemStack mapFrame = new ItemStack(Items.MAP);
                  CobbleUtils.server.execute(() -> template.set(slot, GooeyButton.builder().display(mapFrame).build()));
                  Thread.sleep(250);
                  
                  CobbleUtils.server.execute(() -> template.set(slot, GooeyButton.builder().display(rewardItem).build()));
                  player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, player.getSoundCategory(), 0.6f, 1.5f);

                  int count = flippedCount.incrementAndGet();
                  if (count >= totalCards) {
                    CobbleUtils.runAsync(finishTask);
                  }
                } catch (InterruptedException ignored) {}
              });
            })
            .build();

          template.set(slot, cardButton);
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
      boolean isCardSlot = false;
      for (int cs : cardSlots) {
        if (i == cs) {
          isCardSlot = true;
          break;
        }
      }
      if (!isCardSlot) {
        template.set(i, border);
      }
    }
  }
}
