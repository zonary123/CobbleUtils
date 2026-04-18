package com.kingpixel.cobbleutils.database.users.models;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import com.google.gson.JsonElement;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.UserModel;
import com.kingpixel.cobbleutils.util.UtilsFile;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bson.Document;

import java.util.UUID;

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

  public abstract boolean giveToPlayer(ServerPlayerEntity playerEntity);

  public GooeyButton getButton(UserModel userModel, UUID targetUUID) {
    return GooeyButton.builder()
      .display(getDisplay())
      .onClick(action -> {
        ServerPlayerEntity player = action.getPlayer();
        // Only the owner can claim — not an admin viewing
        if (!player.getUuid().equals(targetUUID)) return;
        UIManager.closeUI(player);
        if (this.giveToPlayer(player)) {
          CobbleUtils.runAsync(() -> {
            DataBaseFactory.dataBaseUsers.removeStorage(this, targetUUID);
            CobbleUtils.server.execute(() ->
              CobbleUtils.language.getStorageMenu().open(player, targetUUID));
          });
        } else {
          player.sendMessage(
            net.minecraft.text.Text.of("§cCould not claim this reward. Is your inventory full?"),
            false
          );
          CobbleUtils.server.execute(() ->
            CobbleUtils.language.getStorageMenu().open(player, targetUUID));
        }
      })
      .build();
  }

  public Document toDocument() {
    String data = UtilsFile.getGson().toJson(this);
    return Document.parse(data);
  }

  public Storage fromDocument(Document document) {
    String data = document.toJson();
    JsonElement jsonElement = UtilsFile.getGson().fromJson(data, JsonElement.class);
    return UtilsFile.getGson().fromJson(jsonElement, this.getClass());
  }
}
