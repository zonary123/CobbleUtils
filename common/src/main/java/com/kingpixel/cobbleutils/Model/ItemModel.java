package com.kingpixel.cobbleutils.Model;

import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.RateLimitedButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.mixins.gooeylibs.GooeyButtonMixin;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.ItemUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.UIUtils;
import com.mongodb.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Carlos Varas Alonso - 28/06/2024 20:02
 */
@ToString
@Data
@Builder
@AllArgsConstructor
public class ItemModel {
  @Builder.Default
  private Integer slot = -1;
  @Builder.Default
  private Integer[] slots = new Integer[]{};
  @Builder.Default
  private String item = "minecraft:emerald";
  @Builder.Default
  private String displayname = "";
  @Builder.Default
  private List<String> lore = new ArrayList<>();
  @Builder.Default
  private long CustomModelData = 0;
  private String nbt;
  private Object tooltip;


  public ItemModel() {
    this.slot = -1;
    this.slots = new Integer[]{};
    this.item = "minecraft:emerald";
    this.displayname = "";
    this.lore = new ArrayList<>();
    this.CustomModelData = 0;
    this.nbt = "";
  }

  public ItemModel(String item) {
    this.slot = 0;
    this.slots = new Integer[]{};
    this.item = item;
    this.displayname = "";
    this.lore = new ArrayList<>();
    this.CustomModelData = 0;
    this.nbt = "";
  }

  public ItemModel(String item, String displayname) {
    this.slot = 0;
    this.slots = new Integer[]{};
    this.item = item;
    this.displayname = displayname;
    this.lore = new ArrayList<>();
    this.CustomModelData = 0;
    this.nbt = "";
  }

  public ItemModel(String item, String displayname, List<String> lore) {
    this.slot = 0;
    this.slots = new Integer[]{};
    this.item = item;
    this.displayname = displayname;
    this.lore = lore;
  }

  public ItemModel(String item, String displayname, List<String> lore, int customModelData) {
    this.slot = 0;
    this.slots = new Integer[]{};
    this.item = item;
    this.displayname = displayname;
    this.lore = lore;
    this.CustomModelData = customModelData;
  }

  public ItemModel(Integer slot, String item, String displayname, List<String> lore, int customModelData) {
    this.slot = slot;
    this.item = item;
    this.displayname = displayname;
    this.lore = lore;
    CustomModelData = customModelData;
  }

  public ItemModel(Integer slot, String item, String displayname, List<String> lore, int customModelData, String nbt) {
    this.slot = slot;
    this.item = item;
    this.displayname = displayname;
    this.lore = lore;
    CustomModelData = customModelData;
    this.nbt = nbt;
  }

  public ItemModel(String item, String displayname, List<String> lore, long customModelData) {
    this.slot = 0;
    this.item = item;
    this.displayname = displayname;
    this.lore = lore;
    this.CustomModelData = customModelData;
  }

  public ItemModel(Integer slot, String item, String displayname, List<String> lore, long customModelData) {
    this.slot = slot;
    this.item = item;
    this.displayname = displayname;
    this.lore = lore;
    CustomModelData = customModelData;
  }

  public ItemModel(Integer slot, String item, String displayname, List<String> lore, long customModelData, String nbt) {
    this.slot = slot;
    this.item = item;
    this.displayname = displayname;
    this.lore = lore;
    CustomModelData = customModelData;
    this.nbt = nbt;
  }

  public ItemModel(ItemModel itemMoney) {
    this.slot = itemMoney.getSlot();
    this.slots = itemMoney.getSlots();
    this.item = itemMoney.getItem();
    this.displayname = itemMoney.getDisplayname();
    this.lore = itemMoney.getLore();
    CustomModelData = itemMoney.getCustomModelData();
    this.nbt = itemMoney.getNbt();
  }

  /**
   * Get the itemstack of the item
   *
   * @return The itemstack of the item
   */
  public ItemStack getItemStack() {
    return getItemStack(this);
  }

  /**
   * Get the itemstack of the item
   *
   * @param amount The amount of the item
   * @return The itemstack of the item
   */
  public ItemStack getItemStack(int amount) {
    return getItemStack(this, amount);
  }

  /**
   * Get the itemstack of the item
   *
   * @param itemModel The item model to get the itemstack
   * @return The itemstack of the item
   */
  public static ItemStack getItemStack(ItemModel itemModel) {
    return getItemStack(itemModel, 1);
  }

  private static final Map<ItemModel, ItemStack> itemCache = new HashMap<>();

  /**
   * Get the itemstack of the item
   *
   * @param itemModel The item model to get the itemstack
   * @param amount    The amount of the item
   * @return The itemstack of the item
   */
  public static ItemStack getItemStack(ItemModel itemModel, int amount) {
    ItemStack itemStack = itemCache.get(itemModel);
    if (itemStack != null && itemStack.getCount() > 0) return itemStack.copyWithCount(amount == 0 ? 1 : amount);
    if (itemModel.getItem().startsWith("pokemon:")) {
      itemStack = PokemonItem.from(PokemonProperties.Companion.parse(itemModel.getItem().replace(
        "pokemon:", "")));
    } else if (itemModel.getItem().startsWith("command:")) {
      String command = itemModel.getItem().replace("command:", "");
      for (Map.Entry<String, ItemModel> entry : CobbleUtils.config.getItemsCommands().entrySet()) {
        if (command.startsWith(entry.getKey())) {
          itemStack = ItemUtils.parseItemId(entry.getValue().getItem(), amount, entry.getValue().getCustomModelData());
          break;
        }
      }
      if (itemStack == null) return ItemUtils.parseItemId("minecraft:command_block", amount);
    } else if (itemModel.getItem().startsWith("head:")) {
      itemStack = PlayerUtils.getHeadItem(itemModel.getItem().replace("head:", ""), amount);
    } else if (itemModel.getItem().startsWith("money:")) {
      itemStack = new ItemChance(itemModel.getItem(), 0).getIcon();
    } else {
      itemStack = ItemUtils.parseItemModel(itemModel, amount);
    }
    itemCache.put(itemModel, itemStack);
    return itemStack.copyWithCount(amount == 0 ? 1 : amount);
  }

  /**
   * Get the button of the item
   *
   * @param action The action of the button
   * @return The button of the item
   */
  @Deprecated(forRemoval = true)
  public GooeyButton getButton(Consumer<ButtonAction> action) {
    return getButton(1, null, null, action);
  }

  /**
   * Get the button of the item
   *
   * @param action The action of the button
   * @return The button of the item
   */
  @Deprecated(forRemoval = true)
  public GooeyButton getButton(int amount, Consumer<ButtonAction> action) {
    return getButton(amount, null, null, action);
  }

  /**
   * Get the button of the item
   *
   * @param action The action of the button
   * @param amount The amount of the item
   * @return The button of the item
   */
  @Deprecated(forRemoval = true)
  public GooeyButton getButton(int amount, String name, Consumer<ButtonAction> action) {
    return getButton(amount, name, null, action);
  }

  /**
   * Get the button of the item
   *
   * @param action The action of the button
   * @param amount The amount of the item
   * @return The button of the item
   */
  @Deprecated(forRemoval = true)
  public GooeyButton getButton(int amount, String name,
                               List<String> lore, Consumer<ButtonAction> action) {
    ItemStack itemStack = this.getItemStack(amount);
    GooeyButton.Builder builder = GooeyButton.builder()
      .display(itemStack);

    String resultName = name != null ? name : getDisplayname();
    builder.with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(resultName));

    List<String> resultLore = lore != null ? lore : getLore();
    LoreComponent loreComponent = new LoreComponent(AdventureTranslator.toNativeL(resultLore));
    builder.with(DataComponentTypes.LORE, loreComponent);

    if (tooltip != null) {
      if (tooltip instanceof Boolean && Boolean.TRUE.equals(tooltip)) {
        builder.with(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        builder.with(DataComponentTypes.HIDE_TOOLTIP, Unit.INSTANCE);
      } else if (tooltip instanceof String) {
        String tooltipValue = ((String) tooltip).toLowerCase();
        if ("all".equals(tooltipValue)) {
          builder.with(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
          builder.with(DataComponentTypes.HIDE_TOOLTIP, Unit.INSTANCE);
        } else if ("additional".equals(tooltipValue)) {
          builder.with(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        }
      }
    }

    if (action != null) builder.onClick(action);
    return builder
      .build();
  }

  public RateLimitedButton getButton(Consumer<ButtonAction> action,
                                     long delay, TimeUnit timeUnit, @Nullable Integer limit) {

    GooeyButton button = getButton(1, null, null, action);
    RateLimitedButton.Builder rateLimitedButtonBuilder = RateLimitedButton.builder()
      .button(button)
      .interval(delay, timeUnit);
    if (limit != null) rateLimitedButtonBuilder.limit(limit);
    return rateLimitedButtonBuilder.build();
  }

  public RateLimitedButton getButton(int amount, Consumer<ButtonAction> action,
                                     long delay, TimeUnit timeUnit, @Nullable Integer limit) {

    GooeyButton button = getButton(amount, null, null, action);
    RateLimitedButton.Builder rateLimitedButtonBuilder = RateLimitedButton.builder()
      .button(button)
      .interval(delay, timeUnit);
    if (limit != null) rateLimitedButtonBuilder.limit(limit);
    return rateLimitedButtonBuilder.build();
  }

  public RateLimitedButton getButton(int amount, String name, Consumer<ButtonAction> action,
                                     long delay, TimeUnit timeUnit, @Nullable Integer limit) {

    GooeyButton button = getButton(amount, name, null, action);
    RateLimitedButton.Builder rateLimitedButtonBuilder = RateLimitedButton.builder()
      .button(button)
      .interval(delay, timeUnit);
    if (limit != null) rateLimitedButtonBuilder.limit(limit);
    return rateLimitedButtonBuilder.build();
  }

  public RateLimitedButton getButton(int amount, String name, List<String> lore, Consumer<ButtonAction> action,
                                     long delay, TimeUnit timeUnit, @Nullable Integer limit) {

    GooeyButton button = getButton(amount, name, lore, action);
    RateLimitedButton.Builder rateLimitedButtonBuilder = RateLimitedButton.builder()
      .button(button)
      .interval(delay, timeUnit);
    if (limit != null) rateLimitedButtonBuilder.limit(limit);
    return rateLimitedButtonBuilder.build();
  }

  public LinkedPageButton getLinkedPageButton(LinkType linkType) {
    ItemStack itemStack = this.getItemStack();
    return LinkedPageButton.builder()
      .display(itemStack)
      .linkType(linkType)
      .build();
  }

  public void applyTemplate(ChestTemplate template, GooeyButton button) {
    int rows = template.getRows();
    if (UIUtils.isInside(slot, rows)) template.set(slot, button);
    if (slots != null) {
      GooeyButton invisibleButton = CobbleUtils.language.getItemInvisible().getButton(((GooeyButtonMixin) button).getOnClick());
      invisibleButton.getDisplay().set(DataComponentTypes.LORE, button.getDisplay().get(DataComponentTypes.LORE));
      invisibleButton.getDisplay().set(DataComponentTypes.CUSTOM_NAME, button.getDisplay().get(DataComponentTypes.CUSTOM_NAME));
      for (Integer s : slots) {
        if (UIUtils.isInside(s, rows)) template.set(s, invisibleButton);
      }
    }
  }

  public void applyTemplate(ChestTemplate template, RateLimitedButton button) {
    int rows = template.getRows();
    if (UIUtils.isInside(slot, rows)) template.set(slot, button);
    if (slots != null) {
      for (Integer slot : slots) {
        if (UIUtils.isInside(slot, rows)) template.set(slot, button);
      }
    }
  }

}
