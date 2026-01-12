package com.kingpixel.cobbleutils.database.users.models;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import com.google.gson.JsonElement;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.UserModel;
import com.kingpixel.cobbleutils.util.Utils;
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

  public abstract void giveToPlayer(ServerPlayerEntity playerEntity);

  public GooeyButton getButton(UserModel userModel) {
    return GooeyButton.builder()
      .display(getDisplay())
      .onClick(action -> {
        ServerPlayerEntity player = action.getPlayer();
        UIManager.closeUI(player);
        userModel.getStorageList().remove(this);
        this.giveToPlayer(player);
        CobbleUtils.runAsync(() -> {
          DataBaseFactory.dataBaseUsers.removeStorage(this, player.getUuid());
          CobbleUtils.language.getStorageMenu().open(player, player.getUuid());
        });
      })
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
