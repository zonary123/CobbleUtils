package com.kingpixel.cobbleutils.Model.rewards;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.ItemChance;
import lombok.*;
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

  public boolean canGive(ServerPlayerEntity player) {
    return true;
  }

  /**
   * Obtiene el ícono para el GUI de manera segura
   */
  public ItemStack getIcon() {
    String firstReward = reward.split("\\|")[0];
    String[] parts = firstReward.split(":", 2);

    String type = parts[0];
    String data = parts[1];

    IconProvider provider = RewardRegistry.getRewardIconProvider(type);
    ItemStack icon = provider != null ? safeGetIcon(provider, data) : null;

    return icon != null ? icon : Items.BARRIER.getDefaultStack();
  }


  /**
   * Ejecuta el provider de manera segura
   */
  private static ItemStack safeGetIcon(IconProvider provider, String data) {
    try {
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

        executor.execute(player, this, data);
      }
      return true;
    });
  }


  public CompletableFuture<Void> giveToPlayerDisconnected(UUID playerUUID) {
    return CompletableFuture.completedFuture(null);
  }


  public boolean existType() {
    return RewardRegistry.getRewardExecutor(reward.split(":")[0]) != null;
  }
}
