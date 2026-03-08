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
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

  public CompletableFuture<Void> open(ServerPlayerEntity executor, UUID targetUUID) {
    CobbleUtils.server.execute(() -> UIManager.closeUI(executor));
    return DataBaseFactory.dataBaseUsers.findUserStorage(targetUUID)
      .exceptionally(e -> {
        e.printStackTrace();
        CobbleUtils.server.execute(() -> {
          executor.sendMessage(
            AdventureTranslator.toNative("&cAn error occurred while fetching your storage. Please try again later."),
            false
          );
          UIManager.closeUI(executor);
        });
        return null;
      })
      .thenCompose(storageList -> {
        if (storageList == null) storageList = new HashSet<>();
        if (storageList.isEmpty()) {
          CobbleUtils.server.execute(() -> {
            executor.sendMessage(
              AdventureTranslator.toNative("&cYou don't have any pending rewards to claim."),
              false
            );
            UIManager.closeUI(executor);
          });
          return CompletableFuture.completedFuture(null);
        }
        ChestTemplate template = ChestTemplate.builder(rows).build();
        PanelsConfig.applyConfig(template, panels);
        rectangle.apply(template);

        List<Button> buttons = new ArrayList<>();
        List<Storage> invalidStorages = new ArrayList<>();

        for (Storage storage : storageList) {
          try {
            buttons.add(storage.getButton(executor, targetUUID));
          } catch (Exception e) {
            e.printStackTrace();
            invalidStorages.add(storage);
          }
        }

        if (!invalidStorages.isEmpty()) {
          invalidStorages.forEach(storageList::remove);
          DataBaseFactory.dataBaseUsers.removeStorage(invalidStorages, targetUUID);
        }

        claimAll.applyTemplate(template, claimAll.getButton(action -> DataBaseFactory.dataBaseUsers.findUserStorage(targetUUID)
          .thenAccept(storages -> {
            if (storages == null || storages.isEmpty()) {
              CobbleUtils.server.execute(() ->
                executor.sendMessage(
                  AdventureTranslator.toNative("&cYou don't have any pending rewards to claim."),
                  false
                )
              );
              return;
            }

            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            for (Storage storage : storages) {
              futures.add(removeStorage(executor, storage, targetUUID));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
              .thenRun(() -> CobbleUtils.language.getStorageMenu().open(executor, targetUUID));
          }), 1, TimeUnit.MINUTES, 1));

        previousPage.applyTemplate(template, previousPage.getLinkedPageButton(LinkType.Previous));
        nextPage.applyTemplate(template, nextPage.getLinkedPageButton(LinkType.Next));
        close.applyTemplate(template, close.getButton(action -> UIManager.closeUI(action.getPlayer()), 1, TimeUnit.SECONDS, 1));

        GooeyPage page = PaginationHelper.createPagesFromPlaceholders(
          template,
          buttons,
          LinkedPage.builder().title(AdventureTranslator.toNative(title))
        );

        CobbleUtils.server.execute(() -> UIManager.openUIForcefully(executor, page));
        return CompletableFuture.completedFuture(null);
      });
  }

  public static CompletableFuture<Boolean> removeStorage(@NotNull ServerPlayerEntity player, Storage storage, UUID targetUUID) {
    return DataBaseFactory.dataBaseUsers.removeStorage(storage, targetUUID)
      .thenCompose(removed -> {
        if (!removed) {
          player.sendMessage(
            AdventureTranslator.toNative("&cAn error occurred while claiming your reward. Please try again later."),
            false
          );
          return CompletableFuture.completedFuture(false);
        }
        return storage.giveToPlayer(player)
          .thenCompose(given -> {
            if (given) return CompletableFuture.completedFuture(true);
            return DataBaseFactory.dataBaseUsers.addStorage(storage, targetUUID)
              .thenApply(restored -> {
                player.sendMessage(
                  AdventureTranslator.toNative("&cYour reward could not be delivered. Please try again."),
                  false
                );
                return false;
              });
          });
      })
      .handle((result, throwable) -> {
        if (throwable != null) {
          throwable.printStackTrace();

          DataBaseFactory.dataBaseUsers.addStorage(storage, targetUUID);
          player.sendMessage(
            AdventureTranslator.toNative("&cAn unexpected error occurred while claiming your reward."),
            false
          );
          return false;
        }
        return result;
      });
  }
}