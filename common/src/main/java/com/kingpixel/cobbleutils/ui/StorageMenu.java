package com.kingpixel.cobbleutils.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.UserModel;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 06/10/2025 5:14
 */
@Data
public class StorageMenu {
  private int rows;
  private String title;
  private Rectangle rectangle;
  private ItemModel nextPage;
  private ItemModel close;
  private ItemModel previousPage;

  public StorageMenu() {
    this.rows = 6;
    this.title = "Storage";
    this.rectangle = new Rectangle(rows);
    this.nextPage = new ItemModel("minecraft:arrow", "&aNext Page");
    this.nextPage.setSlot(53);
    this.close = new ItemModel("minecraft:barrier", "&cClose");
    this.close.setSlot(49);
    this.previousPage = new ItemModel("minecraft:arrow", "&aPrevious Page");
    this.previousPage.setSlot(45);
  }

  public void open(ServerPlayerEntity executer, UUID targetUUID) {
    CompletableFuture.runAsync(() -> {
        ChestTemplate template = ChestTemplate
          .builder(rows)
          .build();

        rectangle.apply(template);
        UserModel userModel = DataBaseFactory.dataBaseUsers.findUserByUUID(targetUUID);
        if (userModel == null) return;
        var list = userModel.getStorageList();
        int size = list.size();
        List<Button> buttons = new ArrayList<>(size);
        for (Storage storage : list) {
          buttons.add(storage.getButton());
        }


        previousPage.applyTemplate(template, previousPage.getLinkedPageButton(LinkType.Next));
        close.applyTemplate(template, close.getButton(action -> UIManager.closeUI(action.getPlayer()), 1, TimeUnit.SECONDS, 1));
        nextPage.applyTemplate(template, nextPage.getLinkedPageButton(LinkType.Next));

        GooeyPage page = PaginationHelper.createPagesFromPlaceholders(
          template,
          buttons,
          LinkedPage.builder().title(AdventureTranslator.toNative(title))
        );
        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(executer, page));
      }, CobbleUtils.EXECUTOR_COBBLEUTILS)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
  }
}
