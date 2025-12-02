package com.kingpixel.cobbleutils.database.users.models;

import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import com.google.gson.JsonElement;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 06/10/2025 5:11
 */
@Data
public abstract class Storage {
  private UUID id;

  protected Storage() {
    this.id = UUID.randomUUID();
  }

  protected Storage(UUID id) {
    this.id = id;
  }

  public abstract ItemStack getDisplay();

  public abstract void giveToPlayer(ServerPlayerEntity playerEntity);

  public GooeyButton getButton() {
    return GooeyButton.builder()
      .display(getDisplay())
      .onClick(action -> CompletableFuture.runAsync(() -> {
          ServerPlayerEntity player = action.getPlayer();
          DataBaseFactory.dataBaseUsers.removeStorage(this, player.getUuid());
          this.giveToPlayer(player);
          CobbleUtils.language.getStorageMenu().open(player, player.getUuid());
        }, CobbleUtils.EXECUTOR_COBBLEUTILS)
        .exceptionally(e -> {
          e.printStackTrace();
          return null;
        }))
      .build();
  }

  public Document toDocument() {
    String data = Utils.newWithoutSpacingGson().toJson(this);
    return Document.parse(data);
  }

  public Storage fromDocument(Document document) {
    String data = document.toJson();
    JsonElement jsonElement = Utils.newWithoutSpacingGson().fromJson(data, JsonElement.class);
    return Utils.newWithoutSpacingGson().fromJson(jsonElement, this.getClass());
  }
}
