package com.kingpixel.cobbleutils.Model.rewards;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.api.PermissionApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvancedReward {
  @Builder.Default
  private String id = "";
  @Builder.Default
  private boolean showMenu = true;
  @Builder.Default
  private boolean giveAll = false;
  @Builder.Default
  private boolean separateGroups = false;
  @Builder.Default
  private boolean allowDuplicates = false;
  @Builder.Default
  private Map<String, Integer> permissionsAmounts = new LinkedHashMap<>(Map.of(
    "", 1,
    "cobbleutils.reward.advanced", 3
  ));

  @Builder.Default
  private Map<String, List<Reward>> lootTable = new LinkedHashMap<>(Map.of(
    "", EXAMPLE_REWARD_LIST,
    "cobbleutils.reward.advanced", EXAMPLE_REWARD_LIST
  ));

  private static final List<Reward> EXAMPLE_REWARD_LIST = List.of(
    Reward.builder().reward("item:1:minecraft:stone").weight(1.0).build(),
    Reward.builder().reward("item:1:minecraft:stone#[minecraft:custom_model_data=1]").weight(1.0).build(),
    Reward.builder().reward("item:1:minecraft:stone#[minecraft:custom_model_data=2]").weight(-1.0).build(),
    Reward.builder().reward("command:give %player% minecraft:stone").weight(1.0).build(),
    Reward.builder().reward("money:1").weight(1.0).build(),
    Reward.builder().reward("money:1-1").weight(1.0).build(),
    Reward.builder().reward("money:1:IMPACTOR:dollars:Example Reason %money%").weight(1.0).build(),
    Reward.builder().reward("money:1-1:IMPACTOR:dollars:Example Reason %money%").weight(1.0).build(),
    Reward.builder().reward("message:You got a reward!").weight(1.0).build(),
    Reward.builder().reward("pokemon:rattata").weight(1.0).build()
  );

  private int getTotalAmount(UUID playerUUID) {
    int total = permissionsAmounts.getOrDefault("", 1);
    for (Map.Entry<String, Integer> entry : permissionsAmounts.entrySet()) {
      String perm = entry.getKey();
      int amt = entry.getValue();
      if (amt > total && (perm.isEmpty() || PermissionApi.hasPermission(playerUUID, perm, 2))) {
        total = amt;
      }
    }
    return total;
  }

  private List<Reward> getRewardsForPlayer(UUID playerUUID) {
    if (lootTable.isEmpty()) return Collections.emptyList();

    List<Reward> result;
    if (separateGroups) {
      result = lootTable.getOrDefault("", Collections.emptyList());
      for (Map.Entry<String, List<Reward>> entry : lootTable.entrySet()) {
        String perm = entry.getKey();
        if (!perm.isEmpty() && PermissionApi.hasPermission(playerUUID, perm, 2)) {
          result = entry.getValue();
        }
      }
    } else {
      result = new ArrayList<>(lootTable.getOrDefault("", Collections.emptyList()));
      for (Map.Entry<String, List<Reward>> entry : lootTable.entrySet()) {
        String perm = entry.getKey();
        if (!perm.isEmpty() && PermissionApi.hasPermission(playerUUID, perm, 2)) {
          result.addAll(entry.getValue());
        }
      }
    }
    return result;
  }

  private List<Reward> getFinalRewards(UUID playerUUID) {
    List<Reward> pool = getRewardsForPlayer(playerUUID);
    if (pool.isEmpty()) return Collections.emptyList();

    if (giveAll) return new ArrayList<>(pool);

    List<Reward> result = new ArrayList<>();
    List<Reward> weightedPool = new ArrayList<>();
    double totalWeight = 0;

    for (Reward r : pool) {
      if (r.getWeight() <= 0) {
        result.add(r);
      } else {
        weightedPool.add(r);
        totalWeight += r.getWeight();
      }
    }

    int rolls = getTotalAmount(playerUUID);
    for (int i = 0; i < rolls && !weightedPool.isEmpty(); i++) {
      Reward picked = weightedPick(weightedPool, totalWeight);
      result.add(picked);

      if (!allowDuplicates) {
        weightedPool.remove(picked);
        totalWeight -= picked.getWeight();
      }
    }

    return result;
  }


  private Reward weightedPick(List<Reward> pool, double totalWeight) {
    double rnd = ThreadLocalRandom.current().nextDouble(totalWeight);
    double acc = 0;
    for (int i = 0; i < pool.size(); i++) {
      Reward r = pool.get(i);
      if (r.getWeight() <= 0) continue;
      acc += r.getWeight();
      if (rnd <= acc) return r;
    }
    return pool.getLast();
  }

  public CompletableFuture<Void> giveRewards(UUID playerUUID) {
    ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(playerUUID);
    return CobbleUtils.ASYNC.runAsync(() -> {
      List<Reward> finalRewards = getFinalRewards(playerUUID);
      if (finalRewards.isEmpty()) return;

      if (player != null) {
        for (Reward finalReward : finalRewards) {
          giveRewardToPlayer(player, finalReward);
        }
      } else {
        for (Reward finalReward : finalRewards) {
          giveRewardToPlayerDisconnected(playerUUID, finalReward);
        }
      }
    });
  }

  private void giveRewardToPlayer(ServerPlayerEntity player, Reward reward) {
    reward.giveToPlayer(player);
  }

  private void giveRewardToPlayerDisconnected(UUID playerUUID, Reward reward) {
    reward.giveToPlayerDisconnected(playerUUID);
  }

  /**
   * Opens the reward menu for the player. This should be called when the player wants to view their rewards, or when they receive a reward and showMenu is true.
   *
   * @param gui The GUI object containing the player and any necessary callbacks. This is used to open the menu and handle button actions.
   * @return A CompletableFuture that completes when the menu has been opened. The menu is opened asynchronously to avoid blocking the main server thread.
   */
  public CompletableFuture<Void> open(GUIAdvancedReward gui) {
    return CobbleUtils.ASYNC.runAsync(() -> {
      ChestTemplate template = ChestTemplate.builder(6)
        .build();

      ServerPlayerEntity player = gui.getPlayer();
      Consumer<ButtonAction> closeAction = gui.getCloseAction();
      Consumer<ChestTemplate> templateConsumer = gui.getTemplate();
      if (templateConsumer != null) templateConsumer.accept(template);

      Rectangle rectangle = new Rectangle(6);
      rectangle.apply(template);
      List<Button> buttons = new ArrayList<>();
      List<Reward> rewards = getRewardsForPlayer(player.getUuid());
      for (Reward reward : rewards) {
        buttons.add(GooeyButton.of(reward.getIcon()));
      }

      template.set(45, CobbleUtils.language.getItemPrevious().getLinkedPageButton(LinkType.Previous));
      template.set(49, CobbleUtils.language.getItemClose().getButton(closeAction));
      template.set(53, CobbleUtils.language.getItemNext().getLinkedPageButton(LinkType.Next));

      LinkedPage.Builder builder = LinkedPage.builder()
        .template(template)
        .title("Rewards");

      GooeyPage page = PaginationHelper.createPagesFromPlaceholders(template, buttons, builder);

      CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
    });
  }
}
