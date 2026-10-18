package com.kingpixel.cobbleutils.Model;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.annotations.SerializedName;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.api.RewardsApi;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.util.*;
import lombok.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Represents an item chance model with methods to handle rewards.
 */
@Data
@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
public class ItemChance {
  public static final Map<String, List<ItemMod>> modItems = new HashMap<>();
  // Required properties
  @SerializedName(value = "item", alternate = { "reward" })
  private String item;
  private double chance;
  // Optional Unique reward ID
  @Builder.Default
  private Boolean unique = null;
  @Builder.Default
  private String identifier = null;
  @Builder.Default
  private Integer amount = null; // fixed amount, if null, amount is parsed from item string
  @Builder.Default
  private DurationValue cooldown = null;
  // Optional display properties
  private String display;
  private String displayname;

  public ItemChance() {
    this("minecraft:dirt", 100);
  }

  public ItemChance(String item, double chance) {
    this.item = item;
    this.chance = chance;
    this.display = null;
    this.displayname = null;
  }

  public ItemChance(String item, double chance, String display) {
    this.item = item;
    this.chance = chance;
    this.display = display;
    this.displayname = null;
  }

  public ItemChance(String item, double chance, String display, String displayname) {
    this.item = item;
    this.chance = chance;
    this.display = display;
    this.displayname = displayname;
  }

  @Deprecated
  public ItemChance(String item, int chance) {
    this.item = item;
    this.chance = chance;
    this.display = null;
    this.displayname = null;
  }

  @Deprecated
  public ItemChance(String item, int chance, String display) {
    this.item = item;
    this.chance = chance;
    this.display = display;
    this.displayname = null;
  }

  @Deprecated
  public ItemChance(String item, int chance, String display, String displayname) {
    this.item = item;
    this.chance = chance;
    this.display = display;
    this.displayname = displayname;
  }

  @Getter
  @Setter
  @ToString
  public static class ItemMod {
    private String itemId;
    private ItemStack itemStack;

    public ItemMod(String itemId, ItemStack itemStack) {
      this.itemId = itemId;
      this.itemStack = itemStack;
    }

    public static void openMenu(ServerPlayerEntity player) {
      ChestTemplate template = ChestTemplate.builder(6)
          .build();

      List<Button> buttons = new ArrayList<>();
      modItems.forEach((modid, items) -> {
        items.forEach(item -> {
          buttons.add(GooeyButton.builder()
              .display(item.getItemStack().copy())
              .with(DataComponentTypes.CUSTOM_NAME,
                  AdventureTranslator.toNative("mod:" + modid + ":" + item.getItemId()))
              .onClick((action) -> {
                action.getPlayer().giveItemStack(item.getItemStack().copy());
              })
              .build());
        });
      });

      template.rectangle(0, 0, 5, 9, new PlaceholderButton());
      template.fillFromList(buttons);
      ItemModel itemPrevious = CobbleUtils.language.getItemPrevious();
      template.set(45, LinkedPageButton.builder()
          .display(itemPrevious.getItemStack())
          .linkType(LinkType.Previous)
          .build());

      ItemModel itemClose = CobbleUtils.language.getItemClose();
      template.set(49, itemClose.getButton(action -> {
        UIManager.closeUI(action.getPlayer());
      }, 1, TimeUnit.SECONDS, 1));

      ItemModel itemNext = CobbleUtils.language.getItemNext();
      template.set(53, LinkedPageButton.builder()
          .display(itemNext.getItemStack())
          .linkType(LinkType.Next)
          .build());

      LinkedPage.Builder linkedPageBuilder = LinkedPage.builder()
          .title("Mod Rewards");

      UIManager.openUIForcefully(player,
          PaginationHelper.createPagesFromPlaceholders(template, buttons, linkedPageBuilder));
    }
  }

  public static void addModItem(String modid, String itemId, ItemStack itemStack) {
    if (!modItems.containsKey(modid)) {
      modItems.put(modid, new ArrayList<>());
    }
    ItemMod itemMod = new ItemMod(itemId, itemStack);
    boolean exists = modItems.get(modid).stream().anyMatch(i -> i.getItemId().equals(itemId));
    if (!exists) {
      modItems.get(modid).add(itemMod);
    }
  }

  /**
   * Determines the type of item chance.
   *
   * @return The type of item chance.
   */
  public ItemChanceType getType() {
    if (item.startsWith("pokemon:"))
      return ItemChanceType.POKEMON;
    if (item.startsWith("command:"))
      return ItemChanceType.COMMAND;
    if (item.startsWith("money:"))
      return ItemChanceType.MONEY;
    if (item.startsWith("mod:"))
      return ItemChanceType.MOD;
    return ItemChanceType.ITEM;
  }

  public enum ItemChanceType {
    POKEMON, COMMAND, MONEY, ITEM, MOD
  }

  /**
   * Gets default item chances.
   *
   * @return The default item chances.
   */
  public static List<ItemChance> defaultItemChances() {
    List<ItemChance> itemChances = new ArrayList<>();
    itemChances.add(new ItemChance("id:battle", 1.0));
    itemChances.add(new ItemChance("cobblemon:poke_ball", 999.0));
    itemChances.add(new ItemChance("item:1:cobblemon:poke_ball", 1.0));
    itemChances.add(new ItemChance("item:1:minecraft:dirt#[minecraft:custom_model_data=1]", 1.0));
    itemChances.add(new ItemChance("pokemon:rattata alola", 1.0));
    itemChances.add(new ItemChance("command:lp user %player% permission set a", 1.0, "minecraft:emerald", "Give " +
        "permission a"));
    itemChances.add(new ItemChance("command:lp user %player% permission set a#lp user %player% permission set b", 1.0,
        "minecraft:emerald", "Give permission a and b"));
    itemChances.add(new ItemChance("money:1", 1.0));
    itemChances.add(new ItemChance("money:tokens:1", 1.0));
    itemChances.add(new ItemChance("mod:cobblehunt:radar", 1.0));
    itemChances.add(new ItemChance("cobblemon:poke_ball|cobblemon:great_ball|cobblemon:ultra_ball|command:lp user " +
        "%player% permission set a|pokemon:rattata alola", 1.0));
    return itemChances;
  }

  /**
   * Gets the ItemStack of the item with a specific amount.
   *
   * @param amount The amount of the item.
   * @return The ItemStack of the item.
   */
  public ItemStack getItemStack(int amount) {
    return getRewardItemStack(item, amount);
  }

  public ItemStack getItemStack() {
    return getItemStack(1);
  }

  public static boolean giveReward(ServerPlayerEntity player, ItemChance itemChance) throws NoPokemonStoreException {
    return giveReward(player, itemChance, 1);
  }

  public boolean giveReward(ServerPlayerEntity player) {
    return giveReward(player, this, 1);
  }

  private static final Pattern COMMAND_SPLIT_PATTERN = Pattern.compile("(?<!<)#");
  private static final String PREFIX_COMMAND = "command:";
  private static final String INVALID_NUMBER_FORMAT = "Invalid number format: ";
  private static final String BANNER_DELIMITER = "\n================================================================================";

  // Cache for parsed items to improve performance
  private static final Map<ItemChance, ItemStack> ITEM_CACHE = new ConcurrentHashMap<>();

  // Method to get cached item or parse and cache it if not present
  private static ItemStack getCachedItem(ItemChance itemChance, int amount) {
    return ITEM_CACHE.computeIfAbsent(itemChance, key -> {
      try {
        String item = key.getItem();

        if (item.startsWith("item:")) {
          return parseItemStack(item, 1);
        } else if (item.startsWith("mod:")) {
          return getModItem(key);
        } else {
          return ItemUtils.parseItemId(item, 1);
        }
      } catch (Exception e) {
        CobbleUtils.LOGGER_RAW.error("Error caching item: " + itemChance.getItem());
        return ItemStack.EMPTY;
      }
    }).copyWithCount(amount);
  }

  /**
   * Gives a reward to the player based on the item type and amount.
   *
   * @param player The player to give the reward to.
   * @param amount The amount of the item to give.
   * @return True if the reward was given successfully, false otherwise.
   */
  public static boolean giveReward(ServerPlayerEntity player, ItemChance itemChance, int amount) {
    try {
      String item = itemChance.getItem();
      if (item.startsWith("id:")) {
        String id = item.replace("id:", "");
        ItemChance ic = RewardsApi.getReward(id);
        if (ic != null) {
          return giveReward(player, ic, amount);
        } else
          return false;
      }
      String[] parts = item.split("\\|");

      if (parts.length > 1) {
        for (String part : parts) {
          giveReward(player, new ItemChance(part, itemChance.getChance()), amount);
        }
        return true;
      }

      ItemStack itemStack = null;

      if (item.startsWith("pokemon:")) {
        Pokemon pokemon = getRewardPokemon(item);
        return Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemon);
      } else if (item.startsWith("command:")) {
        String[] commandParts = COMMAND_SPLIT_PATTERN.split(item);
        for (String commandPart : commandParts) {
          String command = commandPart.replace(PREFIX_COMMAND, "").trim();

          if (command.startsWith("base64:")) {
            String base64Command = command.replace("base64:", "");
            try {
              command = new String(java.util.Base64.getDecoder().decode(base64Command));
            } catch (IllegalArgumentException e) {
              CobbleUtils.LOGGER_RAW.error("Failed to decode Base64 command: " + base64Command);
              e.printStackTrace();
              continue;
            }
          } else {
            command = command.replace("'", "\"");
          }

          if (!commandPart.isEmpty()) {
            CobbleUtilities.executeCommand(player, command);
          }
        }
        return true;
      } else if (item.startsWith("money:")) {
        return handleMoneyReward(player, item);
      } else if (item.startsWith("message:")) {
        String message = item.replace("message:", "");
        PlayerUtils.sendMessage(
            player,
            message,
            CobbleUtils.config.getPrefix(),
            TypeMessage.CHAT);
        return true;
      } else {
        itemStack = getCachedItem(itemChance, parseAmount(item.split(":")[1]) * amount).copy();
      }
      if (itemStack == null || itemStack.isEmpty()) {
        CobbleUtils.LOGGER_RAW.error("Error giving reward, item not found: " + item);
        return false;
      }
      ItemStack finalItemStack = itemStack;
      CobbleUtils.server.executeSync(() -> player.getInventory().offerOrDrop(finalItemStack));
      return true;
    } catch (Exception e) {
      String itemStr = itemChance != null ? itemChance.getItem() : "null";
      CobbleUtils.LOGGER_RAW.error("Error giving reward for ItemChance '{}': {}\n{}",
          itemStr, e.getMessage(), e);
      return false;
    }
  }

  private static Pokemon getRewardPokemon(String item) {
    String p = item.replace("pokemon:", "");
    if (p.isEmpty()) {
      return PokemonProperties.Companion.parse("rattata").create();
    } else {
      return PokemonProperties.Companion.parse(p).create();
    }
  }

  private static boolean handleMoneyReward(ServerPlayerEntity player, String item) {
    ParsedMoney parsed;
    try {
      parsed = parseMoney(item);
    } catch (NumberFormatException e) {
      CobbleUtils.LOGGER_RAW.error(getInvalidRewardExceptionMessage(item, INVALID_NUMBER_FORMAT + e.getMessage()));
      return false;
    }

    try {
      PlayerUtils.sendMessage(player,
          CobbleUtils.language.getMessageReceiveMoney()
              .replace("%amount%", EconomyApi.formatMoney(parsed)),
          "", TypeMessage.CHAT);

      return EconomyApi.addMoney(player, parsed);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error(getInvalidRewardExceptionMessage(item, e.getMessage()), e);
      return false;
    }
  }

  private static ItemStack parseItemStack(String item, int amount) {
    ItemStack itemStack;
    String[] split = item.split("#", 2);
    String[] itemSplit = split[0].split(":");
    String iditem = itemSplit[2] + ":" + itemSplit[3];
    itemStack = ItemUtils.parseItemId(iditem, amount);
    if (split.length < 2)
      return itemStack;
    String nbt = split[1];
    itemStack = ItemUtils.applyNbt(iditem, itemStack, nbt, amount);
    return itemStack;
  }

  /**
   * Gets the title of the item based on its type.
   *
   * @return The title of the item.
   */
  public String getTitle() {
    return getTitle(this);
  }

  private static ItemStack getModItem(ItemChance itemChance) {
    String[] parts = itemChance.getItem().split(":");
    String modid = parts[1];
    String itemId = parts[2];
    int amount = 1;
    if (parts.length > 3) {
      amount = Integer.parseInt(parts[3]);
    }
    ItemStack itemStack = modItems.getOrDefault(modid, new ArrayList<>())
        .stream()
        .filter(i -> i.getItemId().equalsIgnoreCase(itemId))
        .findFirst()
        .orElse(new ItemMod("", Items.DIRT.getDefaultStack())).getItemStack().copy();
    itemStack.setCount(amount);
    return itemStack;
  }

  /**
   * Gets the lore of the item based on its type.
   *
   * @return The lore of the item.
   */
  public List<String> getLore() {
    return getLore(this);
  }

  /**
   * Constructs a GooeyButton instance representing the item button.
   *
   * @param percentage The percentage to display in the lore.
   * @return The constructed GooeyButton instance.
   */
  public GooeyButton getButton(String percentage) {
    String title = getTitle();
    List<String> lore = new ArrayList<>(getLore());
    lore.replaceAll(s -> s.replace("%chance%", percentage));
    return GooeyButton.builder()
        .display(getItemStack())
        .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(title))
        .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)))
        .build();
  }

  private static final ConcurrentHashMap<String, ItemStack> REWARD_ITEM_STACK = new ConcurrentHashMap<>();

  /**
   * Gets the ItemStack of the reward based on its type.
   *
   * @param item   The item to get the ItemStack of.
   * @param amount The amount of the item.
   * @return The ItemStack of the reward.
   */
  private static ItemStack getRewardItemStack(String item, int amount) {
    if (item == null || item.isEmpty())
      return ItemStack.EMPTY;

    String cacheKey = item + "|" + amount;

    // Check if already cached
    ItemStack cached = REWARD_ITEM_STACK.get(cacheKey);
    if (cached != null)
      return cached.copy();

    // Resolve the ItemStack
    ItemStack resolved = resolveRewardItemStack(item, amount);

    // Cache the resolved ItemStack
    REWARD_ITEM_STACK.put(cacheKey, resolved);

    return resolved.copy(); // Return a copy to avoid modifying the cached instance
  }

  /**
   * Resolves the ItemStack without touching the cache (safe for recursion).
   */
  private static ItemStack resolveRewardItemStack(String item, int amount) {
    ItemStack itemStack;

    if (item.startsWith("id:")) {
      String id = item.replace("id:", "");
      ItemChance ic = RewardsApi.getReward(id);

      if (ic != null) {
        String nestedItem = ic.getItem();
        if (nestedItem.equals(item))
          return ItemStack.EMPTY; // Prevent infinite cycles
        return getRewardItemStack(nestedItem, amount);
      }
    }

    String[] parts = item.split("\\|");
    if (parts.length > 1) {
      return getRewardItemStack(parts[0], amount);
    }

    if (item.startsWith("pokemon:")) {
      itemStack = PokemonItem.from(getRewardPokemon(item));
    } else if (item.startsWith("command:")) {
      itemStack = parseCommandItem(item);
    } else if (item.startsWith("money:")) {
      itemStack = parseMoneyItem(item);
    } else if (item.startsWith("item:")) {
      itemStack = parseItemStack(item, parseAmount(item.split(":")[1]) * amount);
    } else if (item.startsWith("mod:")) {
      itemStack = getModItem(new ItemChance(item, 100));
    } else {
      itemStack = ItemUtils.parseItemId(item, amount);
    }

    return itemStack;
  }

  private static ItemStack parseCommandItem(String item) {
    String command = item.replace("command:", "");
    for (Map.Entry<String, ItemModel> entry : CobbleUtils.config.getItemsCommands().entrySet()) {
      if (command.startsWith(entry.getKey())) {
        return entry.getValue().getItemStack();
      }
    }
    return ItemUtils.parseItemId("minecraft:command_block", 1);
  }

  private static ItemStack parseMoneyItem(String item) {
    ParsedMoney parsed;
    try {
      parsed = parseMoney(item);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(getInvalidRewardExceptionMessage(item, INVALID_NUMBER_FORMAT + e.getMessage()),
          e);
    }

    ItemModel impactorItem = new ItemModel(CobbleUtils.language.getItemsEconomy().getOrDefault(parsed.getCurrency(),
        new ItemModel(CobbleUtils.language.getItemMoney())));
    try {
      impactorItem.setDisplayname(impactorItem.getDisplayname().replace("%amount%", EconomyApi.formatMoney(parsed)));
    } catch (Exception e) {
      throw new IllegalArgumentException(getInvalidRewardExceptionMessage(item, e.getMessage()), e);
    }

    return impactorItem.getItemStack();
  }

  private static int parseAmount(String amountStr) {
    try {
      if (amountStr.contains("-")) {
        String[] parts = amountStr.split("-");
        int min = Integer.parseInt(parts[0]);
        int max = Integer.parseInt(parts[1]);
        return Utils.getRandom().nextInt(max - min + 1) + min;
      }
      return Integer.parseInt(amountStr);
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  public static String getTitle(ItemChance itemChance) {
    if (itemChance.getDisplayname() != null && !itemChance.getDisplayname().isEmpty())
      return itemChance.getDisplayname();

    String item = itemChance.getItem();
    String[] parts = item.split("\\|");
    if (parts.length > 1) {
      return getTitle(new ItemChance(parts[0], itemChance.getChance()));
    }
    if (item.startsWith("pokemon:")) {
      return getPokemonTitle(item);
    } else if (item.startsWith("command:")) {
      return getCommandTitle(item);
    } else if (item.startsWith("money:")) {
      return getMoneyTitle(item);
    } else if (item.startsWith("item:")) {
      return getItemTitle(item);
    } else if (item.startsWith("mod:")) {
      return ItemUtils.getTranslatedName(getModItem(itemChance));
    } else {
      return ItemUtils.getTranslatedName(ItemUtils.parseItemId(item));
    }
  }

  private static String getPokemonTitle(String item) {
    Pokemon pokemon = getRewardPokemon(item);
    return PokemonUtils.replace(CobbleUtils.language.getPokemonnameformat(), pokemon);
  }

  private static String getCommandTitle(String item) {
    String command = item.replace("command:", "");
    for (Map.Entry<String, ItemModel> entry : CobbleUtils.config.getItemsCommands().entrySet()) {
      if (command.startsWith(entry.getKey())) {
        return entry.getValue().getDisplayname();
      }
    }
    return command;
  }

  private static BigDecimal parseAmountMoney(String part) {
    if (part.contains("-")) {
      String[] parts = part.split("-");
      int min = Integer.parseInt(parts[0]);
      int max = Integer.parseInt(parts[1]);
      return BigDecimal.valueOf(Utils.getRandom().nextInt(max - min + 1) + min);
    }
    return new BigDecimal(part);
  }

  private static String getMoneyTitle(String item) {
    ParsedMoney parsed;
    try {
      parsed = parseMoney(item);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(getInvalidRewardExceptionMessage(item, INVALID_NUMBER_FORMAT + e.getMessage()),
          e);
    }

    ItemModel itemModel = CobbleUtils.language.getItemsEconomy().getOrDefault(parsed.getCurrency(),
        new ItemModel(CobbleUtils.language.getItemMoney()));
    String format;
    try {
      if (parsed.getAmountString().contains("-")) {
        String[] split = parsed.getAmountString().split("-");
        var min = BigDecimal.valueOf(Integer.parseInt(split[0]));
        var max = BigDecimal.valueOf(Integer.parseInt(split[1]));
        format = EconomyApi.formatMoney(min, parsed) + " &f- " +
            EconomyApi.formatMoney(max, parsed);
      } else {
        format = EconomyApi.formatMoney(parsed);
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(getInvalidRewardExceptionMessage(item, e.getMessage()), e);
    }
    return itemModel.getDisplayname().replace("%amount%", format);
  }

  private static String getItemTitle(String item) {
    ItemStack itemStack = parseItemStack(item, 1);
    return ItemUtils.getTranslatedName(itemStack);
  }

  private static List<String> getLore(ItemChance itemChance) {
    return CobbleUtils.language.getLorechance();
  }

  /**
   * Gets a random reward from a list of item chances and gives it to the player.
   *
   * @param itemChances The list of item chances to choose from.
   * @param player      The player to give the reward to.
   * @throws IllegalArgumentException If the list of item chances is empty.
   */
  @Deprecated
  public static void getRandomReward(List<ItemChance> itemChances, ServerPlayerEntity player) {
    if (itemChances == null || itemChances.isEmpty()) {
      throw new IllegalArgumentException("The list of item chances cannot be null or empty");
    }

    // ✅ Calculate total chance using a simple loop
    double totalChance = 0.0;
    for (ItemChance itemChance : itemChances) {
      totalChance += itemChance.getChance();
    }

    // ✅ Pick a random point within the total chance
    double randomPoint = Utils.getRandom().nextDouble(totalChance);
    double cumulativeChance = 0.0;

    // ✅ Iterate to find which reward corresponds to that random point
    for (ItemChance itemChance : itemChances) {
      cumulativeChance += itemChance.getChance();
      if (randomPoint < cumulativeChance) {
        try {
          giveReward(player, itemChance);
        } catch (NoPokemonStoreException e) {
          e.printStackTrace();
        }
        return; // Exit immediately after giving one reward
      }
    }
  }

  /**
   * Get a random reward from a list of item chances and give it to the player.
   *
   * @param itemChances     The list of item chances to choose from.
   * @param player          The player to give the reward to.
   * @param numberOfRewards The number of rewards to give.
   * @throws IllegalArgumentException If the list of item chances is empty or the
   *                                  number of rewards is less than or equal to
   *                                  zero.
   */
  @Deprecated
  public static void getRandomRewards(List<ItemChance> itemChances, ServerPlayerEntity player, int numberOfRewards) {
    if (itemChances == null || itemChances.isEmpty()) {
      throw new IllegalArgumentException("The list of item chances cannot be empty");
    }
    if (numberOfRewards <= 0) {
      throw new IllegalArgumentException("The number of rewards must be greater than zero");
    }

    for (int i = 0; i < numberOfRewards; i++) {
      getRandomReward(itemChances, player);
    }
  }

  /**
   * Gets a list of rewards from a list of item chances and gives them to the
   * player.
   *
   * @param itemChances     The list of item chances to choose from.
   * @param player          The player to give the rewards to.
   * @param numberOfRewards The number of rewards to give.
   * @return The list of rewards given to the player.
   */
  public static List<ItemChance> getRewards(List<ItemChance> itemChances, ServerPlayerEntity player,
      int numberOfRewards) {
    if (itemChances == null || itemChances.isEmpty()) {
      PlayerUtils.sendMessage(
          player,
          "No rewards available",
          CobbleUtils.config.getPrefix(),
          TypeMessage.CHAT);
      return Collections.emptyList();
    }

    // Filter valid rewards — unique items that are exhausted/on cooldown are
    // excluded
    List<ItemChance> validRewards = new ArrayList<>();
    for (ItemChance itemChance : itemChances) {
      Boolean unique = itemChance.getUnique();

      if (unique == null || !unique) {
        validRewards.add(itemChance);
        continue;
      }
      if (DataBaseFactory.dataBaseUsers.isAvailableReward(player, itemChance)) {
        validRewards.add(itemChance);
      }
    }

    if (validRewards.isEmpty()) {
      PlayerUtils.sendMessage(
          player,
          "No rewards available",
          CobbleUtils.config.getPrefix(),
          TypeMessage.CHAT);
      return Collections.emptyList();
    }

    // Separate guaranteed and normal rewards
    List<ItemChance> guaranteedRewards = new ArrayList<>();
    List<ItemChance> normalRewards = new ArrayList<>();

    for (ItemChance itemChance : validRewards) {
      if (itemChance.getChance() < 0) {
        guaranteedRewards.add(itemChance);
      } else {
        normalRewards.add(itemChance);
      }
    }

    List<ItemChance> rewards = new ArrayList<>();

    // Track unique identifiers already selected in THIS roll to avoid duplicates
    Set<String> selectedUniqueIds = new HashSet<>();

    // Calculate totalChance only for normal rewards
    double totalChance = 0.0;
    for (ItemChance itemChance : normalRewards) {
      totalChance += itemChance.getChance();
    }

    // Randomly select rewards, removing unique items from pool after selection
    for (int i = 0; i < numberOfRewards; i++) {
      if (normalRewards.isEmpty() || totalChance <= 0)
        break;
      double randomPoint = Utils.getRandom().nextDouble(totalChance);
      double cumulativeChance = 0.0;

      for (int j = 0; j < normalRewards.size(); j++) {
        ItemChance itemChance = normalRewards.get(j);
        cumulativeChance += itemChance.getChance();
        if (randomPoint < cumulativeChance) {
          Boolean unique = itemChance.getUnique();
          String identifier = itemChance.getIdentifier();
          if (unique != null && unique && identifier != null) {
            if (selectedUniqueIds.contains(identifier)) {
              // Already selected this unique — re-roll
              break;
            }
            selectedUniqueIds.add(identifier);
            rewards.add(itemChance);
            normalRewards.remove(j);
            totalChance -= itemChance.getChance();
            if (totalChance < 0)
              totalChance = 0;
          } else {
            rewards.add(itemChance);
          }
          break;
        }
      }
    }
    rewards.addAll(guaranteedRewards);
    return rewards;
  }

  /**
   * Gives all rewards from a list of item chances to the player.
   *
   * @param itemChances The list of item chances to choose from.
   * @param player      The player to give the rewards to.
   */
  public static List<ItemChance> getAllRewards(List<ItemChance> itemChances, ServerPlayerEntity player) {
    List<ItemChance> finalItemChances = new ArrayList<>();
    for (ItemChance itemChance : itemChances) {
      Boolean unique = itemChance.getUnique();
      // Non-unique items are always available — skip DB check entirely
      if (unique == null || !unique) {
        finalItemChances.add(itemChance);
        continue;
      }
      // Only check DB for unique items
      if (DataBaseFactory.dataBaseUsers.isAvailableReward(player, itemChance)) {
        finalItemChances.add(itemChance);
      }
    }
    return finalItemChances;
  }

  /**
   * Gets a list of GooeyButton instances representing the item chances.
   *
   * @param itemChances The list of item chances to get the buttons for.
   * @return The list of GooeyButton instances representing the item chances.
   */
  public static List<GooeyButton> getButtons(List<ItemChance> itemChances) {
    List<GooeyButton> buttons = new ArrayList<>();
    for (ItemChance itemChance : itemChances) {
      List<String> lore = new ArrayList<>(itemChance.getLore());
      lore.replaceAll(s -> s.replace("%chance%", String.valueOf(itemChance.getChance())));
      buttons.add(GooeyButton.builder()
          .display(itemChance.getItemStack())
          .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)))
          .build());
    }
    return buttons;
  }

  public ItemStack getIcon() {
    return getIcon(null);
  }

  public ItemStack getIcon(String title) {
    return getIcon(title, null);
  }

  public ItemStack getIcon(String title, List<String> lore) {
    if (title == null)
      title = this.getTitle();
    if (lore == null)
      lore = this.getLore();
    ItemStack itemStack;
    if (this.getDisplay() != null && !this.getDisplay().isEmpty()) {
      itemStack = getRewardItemStack(this.getDisplay(), 1);
    } else {
      itemStack = getItemStack();
    }

    if (title != null && !title.isEmpty()) {
      itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(title));
    }

    return itemStack;
  }


  /**
   * Splits a string by delimiter while ignoring delimiters enclosed in single (') or double (\") quotes.
   * Strips outer quotes from the resulting tokens.
   */
  public static List<String> splitRespectingQuotes(String input, char delimiter) {
    List<String> tokens = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;

    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (c == '\'' && !inDoubleQuote) {
        inSingleQuote = !inSingleQuote;
      } else if (c == '"' && !inSingleQuote) {
        inDoubleQuote = !inDoubleQuote;
      } else if (c == delimiter && !inSingleQuote && !inDoubleQuote) {
        tokens.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    tokens.add(current.toString().trim());
    return tokens;
  }

  /**
   * Parses a raw money reward string into its constituent parts: economyId, currency, and amount.
   * Supports quoted currencies (e.g. money:'mania:dollars':20) and unquoted multi-colon namespaced currencies.
   *
   * @param item The money reward string.
   * @return The parsed money reward.
   */
  public static ParsedMoney parseMoney(String item) {
    List<String> parts = splitRespectingQuotes(item, ':');
    String economyId = "";
    String currency = "money";
    String amountString;

    switch (parts.size()) {
      case 2 -> amountString = parts.get(1);
      case 3 -> {
        currency = parts.get(1);
        amountString = parts.get(2);
      }
      case 4 -> {
        String candidateEconomy = parts.get(1);
        if (EconomyApi.isEconomy(candidateEconomy)) {
          economyId = candidateEconomy;
          currency = parts.get(2);
          amountString = parts.get(3);
        } else {
          currency = candidateEconomy + ":" + parts.get(2);
          amountString = parts.get(3);
        }
      }
      case 5 -> {
        economyId = parts.get(1);
        currency = parts.get(2) + ":" + parts.get(3);
        amountString = parts.get(4);
      }
      default -> throw new IllegalArgumentException(
          getInvalidRewardExceptionMessage(item, "Invalid number of arguments in money reward"));
    }

    BigDecimal amount = parseAmountMoney(amountString);
    return new ParsedMoney(economyId, currency, amountString, amount);
  }

  /**
   * Generates a descriptive error message when an ItemChance reward format or currency is invalid.
   * Includes the exact configured reward item to allow quick searching across config files,
   * along with categorized valid examples of reward formats.
   *
   * @param item   The raw reward string configured in ItemChance.
   * @param reason The root cause or error details.
   * @return The formatted error message.
   */
  public static String getInvalidRewardExceptionMessage(String item, String reason) {
    StringBuilder builder = new StringBuilder();
    builder.append(BANNER_DELIMITER)
        .append("\n [CobbleUtils] INVALID REWARD DETECTED IN CONFIGURATION")
        .append(BANNER_DELIMITER)
        .append("\n • Offending reward string : '").append(item).append("'");
    if (reason != null && !reason.isBlank()) {
      builder.append("\n • Cause / Details         : ").append(reason);
    }
    builder.append("\n • Quick Fix Action        : Search your server configs for '").append(item).append("' and correct it.")
        .append("\n--------------------------------------------------------------------------------")
        .append("\n SUPPORTED FORMATS & EXAMPLES:")
        .append("\n   [Money]")
        .append("\n     - Default currency        : money:<amount>                      (e.g. 'money:100' or 'money:10-50')")
        .append("\n     - Specific currency       : money:<currency>:<amount>           (e.g. 'money:pokedollars:100')")
        .append("\n     - Namespaced currency     : money:'<namespace:curr>':<amount>   (e.g. money:'mania:dollars':20 or money:mania:dollars:20)")
        .append("\n     - Specific economy & curr : money:<economy>:<currency>:<amount> (e.g. money:impactor:pokedollars:100 or money:impactor:'mania:dollars':20)")
        .append("\n   [Items]")
        .append("\n     - Simple item             : <modid>:<item>                      (e.g. 'cobblemon:poke_ball')")
        .append("\n     - Item with amount        : item:<amount>:<modid>:<item>        (e.g. 'item:16:minecraft:diamond')")
        .append("\n     - Item with NBT/Components: item:<amount>:<modid>:<item>#[nbt]  (e.g. 'item:1:minecraft:dirt#[minecraft:custom_model_data=1]')")
        .append("\n   [Pokémon]")
        .append("\n     - Cobblemon properties    : pokemon:<properties>                (e.g. 'pokemon:rattata alola' or 'pokemon:pikachu shiny=yes level=50')")
        .append("\n   [Commands]")
        .append("\n     - Server command          : command:<cmd>                       (e.g. 'command:lp user %player% permission set vip')")
        .append("\n     - Multiple commands       : command:<cmd1>#<cmd2>               (e.g. 'command:heal %player%#feed %player%')")
        .append("\n   [Mod Items]")
        .append("\n     - Registered mod item     : mod:<modid>:<itemid>[:amount]       (e.g. 'mod:cobblehunt:radar' or 'mod:cobblehunt:radar:2')")
        .append("\n   [Custom / Nested]")
        .append("\n     - Referenced reward ID    : id:<reward_id>                      (e.g. 'id:battle')")
        .append("\n     - Multi-option reward     : <reward1>|<reward2>|...             (e.g. 'cobblemon:poke_ball|cobblemon:ultra_ball')")
        .append(BANNER_DELIMITER);
    return builder.toString();
  }
}
