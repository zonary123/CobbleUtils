package com.kingpixel.cobbleutils.Model.rewards;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.EconomySelector;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.api.RewardsAPI;
import com.kingpixel.cobbleutils.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Map;
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
    String[] parts = data.split(":", 4);
    String amountPart = parts[0];
    String economy = parts.length > 2 ? parts[1] : "";
    String currency = parts.length > 3 ? parts[2] : "";
    @Nullable String reason = parts.length == 4 ? parts[3] : null;

    double min, max;
    if (amountPart.contains("-")) {
      String[] range = amountPart.split("-");
      min = Double.parseDouble(range[0]);
      max = Double.parseDouble(range[1]);
    } else {
      min = max = Double.parseDouble(amountPart);
    }

    double finalAmount = min == max ? min : ThreadLocalRandom.current().nextDouble(min, max);

    if (reason == null) reason = "Money Reward: " + finalAmount;

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

  @Nullable
  private static ItemStack getItemStack(String data) {
    String[] nbtSplit = data.split("#", 2);
    String nbt = nbtSplit.length > 1 ? nbtSplit[1] : null;

    String[] parts = nbtSplit[0].split(":", 2);
    if (parts.length < 2) return null;

    int finalAmount = parts[0].contains("-")
      ? ThreadLocalRandom.current().nextInt(Integer.parseInt(parts[0].split("-")[0]), Integer.parseInt(parts[0].split("-")[1]) + 1)
      : Integer.parseInt(parts[0]);
    String itemId = parts[1];

    ItemStack itemStack;
    if (nbt != null && !nbt.isEmpty()) {
      itemStack = ItemUtils.applyNbt(itemId, Utils.parseItemId(itemId, finalAmount), nbt, finalAmount);
    } else {
      itemStack = Utils.parseItemId(itemId, finalAmount);
    }
    return itemStack;
  }

  static {
    // ----------------- EXECUTORS -----------------
    EXECUTORS.put("id", (player, reward, data) -> RewardsAPI.giveReward(player.getUuid(), reward));
    EXECUTORS.put("mod", (player, reward, data) -> Items.BARRIER.getDefaultStack());
    EXECUTORS.put("item", (player, reward, data) -> {
      try {
        ItemStack itemStack = getItemStack(data);
        CobbleUtils.server.execute(() -> {
          if (player.getInventory().getEmptySlot() == -1) {
            reward.giveToPlayerDisconnected(player.getUuid());
          } else {
            player.getInventory().offerOrDrop(itemStack);
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    });


    EXECUTORS.put("command", (player, reward, data) -> PlayerUtils.executeCommand(data, player));
    EXECUTORS.put("money", (player, reward, data) -> {
      try {
        MoneyRewardData moneyData = parseMoneyRewardData(data);
        if (moneyData == null) return;
        double finalAmount = moneyData.getFinalAmount();
        new EconomySelector(moneyData.getEconomy(), moneyData.getCurrency())
          .deposit(player.getUuid(), BigDecimal.valueOf(finalAmount), moneyData.getReason()
            .replace("%money%", finalAmount + ""))
          .whenComplete((res, err) -> {
            if (err != null) err.printStackTrace();
          });
      } catch (Exception e) {
        e.printStackTrace();
      }
    });


    EXECUTORS.put("pokemon", (player, reward, data) -> {
      Pokemon pokemon = PokemonProperties.Companion.parse(data).create();
      CobbleUtils.server.execute(() -> {
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
        if (party.size() != 6) {
          party.add(pokemon);
        } else {
          var pc = Cobblemon.INSTANCE.getStorage().getPC(player);
          if (!pc.add(pokemon)) {
            reward.giveToPlayerDisconnected(player.getUuid());
          }
        }
      });
    });

    EXECUTORS.put("message", (player, reward, data) -> PlayerUtils.sendMessage(player, data, CobbleUtils.config.getPrefix(), TypeMessage.CHAT));

    // ----------------- ICON_PROVIDERS -----------------
    ICON_PROVIDERS.put("mod", (data) -> Items.BARRIER.getDefaultStack());
    ICON_PROVIDERS.put("item", (data) -> {
      ItemStack itemStack = getItemStack(data);
      return itemStack == null ? Items.BARRIER.getDefaultStack() : itemStack;
    });


    ICON_PROVIDERS.put("command", (data) -> new ItemStack(Items.COMMAND_BLOCK));
    ICON_PROVIDERS.put("money", (data) -> {
      MoneyRewardData moneyData = parseMoneyRewardData(data);
      if (moneyData == null) return new ItemStack(Items.GOLD_INGOT);
      var map = CobbleUtils.language.getItemsEconomy();
      ItemModel itemModel = map.getOrDefault(moneyData.getEconomy(), ItemModel.builder()
        .item("item:1:minecraft:gold_ingot")
        .build());
      String display = itemModel.getItem().split(":", 2)[1];
      ItemStack itemStack = getItemStack(display);
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
