package com.kingpixel.cobbleutils.Model;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.AllRewardsCircleAnimation;
import com.kingpixel.cobbleutils.Model.Animations.AnimationUtils;
import com.kingpixel.cobbleutils.Model.Animations.CSGOAnimation;
import com.kingpixel.cobbleutils.Model.Animations.CircleAnimation;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Carlos Varas Alonso - 21/11/2024 3:15
 */
@Getter
@Setter
@ToString
public class AdvancedItemChance {
  // TODO: Add queue for the ANIMATIONS
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
    this.showMenu = true;
    this.title = "";
    this.giveAll = false;
    this.amountRewardsPermission = new HashMap<>();
    this.amountRewardsPermission.put("", 1);
    this.amountRewardsPermission.put("group.vip", 1);
    //this.sound = "minecraft:block.note_block.harp";
    this.newSound = new Sound();
    this.particle = new Particle();
    this.animation = Animations.CIRCLE;
    this.lootTable = new HashMap<>();
    lootTable.put("", ItemChance.defaultItemChances());
    List<ItemChance> itemChances = new ArrayList<>();
    itemChances.add(new ItemChance());
    lootTable.put("group.vip", itemChances);
  }

  private enum TypeError {
    NONE, AMOUNTREWARD, LOOTTABLE
  }

  public boolean checker(ServerPlayerEntity player) {
    TypeError typeError = TypeError.NONE;

    for (Map.Entry<String, Integer> entry : amountRewardsPermission.entrySet()) {
      int value = entry.getValue();
      if (value < 1) {
        typeError = TypeError.AMOUNTREWARD;
        break;
      }
    }

    String extraInfo = "";

    for (Map.Entry<String, List<ItemChance>> entry : lootTable.entrySet()) {
      if (entry.getValue().isEmpty()) {
        typeError = TypeError.LOOTTABLE;
        extraInfo = entry.getKey();
        break;
      }
    }


    if (lootTable == null || lootTable.isEmpty()) {
      typeError = TypeError.LOOTTABLE;
      extraInfo = "Error lootTable is null or empty";
    }

    return switch (typeError) {
      case AMOUNTREWARD -> {
        PlayerUtils.sendMessage(player,
          "%prefix% &cplease notify the administrator of the error in the configuration in the amountReward",
          "&7[&cERROR&7]",
          TypeMessage.CHAT);
        yield true;
      }
      case LOOTTABLE -> {
        PlayerUtils.sendMessage(player,
          "%prefix% &cplease notify the administrator of the error in the configuration in the lootTable -> %extra%"
            .replace("%extra%", extraInfo),
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

  public void giveRewards(ServerPlayerEntity player) {
    if (checker(player)) return;

    List<ItemChance> obtainedRewards = getList(player);
    List<ItemChance> allRewards = obtainedRewards;

    if (giveAll) {
      ItemChance.getAllRewards(obtainedRewards, player);
    } else {
      obtainedRewards = ItemChance.getRewards(obtainedRewards, player, getAmountReward(player));
    }

    if (getNewSound() != null) {
      //SoundUtil.playSound(sound, player);
      getNewSound().start(player);
    }

    if (particle != null) {
      particle.sendParticles(player, player);
    }
    List<ItemStack> showAllRewards = getListDisplay(allRewards);
    List<ItemStack> showObtainedRewards = getListDisplay(obtainedRewards);


    initAnimation(animation, player, showAllRewards, showObtainedRewards);
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
    CompletableFuture.runAsync(() -> {
        ChestTemplate template = ChestTemplate.builder(6)
          .build();

        ItemModel itemClose = CobbleUtils.language.getItemClose();
        template.set(49, itemClose.getButton(action -> {
          UIManager.closeUI(action.getPlayer());
        }));

        templateConsumer.accept(template);
        applyTemplate(player, template);
      })
      .orTimeout(5, TimeUnit.SECONDS)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
  }

  public void openMenu(ServerPlayerEntity player, Consumer<ChestTemplate> templateConsumer,
                       Consumer<ButtonAction> close) {
    if (!showMenu) return;
    CompletableFuture.runAsync(() -> {
        ChestTemplate template = ChestTemplate.builder(6)
          .build();

        ItemModel itemClose = CobbleUtils.language.getItemClose();
        template.set(49, itemClose.getButton(close));

        templateConsumer.accept(template);
        applyTemplate(player, template);
      })
      .orTimeout(5, TimeUnit.SECONDS)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });

  }

  @Deprecated
  public void openMenu(ServerPlayerEntity player) {
    if (!showMenu) return;
    CompletableFuture.runAsync(() -> {
        ChestTemplate template = ChestTemplate.builder(6)
          .build();

        template.set(49, CobbleUtils.language.getItemClose().getButton(action -> {
          UIManager.closeUI(action.getPlayer());
        }));
        applyTemplate(player, template);
      })
      .orTimeout(5, TimeUnit.SECONDS)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });

  }

  private void applyTemplate(ServerPlayerEntity player, ChestTemplate template) {
    List<Button> buttons = getButtons(player);

    Rectangle rectangle = new Rectangle(1, 1, 4, 7);
    rectangle.apply(template);

    int freeSlots = rectangle.getSlotsFree(template.getRows());

    ItemModel info = CobbleUtils.language.getItemAdvancedRewardsInfo();

    List<String> infoLore = new ArrayList<>(info.getLore());
    infoLore.replaceAll(s -> s
      .replace("%amount%", String.valueOf(getAmountReward(player)))
      .replace("%getall%", giveAll ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo()));

    if (info.getSlot() >= 0) {
      template.set(info.getSlot(), GooeyButton.builder()
        .display(info.getItemStack())
        .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(infoLore, player)))
        .build());
    }

    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Free slots: " + freeSlots);
    }

    if (buttons.size() > freeSlots) {
      ItemModel itemNext = CobbleUtils.language.getItemNext();
      template.set(53, LinkedPageButton.builder()
        .display(itemNext.getItemStack())
        .linkType(LinkType.Next)
        .build());

      ItemModel itemPrevious = CobbleUtils.language.getItemPrevious();
      template.set(45, LinkedPageButton.builder()
        .display(itemPrevious.getItemStack())
        .linkType(LinkType.Previous)
        .build());
    }


    template.fill(GooeyButton.builder()
      .display(Utils.parseItemId(CobbleUtils.config.getFill()))
      .with(DataComponentTypes.CUSTOM_NAME, Text.empty())
      .build());

    LinkedPage.Builder linkedPageBuilder = LinkedPage.builder()
      .title(AdventureTranslator.toNative(title == null || title.isEmpty() ? CobbleUtils.language.getTitleLoot() : title));


    UIManager.openUIForcefully(player, PaginationHelper.createPagesFromPlaceholders(template, buttons,
      linkedPageBuilder));
  }

  public List<Button> getButtons(ServerPlayerEntity player) {
    List<Button> buttons = new ArrayList<>();
    double totalWeight = getList(player).stream().mapToDouble(ItemChance::getChance).sum();
    lootTable.forEach((key, value) -> {
      value.forEach(itemChance -> {
        boolean hasPermission = PermissionApi.hasPermission(player, key, 2);
        double chance = hasPermission ? itemChance.getChance() : 0;
        buttons.add(getButton(itemChance, key, chance, hasPermission, totalWeight));
      });
    });
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
    lore.replaceAll(s -> s.replace("%chance%", String.format("%.2f", percentage)));
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
    ItemStack display = itemChance.getItemStack();
    if (itemChance.getDisplay() != null && !itemChance.getDisplay().isEmpty())
      display = new ItemChance(itemChance.getDisplay(), 100).getItemStack();
    return display;
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


