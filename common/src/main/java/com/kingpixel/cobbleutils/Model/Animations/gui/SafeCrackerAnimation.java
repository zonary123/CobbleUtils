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

public class SafeCrackerAnimation extends Animation {

  private static final int dialSlot = 13;
  private static final int resultSlot = 22;

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    startSafe(player, obtained, onComplete);
  }

  public static void startSafe(ServerPlayerEntity player, List<ItemStack> showRewards, Runnable onComplete) {
    if (showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, player.getSoundCategory(), 1.0f, 1.0f);

      try {
        ChestTemplate template = ChestTemplate.builder(3).build();
        fillBorders(template);

        ItemStack dialItem = new ItemStack(Items.COMPASS);
        dialItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e[ Spin Dial ]"));

        AtomicInteger spinCount = new AtomicInteger(0);
        final boolean[] unlocked = {false};

        Button dialButton = GooeyButton.builder()
          .display(dialItem)
          .onClick(action -> {
            if (unlocked[0]) return;
            player.playSoundToPlayer(SoundEvents.BLOCK_DISPENSER_FAIL, player.getSoundCategory(), 0.8f, 1.5f);
            
            int count = spinCount.incrementAndGet();
            if (count >= 5) {
              unlocked[0] = true;
              player.playSoundToPlayer(SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, player.getSoundCategory(), 1.0f, 1.0f);
              player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 0.7f, 1.0f);

              ItemStack openVault = new ItemStack(Items.IRON_DOOR);
              openVault.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&a[ Safe Unlocked! ]"));

              CobbleUtils.server.execute(() -> {
                template.set(dialSlot, GooeyButton.builder().display(openVault).build());
                template.set(resultSlot, GooeyButton.builder().display(showRewards.get(0)).build());
              });

              CobbleUtils.runAsync(() -> {
                try {
                  Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
                CobbleUtils.server.execute(() -> {
                  UIManager.closeUI(player);
                  if (onComplete != null) onComplete.run();
                });
              });
            }
          })
          .build();

        template.set(dialSlot, dialButton);

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
      if (i != dialSlot && i != resultSlot) {
        template.set(i, border);
      }
    }
  }
}
