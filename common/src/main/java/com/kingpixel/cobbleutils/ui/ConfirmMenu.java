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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Data
public class ConfirmMenu {
  private int rows;
  private String title;
  private int slotDisplay;
  private ItemModel confirm;
  private ItemModel cancel;
  private ItemModel close;
  private List<PanelsConfig> panels;

  public ConfirmMenu() {
    this.rows = 3;
    this.title = "Confirm";
    this.slotDisplay = 13;
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
    cancel.setSlot(16);
    close.setSlot(22);
    this.panels = List.of(
      new PanelsConfig(rows)
    );
  }

  public void open(ServerPlayerEntity player, ItemStack itemStack, Consumer<ButtonAction> onConfirm,
                   Consumer<ButtonAction> onCancel) {
    CompletableFuture.runAsync(() -> {
        ChestTemplate template = ChestTemplate.builder(rows).build();

        // Aplicar paneles decorativos
        PanelsConfig.applyConfig(template, panels);

        // Mostrar el ítem principal en el slot correspondiente
        template.set(slotDisplay, GooeyButton.of(itemStack));

        // Botón de confirmación
        if (confirm != null) {
          confirm.applyTemplate(template, confirm.getButton(onConfirm));
        }

        // Botón de cancelación
        if (cancel != null) {
          cancel.applyTemplate(template, cancel.getButton(onCancel));
        }

        // Botón de cierre
        if (close != null) {
          close.applyTemplate(template, close.getButton(onCancel));
        }

        // Crear y abrir la página del menú
        GooeyPage page = GooeyPage.builder()
          .title(AdventureTranslator.toNative(title))
          .template(template)
          .build();

        UIManager.openUIForcefully(player, page);
      }, CobbleUtils.EXECUTOR_COBBLEUTILS)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
  }
}