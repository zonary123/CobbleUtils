package com.kingpixel.cobbleutils.model.rewards;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.RewardsAPI;
import com.kingpixel.cobbleutils.model.ItemModel;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.ItemUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.economys.EconomySelector;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RewardRegistry {
  private static final Map<String, RewardExecutor> EXECUTORS = new ConcurrentHashMap<>();
  private static final Map<String, IconProvider> ICON_PROVIDERS = new ConcurrentHashMap<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class MoneyRewardData {
    private double minAmount;
    private double maxAmount;
    private double finalAmount;
    private String economy;
    private String currency;
    private String reason;
    private EconomySelector economySelector;

    public String format(double amount) {
      String format = economySelector.format(BigDecimal.valueOf(amount));
      if (format == null || format.isEmpty()) return String.format("%.2f", amount);
      return format;
    }
  }

  private static MoneyRewardData parseMoneyRewardData(String data) {

    if (data == null || data.isBlank()) {
      throw new IllegalArgumentException("Money reward data is null or empty");
    }

    if (data.toLowerCase().startsWith("money:")) {
      data = data.substring("money:".length());
    }

    String[] parts = data.split(":");

    String economy = "";
    String currency = "";
    String reason = null;
    String amountPart;

    if (parts.length == 1) {

      amountPart = parts[0];

    } else if (parts.length == 2) {

      if (isAmount(parts[0])) {
        amountPart = parts[0];
      } else {
        currency = parts[0];
        amountPart = parts[1];
      }

    } else {

      if (isAmount(parts[0])) {

        // money:<amount>:<economy>:<currency>:<reason>
        amountPart = parts[0];
        economy = parts[1];
        currency = parts[2];

        if (parts.length > 3) {
          reason = String.join(":", Arrays.copyOfRange(parts, 3, parts.length));
        }

      } else {

        // money:<currency>:<amount>:<economy>:<reason>
        currency = parts[0];
        amountPart = parts[1];

        if (amountPart == null) {
          throw new IllegalArgumentException("Invalid money format: " + data);
        }

        economy = parts.length > 2 ? parts[2] : "";

        if (parts.length > 3) {
          reason = String.join(":", Arrays.copyOfRange(parts, 3, parts.length));
        }
      }
    }

    if (amountPart == null || amountPart.isBlank()) {
      throw new IllegalArgumentException("Money amount is missing in: " + data);
    }

    double min;
    double max;

    try {

      if (isRange(amountPart)) {

        String[] range = amountPart.split("-", 2);

        min = Double.parseDouble(range[0]);
        max = Double.parseDouble(range[1]);

        if (min > max) {
          throw new IllegalArgumentException("Min cannot be greater than max in range: " + amountPart);
        }

      } else {

        if (!isNumeric(amountPart)) {
          throw new IllegalArgumentException("Invalid numeric amount: " + amountPart);
        }

        min = max = Double.parseDouble(amountPart);
      }

    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Invalid money value: " + amountPart, ex);
    }

    double finalAmount = min == max
      ? min
      : ThreadLocalRandom.current().nextDouble(min, max);

    if (reason == null || reason.isBlank()) {
      reason = "Money Reward: " + String.format("%.2f", finalAmount);
    }

    return MoneyRewardData.builder()
      .minAmount(min)
      .maxAmount(max)
      .finalAmount(finalAmount)
      .economy(economy)
      .currency(currency)
      .reason(reason)
      .economySelector(new EconomySelector(economy, currency))
      .build();
  }

  private static boolean isNumeric(String value) {
    if (value == null || value.isBlank()) return false;
    try {
      Double.parseDouble(value);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static boolean isRange(String value) {
    return value.matches("-?\\d+(\\.\\d+)?-\\-?\\d+(\\.\\d+)?");
  }

  private static boolean isAmount(String value) {
    return isNumeric(value) || isRange(value);
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class ItemStackRewardData {
    private int minAmount;
    private int maxAmount;
    private int finalAmount;
    private ItemStack itemStack;

  }

  private static ItemStackRewardData parseItemStackRewardData(String data) {
    if (data == null || data.isEmpty()) return null;

    String[] nbtSplit = data.split("#", 2);
    String base = nbtSplit[0];
    String nbt = nbtSplit.length == 2 && !nbtSplit[1].isEmpty() ? nbtSplit[1] : null;

    String[] parts = base.split(":", 2);
    if (parts.length != 2) return null;

    String amountPart = parts[0];
    String itemId = parts[1];

    int min;
    int max;
    int finalAmount;

    if (amountPart.indexOf('-') > 0) {
      String[] range = amountPart.split("-", 2);
      min = Integer.parseInt(range[0]);
      max = Integer.parseInt(range[1]);
      finalAmount = ThreadLocalRandom.current().nextInt(min, max + 1);
    } else {
      min = max = finalAmount = Integer.parseInt(amountPart);
    }

    ItemStack itemStack;
    if (nbt != null) {
      itemStack = ItemUtils.applyNbt(
        itemId,
        ItemUtils.parseItemId(itemId, finalAmount),
        nbt,
        finalAmount
      );
    } else {
      itemStack = ItemUtils.parseItemId(itemId, finalAmount);
    }

    return ItemStackRewardData.builder()
      .minAmount(min)
      .maxAmount(max)
      .finalAmount(finalAmount)
      .itemStack(itemStack)
      .build();
  }


  static {
    EXECUTORS.put("id", (player, reward, data) -> RewardsAPI.giveReward(player.getUuid(), reward));
    EXECUTORS.put("mod", (player, reward, data) -> CompletableFuture.completedFuture(false));
    EXECUTORS.put("item", (player, reward, data) -> {
      ItemStackRewardData itemStackRewardData = parseItemStackRewardData(data);
      ItemStack itemStack = itemStackRewardData.getItemStack();
      return CobbleUtils.server.submit(() -> {
        if (itemStack == null) {
          player.sendMessage(Text.literal("Invalid item reward data: " + data));
          return false;
        }
        boolean given = false;
        if (player.getInventory().getEmptySlot() != -1) {
          if (CobbleUtils.config.isNotifyRewards()) {
            PlayerUtils.sendMessage(
              player,
              CobbleUtils.language.getMessageRewardItemStack()
                .replace("%item%", ItemUtils.getTranslatedName(itemStack))
                .replace("%amount%", itemStack.getCount() + ""),
              CobbleUtils.config.getPrefix(),
              TypeMessage.CHAT
            );
          }
          player.getInventory().offerOrDrop(itemStack);
          given = true;
        }

        return given;
      });
    });

    EXECUTORS.put("command", (player, reward, data) -> PlayerUtils.executeCommandCompletable(data, player));
    EXECUTORS.put("money", (player, reward, data) -> {
      try {
        MoneyRewardData moneyData = parseMoneyRewardData(data);
        if (moneyData == null) {
          player.sendMessage(Text.literal("Invalid money reward data: " + data));
          return CompletableFuture.completedFuture(false);
        }
        double finalAmount = moneyData.getFinalAmount();
        return moneyData.getEconomySelector()
          .deposit(player.getUuid(), BigDecimal.valueOf(finalAmount), moneyData.getReason().replace("%money%", finalAmount + ""))
          .thenCompose(economyResult -> CompletableFuture.completedFuture(economyResult.isSuccess()));
      } catch (Exception e) {
        e.printStackTrace();
      }
      return CompletableFuture.completedFuture(false);

    });

    EXECUTORS.put("pokemon", (player, reward, data) -> CobbleUtils.server.submit(() -> {
      Pokemon pokemon = PokemonProperties.Companion.parse(data).create();
      var party = Cobblemon.INSTANCE.getStorage().getParty(player);
      return party.add(pokemon);
    }));

    EXECUTORS.put("message", (player, reward, data) -> {
      PlayerUtils.sendMessage(player, data, CobbleUtils.config.getPrefix(), TypeMessage.CHAT);
      return CompletableFuture.completedFuture(true);
    });
    // ----------------- ICON_PROVIDERS -----------------
    ICON_PROVIDERS.put("id", (data) -> Items.NETHER_STAR.getDefaultStack());
    ICON_PROVIDERS.put("mod", (data) -> Items.BARRIER.getDefaultStack());
    ICON_PROVIDERS.put("item", (data) -> {
      ItemStackRewardData itemStackRewardData = parseItemStackRewardData(data);
      ItemStack itemStack = itemStackRewardData.getItemStack();
      int min = itemStackRewardData.getMinAmount();
      int max = itemStackRewardData.getMaxAmount();
      String translatedItem = ItemUtils.getTranslatedName(itemStack);
      if (min != max) {
        itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(
          translatedItem + " x" + min + " - x" + max
        ));
      } else {
        itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(
          translatedItem + " x" + min
        ));
      }
      return itemStack;
    });


    ICON_PROVIDERS.put("command", (data) -> {
      for (Map.Entry<String, ItemModel> entry : CobbleUtils.config.getItemsCommands().entrySet()) {
        if (data.startsWith(entry.getKey())) {
          return entry.getValue().getItemStack();
        }
      }
      return new ItemStack(Items.COMMAND_BLOCK);
    });
    ICON_PROVIDERS.put("money", (data) -> {
      MoneyRewardData moneyData = parseMoneyRewardData(data);
      if (moneyData == null) return new ItemStack(Items.GOLD_INGOT);
      var map = CobbleUtils.language.getItemsEconomy();
      ItemModel itemModel = map.getOrDefault(moneyData.getEconomy(), ItemModel.builder()
        .item("item:1:minecraft:gold_ingot")
        .build());
      String display = itemModel.getItem().split(":", 2)[1];
      ItemStackRewardData itemStackRewardData = parseItemStackRewardData(display);
      ItemStack itemStack = itemStackRewardData.getItemStack();
      if (itemStack == null) itemStack = new ItemStack(Items.GOLD_INGOT);
      String name;
      boolean sameAmount = moneyData.getMinAmount() == moneyData.getMaxAmount();
      if (sameAmount) {
        name = "Money: " + moneyData.format(moneyData.getFinalAmount());
      } else {
        name = "Money: " + moneyData.format(moneyData.getMinAmount()) + " - " + moneyData.format(moneyData.getMaxAmount());
      }
      itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(name));
      return itemStack;
    });
    ICON_PROVIDERS.put("pokemon", (data) -> {
      try {
        Pokemon pokemon = PokemonProperties.Companion.parse(data).create();
        return PokemonItem.from(pokemon);
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    });
    ICON_PROVIDERS.put("message", (data) -> new ItemStack(Items.PAPER));
  }

  public static RewardExecutor getRewardExecutor(String type) {
    return EXECUTORS.get(type);
  }

  public static IconProvider getRewardIconProvider(String type) {
    return ICON_PROVIDERS.get(type);
  }
}
