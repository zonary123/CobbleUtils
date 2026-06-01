package com.kingpixel.cobbleutils.Model.Animations.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.Animation;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PlinkoAnimation extends Animation {

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    if (obtained.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }
    // Ejecutamos el Plinko secuencial de interfaz
    startSequentialGuiPlinko(player, allRewards, obtained, onComplete);
  }

  public static void startSequentialGuiPlinko(ServerPlayerEntity player, List<ItemStack> showAllRewards,
                                              List<ItemStack> showRewards, Runnable onComplete) {
    if (showAllRewards.isEmpty() || showRewards.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CobbleUtils.runAsync(() -> {
      player.playSoundToPlayer(SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
      ThreadLocalRandom random = ThreadLocalRandom.current();

      try {
        // Usamos un único inventario grande de 6 filas (54 slots) fijo para el tablero
        ChestTemplate template = ChestTemplate.builder(6).build();
        fillStaticBoard(template);

        GooeyPage page = GooeyPage.builder()
          .template(template)
          .build();

        // Abrimos la interfaz al jugador en el hilo principal
        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));

        ItemStack grayGlassPane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);

        // --- PROCESADO SECUENCIAL ÍTEM POR ÍTEM ---
        for (int i = 0; i < showRewards.size(); i++) {
          ItemStack finalReward = showRewards.get(i);

          // Calculamos un camino dinámico paso a paso (De la fila 0 a la 5)
          int[] path = new int[6];
          path[0] = 4; // Slot central superior

          int currentSlot = 4;
          for (int row = 1; row <= 5; row++) {
            // El Plinko baja una fila (+9 slots) y decide de forma aleatoria ir a la izquierda (-1) o derecha (+1)
            int sideMove = random.nextBoolean() ? 1 : -1;

            // Control de márgenes para que no se salga de las paredes del inventario
            int nextSlot = currentSlot + 9 + sideMove;
            int nextCol = nextSlot % 9;
            if (nextCol < 1 || nextCol > 7) {
              nextSlot = currentSlot + 9; // Si se desborda, cae recto
            }

            path[row] = nextSlot;
            currentSlot = nextSlot;
          }

          // Ejecutamos la animación de caída de este ítem a través de las filas
          for (int step = 0; step < 6; step++) {
            final int currentStep = step;

            CobbleUtils.server.execute(() -> {
              // Limpiamos el rastro del paso anterior volviendo a poner el fondo
              if (currentStep > 0) {
                template.set(path[currentStep - 1], guiButtons(grayGlassPane));
              }

              // Efecto de ruleta: Mientras cae se ve aleatorio, al tocar el fondo (paso 5) revela el premio real
              ItemStack displayStack = (currentStep == 5)
                ? finalReward
                : showAllRewards.get(random.nextInt(showAllRewards.size()));

              template.set(path[currentStep], guiButtons(displayStack));
            });

            // Sonido de rebote mecánico
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.RECORDS, 0.8f, 1.3f);
            Thread.sleep(350); // Velocidad de caída fluida por ranura
          }

          // Sonido de premio depositado en la zona baja del tablero
          player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.2f);

          // Espera breve en el fondo antes de lanzar el siguiente ítem por el embudo
          Thread.sleep(600);
        }

        // Gran celebración al terminar toda la lista de premios
        player.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.0f);
        Thread.sleep(1000);

        // Cierre e invocación del callback de fin
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

  /**
   * Rellena un único tablero estético con obstáculos fijos simétricos
   */
  private static void fillStaticBoard(ChestTemplate template) {
    ItemStack grayGlass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
    ItemStack yellowGlass = new ItemStack(Items.YELLOW_STAINED_GLASS_PANE); // Los "Clavos"

    // Rellenamos todo con la base gris por defecto
    for (int i = 0; i < 54; i++) {
      template.set(i, guiButtons(grayGlass));
    }

    // Diseñamos una estructura piramidal de pines amarillos en ranuras intercaladas
    int[] pins = {
      12, 14,      // Fila 1
      21, 23, 25,  // Fila 2
      30, 32, 34,  // Fila 3
      41, 43        // Fila 4
    };

    for (int pinSlot : pins) {
      template.set(pinSlot, guiButtons(yellowGlass));
    }
  }

  private static @Nullable Button guiButtons(ItemStack item) {
    return GooeyButton.builder()
      .display(item)
      .build();
  }
}