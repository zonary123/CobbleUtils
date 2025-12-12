package com.kingpixel.cobbleutils.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.config.Lang;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Data
public class ConfirmMenu {
  private int rows;
  private String title;
  private int slotDisplay;
  private long customModelDataConfirm;
  private ItemModel confirm;
  private ItemModel cancel;
  private ItemModel close;
  private List<PanelsConfig> panels;
  transient
  private ChestTemplate template;
  transient
  private Text titleText;

  public ConfirmMenu() {
    this.rows = 3;
    this.title = "Confirm";
    this.slotDisplay = 13;
    this.customModelDataConfirm = -1;
    Lang lang = CobbleUtils.language;
    if (lang != null) {
      this.confirm = CobbleUtils.language.getItemConfirm();
      this.cancel = CobbleUtils.language.getItemCancel();
      this.close = CobbleUtils.language.getItemClose();
    } else {
      this.confirm = new ItemModel("minecraft:emerald", "&aConfirm");
      this.cancel = new ItemModel("minecraft:redstone", "&cCancel");
      this.close = new ItemModel("minecraft:barrier", "&cClose");
    }
    confirm.setSlot(10);
    confirm.setDisplayname("&aConfirm");
    cancel.setSlot(16);
    cancel.setDisplayname("&cCancel");
    close.setSlot(22);
    close.setDisplayname("&cClose");
    this.panels = List.of(
      new PanelsConfig(rows)
    );
    int totalSlots = rows * 9;
    for (PanelsConfig panel : panels) {
      panel.getSlots().removeIf(slot -> slot < 0 || slot >= totalSlots);
    }
  }

  private ChestTemplate createTemplate() {
    ChestTemplate template = ChestTemplate.builder(rows).build();
    PanelsConfig.applyConfig(template, panels);

    titleText = AdventureTranslator.toNative(title);
    int totalSlots = rows * 9;
    for (PanelsConfig panel : panels) {
      panel.getSlots().removeIf(slot -> slot < 0 || slot >= totalSlots);
    }
    return template;
  }

  /**
   * Opens the default confirm menu if useDefault is enabled.
   * This is the recommended way to open confirm menus when using CobbleUtils as an API.
   *
   * @param player The player to show the menu to
   * @param itemStack The item to display
   * @param onConfirm Action to execute on confirm
   * @param onCancel Action to execute on cancel
   * @return true if the default menu was used, false if custom implementation should be used
   */
  public static boolean openDefault(ServerPlayerEntity player, ItemStack itemStack,
                                     Consumer<ButtonAction> onConfirm, Consumer<ButtonAction> onCancel) {
    if (!CobbleUtils.config.isUseDefault()) {
      return false;
    }
    // Force the use of default confirm menu
    CobbleUtils.language.getConfirmMenu().open(player, itemStack, onConfirm, onCancel);
    return true;
  }

  public void open(ServerPlayerEntity player, ItemStack itemStack, Consumer<ButtonAction> onConfirm,
                   Consumer<ButtonAction> onCancel) {
    CompletableFuture.runAsync(() -> {
        ChestTemplate template = createTemplate();

        // Copiar el ItemStack para no modificar el original
        ItemStack displayStack = itemStack.copy();

        // Aplicar customModelData si está configurado (>= 0 es válido, -1 significa no aplicar)
        if (customModelDataConfirm >= 0) {
          displayStack.set(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA,
            new net.minecraft.component.type.CustomModelDataComponent((int) customModelDataConfirm));
        }

        // Mostrar el ítem principal en el slot correspondiente
        template.set(slotDisplay, GooeyButton.builder()
          .display(displayStack)
          .build()
        );

        // Botón de confirmación
        if (confirm != null) confirm.applyTemplate(template, confirm.getButton(onConfirm, 1, TimeUnit.SECONDS, 1));
        if (cancel != null) cancel.applyTemplate(template, cancel.getButton(onCancel, 1, TimeUnit.SECONDS, 1));
        if (close != null) close.applyTemplate(template, close.getButton(onCancel, 1, TimeUnit.SECONDS, 1));
        // Crear y abrir la página del menú
        GooeyPage page = GooeyPage.builder()
          .title(titleText)
          .template(template)
          .build();
        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
      }, CobbleUtils.EXECUTOR_COBBLEUTILS)
      .orTimeout(2, TimeUnit.SECONDS)
      .exceptionally(e -> {
        if (e instanceof TimeoutException) return null;
        e.printStackTrace();
        return null;
      });
  }
}