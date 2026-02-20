package com.kingpixel.cobbleutils.Model.rewards;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.EconomySelector;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.api.RewardsAPI;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.ItemUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
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

    EXECUTORS.put("item", (player, reward, data) ->
      DataBaseFactory.dataBaseUsers.isAvailableReward(player.getUuid(), reward)
        .thenCompose(available -> {
          if (!available) return CompletableFuture.completedFuture(true);
          ItemStackRewardData itemStackRewardData = parseItemStackRewardData(data);
          ItemStack itemStack = itemStackRewardData.getItemStack();
          return CobbleUtils.server.submit(() -> {
            if (itemStack == null) {
              player.sendMessage(Text.literal("Invalid item reward data: " + data));
              return false;
            }
            boolean given = false;
            if (player.getInventory().getEmptySlot() != -1) {
              player.getInventory().offerOrDrop(itemStack);
              given = true;
            }
            if (given && CobbleUtils.config.isNotifyRewards()) {
              player.sendMessage(
                AdventureTranslator.toNative(
                  CobbleUtils.language.getMessageRewardItemStack()
                    .replace("%item%", ItemUtils.getTranslatedName(itemStack))
                    .replace("%amount%", itemStack.getCount() + "")
                )
              );
            }
            return given;
          });
        })
    );

    EXECUTORS.put("command", (player, reward, data) ->
      DataBaseFactory.dataBaseUsers.isAvailableReward(player.getUuid(), reward)
        .thenCompose(available -> {
          if (!available) return CompletableFuture.completedFuture(true);
          return PlayerUtils.executeCommandCompletable(data, player);
        })
    );

    EXECUTORS.put("money", (player, reward, data) ->
      DataBaseFactory.dataBaseUsers.isAvailableReward(player.getUuid(), reward)
        .thenCompose(available -> {
          if (!available) return CompletableFuture.completedFuture(true);
          try {
            MoneyRewardData moneyData = parseMoneyRewardData(data);
            if (moneyData == null) return CompletableFuture.completedFuture(false);
            double finalAmount = moneyData.getFinalAmount();
            return new EconomySelector(moneyData.getEconomy(), moneyData.getCurrency())
              .deposit(player.getUuid(), BigDecimal.valueOf(finalAmount), moneyData.getReason().replace("%money%", finalAmount + ""))
              .thenCompose(economyResult -> CompletableFuture.completedFuture(economyResult.isSuccess()));
          } catch (Exception e) {
            e.printStackTrace();
          }
          return CompletableFuture.completedFuture(false);
        })
    );

    EXECUTORS.put("pokemon", (player, reward, data) ->
      DataBaseFactory.dataBaseUsers.isAvailableReward(player.getUuid(), reward)
        .thenCompose(available -> {
          if (!available) return CompletableFuture.completedFuture(true);
          return CobbleUtils.server.submit(() -> {
            Pokemon pokemon = PokemonProperties.Companion.parse(data).create();
            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            return party.add(pokemon);
          });
        })
    );

    EXECUTORS.put("message", (player, reward, data) -> {
      PlayerUtils.sendMessage(player, data, CobbleUtils.config.getPrefix(), TypeMessage.CHAT);
      return CompletableFuture.completedFuture(true);
    });
    // ----------------- ICON_PROVIDERS -----------------
    ICON_PROVIDERS.put("mod", (data) -> Items.BARRIER.getDefaultStack());
    ICON_PROVIDERS.put("item", (data) -> {
      ItemStackRewardData itemStackRewardData = parseItemStackRewardData(data);
      ItemStack itemStack = itemStackRewardData.getItemStack();
      int min = itemStackRewardData.getMinAmount();
      int max = itemStackRewardData.getMaxAmount();
      if (min != max) {
        itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(
          "Amount: " + min + " - " + max
        ));
      } else {
        itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(
          "Amount: " + min
        ));
      }
      return itemStack;
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
