package com.kingpixel.cobbleutils.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.UserModel;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.AdventureTranslator;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso
 */
@Data
public class StorageMenu {

  private int rows;
  private String title;
  private Rectangle rectangle;
  private ItemModel nextPage;
  private ItemModel close;
  private ItemModel previousPage;
  private ItemModel claimAll;
  private List<PanelsConfig> panels;

  public StorageMenu() {
    this.rows = 6;
    this.title = "Storage";
    this.rectangle = new Rectangle(rows);

    this.nextPage = new ItemModel("minecraft:arrow", "&aNext Page");
    this.nextPage.setSlot(53);

    this.previousPage = new ItemModel("minecraft:arrow", "&aPrevious Page");
    this.previousPage.setSlot(45);

    this.claimAll = new ItemModel("minecraft:chest", "&eClaim All Rewards");
    this.claimAll.setSlot(47);

    this.close = new ItemModel("minecraft:barrier", "&cClose");
    this.close.setSlot(49);

    this.panels = List.of(new PanelsConfig(rows));

    int totalSlots = rows * 9;
    for (PanelsConfig panel : panels) {
      panel.getSlots().removeIf(slot -> slot < 0 || slot >= totalSlots);
    }
  }

  public void open(ServerPlayerEntity executer, UUID targetUUID) {
    boolean isOwner = executer.getUuid().equals(targetUUID);
    CobbleUtils.runAsync(() -> {
      // Invalidate cache to ensure fresh data
      DataBaseFactory.dataBaseUsers.invalidateUser(targetUUID);
      UserModel userModel = DataBaseFactory.dataBaseUsers.findUser(targetUUID);
      if (userModel == null) return;

      if (userModel.getStorageList() == null) {
        CobbleUtils.LOGGER_RAW.warn("User {} has null storage list, initializing to empty set.", targetUUID);
        userModel.setStorageList(new HashSet<>());
      }

      ChestTemplate template = ChestTemplate.builder(rows).build();
      PanelsConfig.applyConfig(template, panels);
      rectangle.apply(template);

      List<Button> buttons = new ArrayList<>();
      List<Storage> invalidStorages = new ArrayList<>();

      for (Storage storage : userModel.getStorageList()) {
        try {
          buttons.add(storage.getButton(userModel, targetUUID));
        } catch (Exception e) {
          CobbleUtils.LOGGER_RAW.error("Invalid storage entry for user {}", targetUUID, e);
          invalidStorages.add(storage);
        }
      }

      if (!invalidStorages.isEmpty()) {
        DataBaseFactory.dataBaseUsers.removeStorageBatch(invalidStorages, targetUUID);
      }

      // CLAIM ALL — only show for the owner, not admins viewing someone else's storage
      if (isOwner) {
        claimAll.applyTemplate(template, claimAll.getButton(action -> {
          ServerPlayerEntity player = action.getPlayer();
          UIManager.closeUI(player);
          CobbleUtils.runAsync(() -> {
            // Invalidate + re-fetch to get truly fresh data
            DataBaseFactory.dataBaseUsers.invalidateUser(targetUUID);
            UserModel data = DataBaseFactory.dataBaseUsers.findUser(targetUUID);
            if (data == null || data.getStorageList().isEmpty()) {
              player.sendMessage(
                AdventureTranslator.toNative("&cYou don't have any pending rewards to claim."),
                false
              );
              return;
            }

            // Work on a copy — never mutate the cached object
            List<Storage> toClaim = new ArrayList<>(data.getStorageList());
            List<Storage> claimed = new ArrayList<>();

            for (Storage storage : toClaim) {
              try {
                if (storage.giveToPlayer(player)) {
                  claimed.add(storage);
                } else {
                  player.sendMessage(
                    AdventureTranslator.toNative("&cCould not claim reward: &e" + storage.getDisplay().getName().getString() + " &c(inventory full?)"),
                    false
                  );
                }
              } catch (Exception e) {
                CobbleUtils.LOGGER_RAW.error("Error claiming storage for {}", targetUUID, e);
              }
            }

            if (!claimed.isEmpty()) {
              DataBaseFactory.dataBaseUsers.removeStorageBatch(claimed, targetUUID);
            }

            player.sendMessage(
              AdventureTranslator.toNative("&aYou have successfully claimed &e" + claimed.size() + " &arewards!"),
              false
            );
            CobbleUtils.server.execute(() -> CobbleUtils.language.getStorageMenu().open(player, targetUUID));
          });
        }, 1, TimeUnit.SECONDS, 1));
      }

      // Navegación
      previousPage.applyTemplate(template, previousPage.getLinkedPageButton(LinkType.Previous));
      nextPage.applyTemplate(template, nextPage.getLinkedPageButton(LinkType.Next));
      close.applyTemplate(template,
        close.getButton(action -> UIManager.closeUI(action.getPlayer()))
      );

      GooeyPage page = PaginationHelper.createPagesFromPlaceholders(
        template,
        buttons,
        LinkedPage.builder().title(AdventureTranslator.toNative(title))
      );

      CobbleUtils.server.execute(() -> UIManager.openUIForcefully(executer, page));
    });
  }
}
