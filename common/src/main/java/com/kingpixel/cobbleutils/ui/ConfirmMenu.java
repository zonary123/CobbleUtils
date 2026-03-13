package com.kingpixel.cobbleutils.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.config.Lang;
import com.kingpixel.cobbleutils.model.ItemModel;
import com.kingpixel.cobbleutils.model.PanelsConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Data
public class ConfirmMenu {
  private boolean useDefault = false;
  private int rows;
  private String title;
  private int slotDisplay;
  private long customModelDataConfirm;
  private ItemModel confirm;
  private ItemModel cancel;
  private ItemModel close;
  private List<PanelsConfig> panels;

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


  public CompletableFuture<Void> open(ConfirmMenuData data) {
    return CobbleUtils.ASYNC.runAsync(() -> {
      ConfirmMenu confirmMenu = this;
      if (useDefault) confirmMenu = CobbleUtils.language.getConfirmMenu();
      confirmMenu.openFinal(data);
    });
  }

  private void openFinal(ConfirmMenuData data) {
    ServerPlayerEntity player = data.getPlayer();
    ChestTemplate template = ChestTemplate
      .builder(data.getRows(rows))
      .build();
    PanelsConfig.applyConfig(template, panels);

    ItemStack display = data.getDisplay();
    if (customModelDataConfirm >= 0) {
      display.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent((int) customModelDataConfirm));
    }
    template.set(slotDisplay, GooeyButton.builder()
      .display(display)
      .build()
    );

    Consumer<ChestTemplate> templateConsumer = data.getTemplate();
    if (templateConsumer != null) templateConsumer.accept(template);


    Consumer<ButtonAction> onConfirm = data.getOnConfirm();
    if (confirm != null) confirm.applyTemplate(template, confirm.getButton(onConfirm, 1, TimeUnit.SECONDS, 1));

    Consumer<ButtonAction> onCancel = data.getOnCancel();
    if (cancel != null) cancel.applyTemplate(template, cancel.getButton(onCancel, 1, TimeUnit.SECONDS, 1));
    if (close != null) close.applyTemplate(template, close.getButton(onCancel, 1, TimeUnit.SECONDS, 1));

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title(data.getTitle(title))
      .build();

    CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
  }


  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class ConfirmMenuData {
    private ServerPlayerEntity player;
    private String title;
    private Integer rows;
    private ItemStack display;
    private Consumer<ChestTemplate> template;
    private Consumer<ButtonAction> onConfirm;
    private Consumer<ButtonAction> onCancel;

    public String getTitle(String title) {
      return this.title != null ? this.title : title;
    }

    public Integer getRows(int rows) {
      return this.rows != null ? this.rows : rows;
    }
  }

}