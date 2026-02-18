package com.kingpixel.cobbleutils.Model.rewards;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.EconomySelector;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.api.RewardsAPI;
import com.kingpixel.cobbleutils.util.ItemUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reward {
  public static final Map<String, RewardExecutor> EXECUTORS = new HashMap<>();
  public static final Map<String, RewardIconProvider> ICON_PROVIDERS = new HashMap<>();
  @Builder.Default
  private String id = null;
  @Builder.Default
  private String reward = "minecraft:stone";
  @Builder.Default
  private double weight = 1.0;
  @Builder.Default
  private Boolean unique = false;
  @Builder.Default
  private String identifier = null;
  @Builder.Default
  private Integer amount = null;
  @Builder.Default
  private DurationValue cooldown = null;
  @Builder.Default
  private String display = null;
  @Builder.Default
  private String displayname = null;

  public ItemChance toItemChance() {
    return ItemChance.builder()
      .item(reward)
      .chance(weight)
      .unique(unique)
      .identifier(identifier)
      .amount(amount)
      .cooldown(cooldown)
      .display(display)
      .displayname(displayname)
      .build();
  }

  public void fix() {
    if (reward == null) reward = "minecraft:stone";
    if (unique) {
      if (identifier == null) identifier = UUID.randomUUID().toString();
      if (amount == null) amount = 1;
      if (cooldown == null) cooldown = DurationValue.parse("60m");
    } else {
      identifier = null;
      amount = null;
      cooldown = null;
    }
  }

  /**
   * Obtiene el ícono para el GUI de manera segura
   */
  public ItemStack getIcon() {
    String firstReward = reward.split("\\|")[0];
    String[] parts = firstReward.split(":", 2);

    String type = parts[0];
    String data = parts.length > 1 ? parts[1] : "";

    RewardIconProvider provider = ICON_PROVIDERS.get(type);
    ItemStack icon = provider != null ? safeGetIcon(provider, this, data) : null;

    return icon != null ? icon : Items.BARRIER.getDefaultStack();
  }


  /**
   * Ejecuta el provider de manera segura
   */
  private static ItemStack safeGetIcon(RewardIconProvider provider, Reward reward, String data) {
    try {
      ItemStack icon = provider.getIcon(reward, data);
      if (icon != null && reward.getDisplay() != null) {
        icon.set(DataComponentTypes.CUSTOM_NAME, Text.literal(reward.getDisplay()));
      }
      return icon;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public CompletableFuture<Void> giveToPlayer(@NonNull ServerPlayerEntity player) {
    return CobbleUtils.ASYNC.runAsync(() -> {
      String[] allRewards = reward.split("\\|");
      for (String singleReward : allRewards) {
        singleReward = singleReward.trim();
        if (singleReward.isEmpty()) continue;

        String[] rewardParts = singleReward.split(":", 2);
        if (rewardParts.length < 2) {
          CobbleUtils.LOGGER.error("Invalid reward format: {}", singleReward);
          continue;
        }

        String type = rewardParts[0];
        String data = rewardParts[1];

        RewardExecutor executor = EXECUTORS.get(type);
        if (executor == null) {
          executor = EXECUTORS.get("item");
          if (executor == null) {
            CobbleUtils.LOGGER.error("No executor found for reward type: {}", type);
            continue;
          }
          data = type + ":" + data;
        }

        executor.execute(player, this, data);
      }
    });
  }


  public CompletableFuture<Void> giveToPlayerDisconnected(UUID playerUUID) {
    return CompletableFuture.completedFuture(null);
  }

  static {
    // ----------------- EXECUTORS -----------------
    EXECUTORS.put("id", (player, reward, data) -> RewardsAPI.giveReward(player.getUuid(), reward));
    EXECUTORS.put("mod", (player, reward, data) -> Items.BARRIER.getDefaultStack());
    EXECUTORS.put("item", (player, reward, data) -> {
      try {
        String[] nbtSplit = data.split("#", 2);
        String nbt = nbtSplit.length > 1 ? nbtSplit[1] : null;

        String[] parts = nbtSplit[0].split(":", 2);
        if (parts.length < 2) return;

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

        ItemStack finalItemStack = itemStack;
        CobbleUtils.server.execute(() -> {
          if (player.getInventory().getEmptySlot() == -1) {
            reward.giveToPlayerDisconnected(player.getUuid());
          } else {
            player.getInventory().offerOrDrop(finalItemStack);
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    });


    EXECUTORS.put("command", (player, reward, data) -> PlayerUtils.executeCommand(data, player));
    EXECUTORS.put("money", (player, reward, data) -> {
      try {
        String[] parts = data.split(":", 5);
        if (parts.length < 2) return;

        String amountPart = parts[0];
        String economy = parts.length > 2 ? parts[1] : "";
        String currency = parts.length > 3 ? parts[2] : "";
        @Nullable String reason = parts.length == 5 ? parts[4] : null;

        double min, max;
        if (amountPart.contains("-")) {
          String[] range = amountPart.split("-");
          min = Double.parseDouble(range[0]);
          max = Double.parseDouble(range[1]);
        } else {
          min = max = Double.parseDouble(amountPart);
        }

        double finalAmount = ThreadLocalRandom.current().nextDouble(min, max + 1);

        if (reason == null) reason = "Money Reward: " + finalAmount;

        new EconomySelector(economy, currency)
          .deposit(player.getUuid(), BigDecimal.valueOf(finalAmount), reason.replace("%money%", finalAmount + ""))
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
    ICON_PROVIDERS.put("mod", (reward1, data) -> Items.BARRIER.getDefaultStack());
    ICON_PROVIDERS.put("item", (reward, data) -> {
      try {
        String[] nbtSplit = data.split("#", 2);
        String nbt = nbtSplit.length > 1 ? nbtSplit[1] : null;

        String[] parts = nbtSplit[0].split(":", 2);
        if (parts.length < 2) return new ItemStack(Items.BARRIER);

        int finalAmount = parts[0].contains("-")
          ? ThreadLocalRandom.current().nextInt(
          Integer.parseInt(parts[0].split("-")[0]),
          Integer.parseInt(parts[0].split("-")[1]) + 1
        )
          : Integer.parseInt(parts[0]);
        String itemId = parts[1];

        ItemStack itemStack;
        if (nbt != null && !nbt.isEmpty()) {
          itemStack = ItemUtils.applyNbt(itemId, Utils.parseItemId(itemId, finalAmount), nbt, finalAmount);
        } else {
          itemStack = Utils.parseItemId(itemId, finalAmount);
        }
        return itemStack;
      } catch (Exception e) {
        e.printStackTrace();
        return new ItemStack(Items.BARRIER);
      }
    });


    ICON_PROVIDERS.put("command", (reward, data) -> new ItemStack(Items.COMMAND_BLOCK));
    ICON_PROVIDERS.put("money", (reward, data) -> new ItemStack(Items.GOLD_INGOT));
    ICON_PROVIDERS.put("pokemon", (reward, data) -> {
      try {
        Pokemon pokemon = PokemonProperties.Companion.parse(reward.getReward().split(":", 2)[1]).create();
        return PokemonItem.from(pokemon);
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    });
    ICON_PROVIDERS.put("message", (reward, data) -> new ItemStack(Items.PAPER));
  }
}
