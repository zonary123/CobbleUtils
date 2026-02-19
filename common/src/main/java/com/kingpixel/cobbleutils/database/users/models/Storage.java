package com.kingpixel.cobbleutils.database.users.models;

import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.RateLimitedButton;
import com.google.gson.JsonElement;
import com.kingpixel.cobbleutils.ui.StorageMenu;
import com.kingpixel.cobbleutils.util.UtilsFile;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

  public abstract CompletableFuture<Boolean> giveToPlayer(ServerPlayerEntity playerEntity);

  public RateLimitedButton getButton(UUID targetUUID) {
    return RateLimitedButton.builder()
      .button(GooeyButton.builder()
        .display(getDisplay())
        .onClick(action -> {
          ServerPlayerEntity player = action.getPlayer();
          StorageMenu.removeStorage(player, this, targetUUID);
        })
        .build())
      .interval(1, TimeUnit.SECONDS)
      .limit(1)
      .build();
  }

  public Document toDocument() {
    String data = UtilsFile.getGson().toJson(this);
    return Document.parse(data);
  }

  public static Storage fromDocument(Document document) {
    String data = document.toJson();
    JsonElement jsonElement = UtilsFile.getGson().fromJson(data, JsonElement.class);
    return UtilsFile.getGson().fromJson(jsonElement, Storage.class);
  }
}
