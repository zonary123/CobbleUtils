package com.kingpixel.cobbleutils.Model.Animations;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class CSGOAnimation {

  private static final int[] spinSlots = {10, 11, 12, 13, 14, 15, 16};
  private static final Random random = new Random();
  private static final int currentIndex = 0;

  private static final int totalCycles = 50; // Changes how many cycles the animation does
  private static final int startSpinSpeed = 40; // Lower = Faster // Starts off fast
  private static final double decayFactor = 1.1; // Increases how fast it slows down the animation

  public static void start(ServerPlayerEntity player, List<ItemStack> showAllRewards, List<ItemStack> showRewards) {
    // Sonido inicial en main thread
    CobbleUtils.server.executeSync(() ->
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, player.getSoundCategory(), 1.0f, 1.0f)
    );

    CompletableFuture.runAsync(() -> {
      try {
        ChestTemplate template = ChestTemplate.builder(3).build();
        fillGuiWithGlass(template);

        List<ItemStack> rewards = showAllRewards; // no copia innecesaria
        ItemStack[] currentItems = new ItemStack[spinSlots.length];
        for (int i = 0; i < spinSlots.length; i++) {
          currentItems[i] = rewards.get((currentIndex + i) % rewards.size());
        }

        int rewardCycle = totalCycles - 4;
        int spinSpeed = startSpinSpeed;

        for (int i = 0; i < totalCycles; i++) {
          double progress = (double) i / (totalCycles - 1);
          double dynamicDecayFactor = i >= totalCycles - 10 ? 1.2 : decayFactor;
          int spinSpeedNew = (int) (startSpinSpeed * Math.pow(dynamicDecayFactor, progress * 15));
          spinSpeed = (int) Math.max(10, Math.abs(spinSpeedNew - spinSpeed) < 5
            ? spinSpeedNew
            : spinSpeed + Math.signum(spinSpeedNew - spinSpeed) * 5);

          shiftItemsLeft(currentItems, rewards);

          if (i == rewardCycle) {
            ItemStack reward = showRewards.get(random.nextInt(showRewards.size()));
            currentItems[spinSlots.length - 1] = reward;
          }

          // Aquí solo la parte que toca MC en main thread
          ItemStack[] finalItems = currentItems.clone();
          CobbleUtils.server.executeSync(() -> {
            for (int j = 0; j < spinSlots.length; j++) {
              template.set(spinSlots[j], guiButtons(finalItems[j]));
            }
            player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, player.getSoundCategory(), 1.0f, 1.0f);
            UIManager.openUIForcefully(player, GooeyPage.builder().template(template).build());
          });

          Thread.sleep(spinSpeed); // espera solo en async
        }

      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }, CobbleUtils.EXECUTOR_COBBLEUTILS);
  }


  private static void fillGuiWithGlass(ChestTemplate template) {
    ItemStack grayGlassPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
    ItemStack limeGlassPane = new ItemStack(Items.LIME_STAINED_GLASS_PANE);

    for (int i = 0; i < 27; i++) {
      if (i == 4 || i == 22) {
        template.set(i, guiButtons(limeGlassPane));
      } else {
        template.set(i, guiButtons(grayGlassPane));
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