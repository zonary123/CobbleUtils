package com.kingpixel.cobbleutils.Model.rewards;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.models.StorageRewards;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.ItemUtils;
import lombok.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reward {
  private transient ItemStack cacheItemStack = null;
  @Builder.Default
  private String id = null;
  @Builder.Default
  private String reward = "item:1:minecraft:stone";
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

  @SuppressWarnings(value = "ALL")
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

  private String getType() {
    if (reward == null || reward.isBlank()) return null;
    String[] parts = reward.split(":", 2);
    if (parts.length < 2) return null;
    return parts[0].trim();
  }

  public boolean equalsItemStack(ItemStack stack) {
    if (cacheItemStack != null) return ItemUtils.equals(cacheItemStack, stack);
    String type = getType();
    if (type == null) return false;
    if (type.equals("item")) {
      cacheItemStack = buildFromString(reward);
      return ItemUtils.equals(cacheItemStack, stack);
    }
    return false;
  }

  public void fix() {
    if (reward == null) reward = "item:1:minecraft:stone";
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
    ItemStack icon = null;
    if (display != null && !display.isBlank()) icon = buildFromString(display);

    if ((icon == null || icon.isEmpty()) && reward != null && !reward.isBlank()) {
      String cleanedReward = clean(reward);
      String[] rewardParts = cleanedReward.split("\\|");
      if (rewardParts.length > 0) icon = buildFromString(rewardParts[0]);
    }

    if (icon == null || icon.isEmpty()) icon = Items.BARRIER.getDefaultStack();

    if (displayname != null && !displayname.isBlank() && icon != null && !icon.isEmpty()) {
      icon = icon.copy();
      icon.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(displayname));
    }

    return icon;
  }

  private ItemStack buildFromString(String input) {
    if (input == null || input.isBlank()) return null;

    String cleaned = clean(input);
    String[] parts = cleaned.split(":", 2);

    if (parts.length < 2) return null;

    String type = parts[0].trim();
    String data = parts[1].trim();

    IconProvider provider = RewardRegistry.getRewardIconProvider(type);
    if (provider == null) return null;

    return safeGetIcon(provider, data);
  }

  private String clean(String input) {
    return input.replace("\"", "").trim();
  }

  /**
   * Ejecuta el provider de manera segura
   */
  private static ItemStack safeGetIcon(IconProvider provider, String data) {
    try {
      if (provider == null) {
        CobbleUtils.LOGGER.warn("No icon provider found for reward type: " + data);
        return Items.BARRIER.getDefaultStack();
      }
      return provider.getIcon(data);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public CompletableFuture<Boolean> giveToPlayer(@NonNull ServerPlayerEntity player) {
    return CobbleUtils.ASYNC.supply(() -> {
        String[] allRewards = reward.split("\\|");
        for (String singleReward : allRewards) {
          try {
            singleReward = singleReward.trim();
            if (singleReward.isEmpty()) continue;

            String[] rewardParts = singleReward.split(":", 2);
            if (rewardParts.length < 2) {
              player.sendMessage(
                Text.literal("Invalid reward format: " + singleReward)
              );
              continue;
            }

            String type = rewardParts[0];
            String data = rewardParts[1];

            RewardExecutor executor = RewardRegistry.getRewardExecutor(type);
            if (executor == null) {
              player.sendMessage(
                Text.literal(
                  "Unknown reward type: " + type + " for reward: " + singleReward
                )
              );
              continue;
            }

            String finalSingleReward = singleReward;
            executor.execute(player, this, data)
              .whenCompleteAsync((success, throwable) -> {
                if (throwable != null) {
                  throwable.printStackTrace();
                }

                if (throwable != null || Boolean.FALSE.equals(success)) {
                  final Reward finalReward = Reward.builder()
                    .reward(finalSingleReward)
                    .build();

                  DataBaseFactory.dataBaseUsers.addStorage(
                    StorageRewards.builder()
                      .reward(finalReward)
                      .build(),
                    player.getUuid()
                  );
                }
              });


          } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(
              Text.literal("Error giving reward: " + singleReward)
            );
          }
        }
        return true;
      })
      .exceptionally(throwable -> {
        throwable.printStackTrace();
        return false;
      });
  }


  public CompletableFuture<Boolean> giveToPlayerDisconnected(UUID playerUUID) {
    return DataBaseFactory.dataBaseUsers.addStorage(StorageRewards.builder().reward(this).build(), playerUUID);
  }


  public boolean existType() {
    String type = reward.split(":", 2)[0];
    if (type == null || type.isEmpty()) return false;
    return RewardRegistry.getRewardExecutor(type) != null;
  }
}
