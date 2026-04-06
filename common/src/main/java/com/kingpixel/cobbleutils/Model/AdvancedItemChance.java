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
import com.kingpixel.cobbleutils.Model.Animations.AllRewardsCircleAnimation;
import com.kingpixel.cobbleutils.Model.Animations.AnimationUtils;
import com.kingpixel.cobbleutils.Model.Animations.CSGOAnimation;
import com.kingpixel.cobbleutils.Model.Animations.CircleAnimation;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.command.suggests.CobbleUtilsSuggests;
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
import net.minecraft.util.math.Vec3d;

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
  // Title of the menu rewards
  private boolean showMenu;
  private String title;
  // Options for the rewards
  private boolean giveAll;
  private final Map<String, Integer> amountRewardsPermission;
  // Effects and animations
  private Sound newSound;
  private Particle particle;
  private Animations animation;
  // Rewards
  private final Map<String, List<ItemChance>> lootTable;

  public AdvancedItemChance() {
    this.id = "";
    this.showMenu = true;
    this.title = "";
    this.giveAll = false;
    this.amountRewardsPermission = new HashMap<>();
    this.amountRewardsPermission.put("", 1);
    this.amountRewardsPermission.put("group.vip", 1);
    //this.sound = "minecraft:block.note_block.harp";
    this.newSound = new Sound();
    this.particle = new Particle();
    this.animation = Animations.NONE;
    this.lootTable = new HashMap<>();
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
    int amount = 1;
    for (Map.Entry<String, Integer> entry : amountRewardsPermission.entrySet()) {
      if (PermissionApi.hasPermission(player, entry.getKey(), 2)) {
        if (entry.getValue() > amount) {
          amount = entry.getValue();
        }
      }
    }
    return amount;
  }

  public void giveRewards(UUID playerUUID) {
    var player = CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
    CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.getPlayer(playerUUID)
      .ifPresent(dataResultPlayer -> giveRewardsInternal(dataResultPlayer.player(), player != null, playerUUID));
  }

  public void giveRewards(ServerPlayerEntity player) {
    giveRewardsInternal(player, true, player.getUuid());
  }

  /**
   * Lógica común para dar recompensas a un jugador.
   *
   * @param player     Jugador objetivo.
   * @param online     Si el jugador está online.
   * @param playerUUID UUID del jugador si está offline, null si es online.
   */
  private void giveRewardsInternal(ServerPlayerEntity player, boolean online, UUID playerUUID) {
    try {
      AdvancedItemChance finish = this;
      if (finish.getId() != null && !finish.getId().isEmpty()) {
        finish = CobbleUtils.advancedRewardsConfig.getTEMPLATE_REWARDS().get(finish.getId());
        if (finish == null) {
          PlayerUtils.sendMessage(player,
            "%prefix% &cThe Advanced Reward Template with id &e" + this.getId() + " &cdoes not exist, please notify the " +
              "administrator" +
              " of the error",
            "&7[&cERROR&7]",
            TypeMessage.CHAT);
          finish = this;
        }
      }

      checker(player);

      List<ItemChance> obtainedRewards = finish.getList(player);
      List<ItemChance> allRewards = new ArrayList<>(obtainedRewards);

      if (giveAll) {
        obtainedRewards = ItemChance.getAllRewards(obtainedRewards, player);
      } else {
        obtainedRewards = ItemChance.getRewards(obtainedRewards, player, finish.getAmountReward(player));
      }

      if (obtainedRewards.isEmpty()) {
        PlayerUtils.sendMessage(player,
          "%prefix% &cYou have not obtained any rewards, please try again",
          "&7[&cERROR&7]",
          TypeMessage.CHAT);
        return;
      }

      if (online) {
        for (ItemChance reward : obtainedRewards) {
          reward.giveReward(player);
        }

        List<ItemStack> showAllRewards = finish.getListDisplay(allRewards);
        List<ItemStack> showObtainedRewards = finish.getListDisplay(obtainedRewards);

        if (finish.getNewSound() != null) finish.getNewSound().start(player);
        if (finish.getParticle() != null) finish.getParticle().sendParticles(player, player);
        initAnimation(finish.getAnimation(), player, showAllRewards, showObtainedRewards);

      } else if (playerUUID != null) {
        List<Storage> storageList = new ArrayList<>();
        for (ItemChance reward : obtainedRewards) {
          storageList.add(new StorageRewards(reward));
        }
        DataBaseFactory.dataBaseUsers.addStorage(storageList, playerUUID);
      }
    } catch (Exception e) {
      e.printStackTrace();
      PlayerUtils.sendMessage(player,
        "%prefix% &cAn error occurred while trying to give you the rewards, please notify the administrator of the error date: " + new Date().toString(),
        "&7[&cERROR&7]",
        TypeMessage.CHAT);
    }
  }


  public static void initAnimation(Animations animation, ServerPlayerEntity player, List<ItemStack> showAllRewards, List<ItemStack> showObtainedRewards) {
    Vec3d centerPosition = AnimationUtils.getPosition(player, null);
    switch (animation) {
      case CSGO:
        CSGOAnimation.start(player, showAllRewards, showObtainedRewards);
        break;
      case CIRCLE:
        CircleAnimation.start(player, showObtainedRewards, centerPosition);
        break;
      case ALLCIRCLE:
        AllRewardsCircleAnimation.start(player, showAllRewards, centerPosition);
        break;
      default:
        break;
    }
  }

  public enum Animations {
    NONE, // No animation
    RANDOM,
    CSGO, // Show the items in a CSGO style
    CIRCLE, // Show the items in a circle style
    ALLCIRCLE // Show all the items in a circle style
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
      finish = CobbleUtils.advancedRewardsConfig.getTEMPLATE_REWARDS().get(getId());
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

  // Add this field to your class
  private transient double cachedTotalWeight = -1;

  public List<Button> getButtons(ServerPlayerEntity player, AdvancedItemChance finish) {
    List<Button> buttons = new ArrayList<>();

    // ✅ Only recalculate if cache is invalid
    if (cachedTotalWeight < 0) {
      double total = 0;
      List<ItemChance> itemChances = finish.getList(player);
      for (ItemChance item : itemChances) {
        total += item.getChance();
      }
      cachedTotalWeight = total;
    }

    // ✅ Build buttons using cached total weight
    for (Map.Entry<String, List<ItemChance>> entry : lootTable.entrySet()) {
      String key = entry.getKey();
      List<ItemChance> chances = entry.getValue();

      for (ItemChance itemChance : chances) {
        boolean hasPermission = PermissionApi.hasPermission(player, key, 2);
        double chance = hasPermission ? itemChance.getChance() : 0.0;
        buttons.add(getButton(itemChance, key, chance, hasPermission, cachedTotalWeight));
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
    List<ItemChance> itemChances = new ArrayList<>();
    lootTable.forEach((key, value) -> {
      if (PermissionApi.hasPermission(player, key, 2)) {
        itemChances.addAll(value);
      }
    });
    return itemChances;
  }

}

