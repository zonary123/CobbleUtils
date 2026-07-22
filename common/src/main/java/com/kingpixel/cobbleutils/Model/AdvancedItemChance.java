package com.kingpixel.cobbleutils.Model;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.AnimationQueue;
import com.kingpixel.cobbleutils.Model.Animations.core.Animations;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.command.suggests.CobbleUtilsSuggests;
import com.kingpixel.cobbleutils.config.AdvancedRewardsConfig;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.database.users.models.StorageRewards;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Carlos Varas Alonso - 21/11/2024 3:15
 */
@Getter
@Setter
@ToString
public class AdvancedItemChance {
  // TODO: Add queue for the ANIMATIONS
  private String id;
  private boolean showMenu;
  private String title;
  private boolean giveAll;
  private boolean cumulativeLootTable;
  private final Map<String, Integer> amountRewardsPermission;
  private Sound newSound;
  private Particle particle;
  private Animations animation;
  private final Map<String, List<ItemChance>> lootTable;

  public AdvancedItemChance() {
    this.id = "";
    this.showMenu = true;
    this.title = "";
    this.giveAll = false;
    this.cumulativeLootTable = true;
    this.amountRewardsPermission = new HashMap<>();
    this.amountRewardsPermission.put("", 1);
    this.amountRewardsPermission.put("group.vip", 1);
    this.newSound = new Sound();
    this.particle = new Particle();
    this.animation = Animations.NONE;
    this.lootTable = new LinkedHashMap<>();
    lootTable.put("", ItemChance.defaultItemChances());
    List<ItemChance> itemChances = new ArrayList<>();
    itemChances.add(new ItemChance());
    lootTable.put("group.vip", itemChances);
    lootTable.entrySet().removeIf(entry -> entry.getValue().isEmpty());
  }

  private enum TypeError {
    NONE, AMOUNTREWARD, LOOTTABLE
  }

  public boolean checker(ServerPlayerEntity player) {
    TypeError typeError = TypeError.NONE;
    if (animation == null) animation = Animations.NONE;

    for (Map.Entry<String, Integer> entry : amountRewardsPermission.entrySet()) {
      int value = entry.getValue();
      if (value < 1) {
        typeError = TypeError.AMOUNTREWARD;
        break;
      }
    }


    lootTable.entrySet().removeIf(entry -> entry.getValue().isEmpty());

    return switch (typeError) {
      case AMOUNTREWARD -> {
        PlayerUtils.sendMessage(player,
          "%prefix% &cplease notify the administrator of the error in the configuration in the amountReward",
          "&7[&cERROR&7]",
          TypeMessage.CHAT);
        yield true;
      }
      default -> false;
    };

  }

  private int getAmountReward(ServerPlayerEntity player) {
    int maxAmount = 1;

    for (Map.Entry<String, Integer> entry : amountRewardsPermission.entrySet()) {
      if (PermissionApi.hasPermission(player, entry.getKey(), 2)) {
        maxAmount = Math.max(maxAmount, entry.getValue());
      }
    }

    return maxAmount;
  }

  public void giveRewards(UUID playerUUID) {
    var onlinePlayer = CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
    if (onlinePlayer != null) {
      giveRewardsInternal(onlinePlayer, true, playerUUID);
      return;
    }
    // Player is offline — try to get a fake player entity for permission checks etc.
    var optResult = CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.getPlayer(playerUUID);
    if (optResult.isPresent()) {
      giveRewardsInternal(optResult.get().player(), false, playerUUID);
    } else {
      CobbleUtils.LOGGER_RAW.error("Cannot give rewards: player {} not found (offline or online)", playerUUID);
    }
  }

  public void giveRewards(ServerPlayerEntity player) {
    giveRewardsInternal(player, true, player.getUuid());
  }

  /**
   * @param player     Target player entity.
   * @param online     Whether the player is online.
   * @param playerUUID UUID of the player.
   */
  private void giveRewardsInternal(ServerPlayerEntity player, boolean online, UUID playerUUID) {
    AdvancedItemChance active = this;

    try {
      String id = this.getId();

      if (id != null && !id.isEmpty()) {
        Map<String, AdvancedItemChance> templates =
          CobbleUtils.advancedRewardsConfig != null
            ? CobbleUtils.advancedRewardsConfig.getTEMPLATE_REWARDS()
            : null;

        if (templates != null) {
          AdvancedItemChance template = templates.get(id);
          if (template != null) {
            active = template;
          } else {
            PlayerUtils.sendMessage(
              player,
              "%prefix% &cReward template not found: &e" + id,
              "&7[&cERROR&7]",
              TypeMessage.CHAT
            );
          }
        } else {
          PlayerUtils.sendMessage(
            player,
            "%prefix% &cTemplate config is null",
            "&7[&cERROR&7]",
            TypeMessage.CHAT
          );
        }
      }

      checker(player);

      List<ItemChance> baseRewards = active.getList(player);

      if (baseRewards.isEmpty()) {
        PlayerUtils.sendMessage(
          player,
          "%prefix% &cNo rewards available from loot table",
          "&7[&cERROR&7]",
          TypeMessage.CHAT
        );
        return;
      }

      List<ItemChance> allRewards = new ArrayList<>(baseRewards);

      List<ItemChance> obtainedRewards;

      if (giveAll) {
        obtainedRewards = ItemChance.getAllRewards(baseRewards, player);
      } else {
        obtainedRewards = ItemChance.getRewards(
          baseRewards,
          player,
          active.getAmountReward(player)
        );
      }

      if (obtainedRewards.isEmpty()) {
        PlayerUtils.sendMessage(
          player,
          "%prefix% &cNo rewards obtained after roll",
          "&7[&cERROR&7]",
          TypeMessage.CHAT
        );
        return;
      }

      if (online) {

        for (ItemChance reward : obtainedRewards) {
          if (reward != null) {
            reward.giveReward(player);
          }
        }

        DataBaseFactory.dataBaseUsers.claimRewardsBatch(playerUUID, obtainedRewards);

        List<ItemStack> showAllRewards = active.getListDisplay(allRewards);
        List<ItemStack> showObtainedRewards = active.getListDisplay(obtainedRewards);

        if (active.getNewSound() != null) {
          active.getNewSound().start(player);
        }

        if (active.getParticle() != null) {
          active.getParticle().sendParticles(player, player);
        }

        initAnimation(
          active.getAnimation(),
          player,
          showAllRewards,
          showObtainedRewards
        );

      } else if (playerUUID != null) {

        List<Storage> storageList = new ArrayList<>();

        for (ItemChance reward : obtainedRewards) {
          if (reward != null) {
            storageList.add(new StorageRewards(reward));
          }
        }

        // Single user fetch, apply both claims and storage, single save
        DataBaseFactory.dataBaseUsers.claimRewardsAndAddStorage(playerUUID, obtainedRewards, storageList);
      }

    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Error giving rewards to {}", playerUUID, e);
      PlayerUtils.sendMessage(
        player,
        "%prefix% &cError while giving rewards. Contact admin. Time: " + new java.util.Date(),
        "&7[&cERROR&7]",
        TypeMessage.CHAT
      );
    }
  }


  public static void initAnimation(Animations animation, ServerPlayerEntity player, List<ItemStack> showAllRewards, List<ItemStack> showObtainedRewards) {
    if (animation == null || animation == Animations.NONE) return;
    AnimationQueue.enqueue(player, animation, showAllRewards, showObtainedRewards);
  }


  // Menus methods

  public void openMenu(ServerPlayerEntity player, Consumer<ChestTemplate> templateConsumer) {
    if (!showMenu) return;
    CobbleUtils.runAsync(() -> {
      int rows = CobbleUtils.language.getAdvancedRewardsGUI().getRows();
      ChestTemplate template = ChestTemplate.builder(rows <= 0 ? 6 : rows)
        .build();

      ItemModel itemClose = CobbleUtils.language.getAdvancedRewardsGUI().getClose();
      template.set(itemClose.getSlot(), itemClose.getButton(action -> UIManager.closeUI(action.getPlayer())));

      templateConsumer.accept(template);
      applyTemplate(player, template);
    });
  }

  public void openMenu(ServerPlayerEntity player, Consumer<ChestTemplate> templateConsumer,
                       Consumer<ButtonAction> close) {
    if (!showMenu) return;
    CobbleUtils.runAsync(() -> {
      int rows = CobbleUtils.language.getAdvancedRewardsGUI().getRows();
      ChestTemplate template = ChestTemplate.builder(rows <= 0 ? 6 : rows)
        .build();

      ItemModel itemClose = CobbleUtils.language.getAdvancedRewardsGUI().getClose();
      template.set(itemClose.getSlot(), itemClose.getButton(close));

      templateConsumer.accept(template);
      applyTemplate(player, template);
    });
  }

  @Deprecated
  public void openMenu(ServerPlayerEntity player) {
    if (!showMenu) return;
    CobbleUtils.runAsync(() -> {
      int rows = CobbleUtils.language.getAdvancedRewardsGUI().getRows();
      ChestTemplate template = ChestTemplate.builder(rows <= 0 ? 6 : rows)
        .build();

      ItemModel itemClose = CobbleUtils.language.getAdvancedRewardsGUI().getClose();
      template.set(itemClose.getSlot(), itemClose.getButton(action -> {
        UIManager.closeUI(action.getPlayer());
      }));
      applyTemplate(player, template);
    });
  }

  private void applyTemplate(ServerPlayerEntity player, ChestTemplate template) {
    AdvancedItemChance finish;
    if (getId() != null && !getId().isEmpty()) {
      finish = AdvancedRewardsConfig.getAdvancedReward(getId());
      if (finish == null) {
        PlayerUtils.sendMessage(player,
          "%prefix% &cThe Advanced Reward Template with id &e" + this.getId() + " &cdoes not exist, please notify the" +
            " " +
            "administrator of the error",
          "&7[&cERROR&7]",
          TypeMessage.CHAT);
        return;
      }
    } else {
      finish = this;
    }


    PanelsConfig.applyConfig(template, CobbleUtils.language.getAdvancedRewardsGUI().getPanels());
    Rectangle rectangle = new Rectangle(1, 1, 4, 7);
    rectangle.apply(template);

    List<Button> buttons = finish.getButtons(player, finish);

    int freeSlots = rectangle.getSlotsFree(template.getRows());

    ItemModel info = CobbleUtils.language.getItemAdvancedRewardsInfo();

    List<String> infoLore = new ArrayList<>(info.getLore());
    infoLore.replaceAll(s -> s
      .replace("%amount%", String.valueOf(finish.getAmountReward(player)))
      .replace("%getall%", giveAll ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo()));

    if (info.getSlot() >= 0) {
      template.set(info.getSlot(), GooeyButton.builder()
        .display(info.getItemStack())
        .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(infoLore, player)))
        .build());
    }

    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER_RAW.info("Free slots: " + freeSlots);
    }

    if (buttons.size() > freeSlots) {
      ItemModel itemNext = CobbleUtils.language.getAdvancedRewardsGUI().getNext();
      template.set(itemNext.getSlot(), LinkedPageButton.builder()
        .display(itemNext.getItemStack())
        .linkType(LinkType.Next)
        .build());

      ItemModel itemPrevious = CobbleUtils.language.getAdvancedRewardsGUI().getPrevious();
      template.set(itemPrevious.getSlot(), LinkedPageButton.builder()
        .display(itemPrevious.getItemStack())
        .linkType(LinkType.Previous)
        .build());
    }

    LinkedPage.Builder linkedPageBuilder = LinkedPage.builder()
      .title(AdventureTranslator.toNative(title == null || title.isEmpty() ? CobbleUtils.language.getTitleLoot() : title));

    GooeyPage page = PaginationHelper.createPagesFromPlaceholders(template, buttons, linkedPageBuilder);

    CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
  }

  public List<Button> getButtons(ServerPlayerEntity player, AdvancedItemChance finish) {
    List<Button> buttons = new ArrayList<>();

    // Calculate total weight fresh each time — loot table may change via config reload
    double totalWeight = 0;
    List<ItemChance> itemChances = finish.getList(player);
    for (ItemChance item : itemChances) {
      totalWeight += item.getChance();
    }

    for (Map.Entry<String, List<ItemChance>> entry : lootTable.entrySet()) {
      String key = entry.getKey();
      List<ItemChance> chances = entry.getValue();

      for (ItemChance itemChance : chances) {
        boolean hasPermission = PermissionApi.hasPermission(player, key, 2);
        double chance = hasPermission ? itemChance.getChance() : 0.0;
        buttons.add(getButton(itemChance, key, chance, hasPermission, totalWeight));
      }
    }

    return buttons;
  }


  private GooeyButton getButton(ItemChance itemChance, String permission, double chance, boolean havePermission,
                                double totalWeight) {
    // Calcula el porcentaje basado en el peso total
    double percentage = totalWeight > 0 ? (chance / totalWeight) * 100 : 0;

    // Prepara el lore para mostrar el porcentaje calculado
    String name;
    if (itemChance.getDisplayname() != null) {
      name = itemChance.getDisplayname();
    } else {
      name = itemChance.getTitle();
    }

    List<String> lore = new ArrayList<>(CobbleUtils.language.getLorechance());
    if (giveAll) {
      lore.removeIf(s -> s.contains("%chance%"));
    } else {
      lore.replaceAll(s -> s.replace("%chance%", String.format("%.2f", percentage)));
    }
    if (!havePermission) {
      lore.add(CobbleUtils.language.getMessagePermissionRewards()
        .replace("%permission%", permission));
    }


    return GooeyButton.builder()
      .display(getDisplay(itemChance))
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(name))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)))
      .build();
  }

  private ItemStack getDisplay(ItemChance itemChance) {
    return itemChance.getIcon();
  }

  private List<ItemStack> getListDisplay(List<ItemChance> itemChances) {
    List<ItemStack> itemStacks = new ArrayList<>();
    itemChances.forEach(itemChance -> itemStacks.add(getDisplay(itemChance)));
    return itemStacks;
  }


  private List<ItemChance> getList(ServerPlayerEntity player) {
    List<ItemChance> result = new ArrayList<>();

    if (lootTable == null) return result;

    for (Map.Entry<String, List<ItemChance>> entry : lootTable.entrySet()) {
      if (entry.getValue() != null && PermissionApi.hasPermission(player, entry.getKey(), 2)) {
        result.addAll(entry.getValue());
        if (!cumulativeLootTable) {
          break;
        }
      }
    }

    return result;
  }

}

