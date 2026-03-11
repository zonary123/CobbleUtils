package com.kingpixel.cobbleutils.command.admin;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.command.argument.PokemonPropertiesArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.model.rewards.Reward;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.api.RewardsAPI;
import com.kingpixel.cobbleutils.command.suggests.CobbleUtilsSuggests;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.models.StorageItemStack;
import com.kingpixel.cobbleutils.database.users.models.StoragePokemon;
import com.kingpixel.cobbleutils.database.users.models.StorageRewards;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class StorageCommand {
  private static final String ARG_PLAYER = "player";
  private static final String ARG_POKEMON = "pokemon";
  private static final String ARG_ITEM = "item";
  private static final String ARG_AMOUNT = "amount";
  private static final List<String> PERMISSIONS = List.of("cobbleutils.admin");

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobbleutils.storage.base"), 1))
        .executes(context -> {
          ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
          CobbleUtils.language.getStorageMenu().open(player, player.getUuid());
          return 1;
        })
        .then(
          CommandManager.literal("add")
            .requires(source -> PermissionApi.hasPermission(source, PERMISSIONS, 2))
            .then(
              CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.suggestPlayerName(ARG_PLAYER, PERMISSIONS, 2)
                // --- Add Pokémon ---
                .then(
                  CommandManager.literal("pokemon")
                    .then(
                      CommandManager.literal("slot")
                        .then(
                          CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
                            .executes(context -> {
                              ServerCommandSource source = context.getSource();
                              UUID targetUUID = getPlayerUUID(context);
                              String targetName = getTargetName(context);
                              Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot").clone(true, CobbleUtils.server.getRegistryManager());

                              DataBaseFactory.dataBaseUsers.addStorage(new StoragePokemon(pokemon), targetUUID);
                              sendFeedback(source,
                                "✅ Added Pokémon " + pokemon.getDisplayName(false).getString() + " to " + targetName +
                                  "'s storage.");
                              return 1;
                            })
                        )
                    )
                    .then(
                      CommandManager.literal("properties")
                        .then(
                          CommandManager.argument(ARG_POKEMON, PokemonPropertiesArgumentType.Companion.properties())
                            .executes(context -> {
                              ServerCommandSource source = context.getSource();
                              UUID targetUUID = getPlayerUUID(context);
                              String targetName = getTargetName(context);
                              Pokemon pokemon = PokemonPropertiesArgumentType.Companion.getPokemonProperties(context, ARG_POKEMON).create();
                              DataBaseFactory.dataBaseUsers.addStorage(new StoragePokemon(pokemon), targetUUID);
                              sendFeedback(source, "✅ Added Pokémon " + pokemon.getDisplayName(false).getString() +
                                " to " + targetName + "'s storage.");
                              return 1;
                            })
                        )
                    )
                )
                // --- Add ItemStack ---
                .then(
                  CommandManager.literal("itemstack")
                    .then(
                      CommandManager.literal("hand")
                        .executes(context -> {
                          ServerCommandSource source = context.getSource();
                          ServerPlayerEntity sender = source.getPlayerOrThrow();
                          UUID targetUUID = getPlayerUUID(context);
                          String targetName = getTargetName(context);
                          var itemStack = sender.getMainHandStack().copy();
                          if (itemStack.isEmpty()) {
                            sendFeedback(source, "⚠️ You must be holding an item to use this command.");
                            return 0;
                          }
                          DataBaseFactory.dataBaseUsers.addStorage(new StorageItemStack(itemStack), targetUUID);
                          sendFeedback(source, "✅ Added " + itemStack.getCount() + "x " + itemStack.getName().getString() + " to " + targetName + "'s storage.");
                          return 1;
                        })
                    )
                    .then(
                      CommandManager.literal("command")
                        .then(
                          CommandManager.argument(ARG_AMOUNT, IntegerArgumentType.integer())
                            .then(
                              CommandManager.argument(ARG_ITEM, ItemStackArgumentType.itemStack(registry))
                                .executes(context -> {
                                  ServerCommandSource source = context.getSource();
                                  UUID targetUUID = getPlayerUUID(context);
                                  String targetName = getTargetName(context);
                                  int amount = IntegerArgumentType.getInteger(context, ARG_AMOUNT);
                                  var itemStack = ItemStackArgumentType.getItemStackArgument(context, ARG_ITEM).createStack(amount, true);
                                  if (itemStack.isEmpty()) {
                                    sendFeedback(source, "⚠️ The specified item stack is empty.");
                                    return 0;
                                  }
                                  DataBaseFactory.dataBaseUsers.addStorage(new StorageItemStack(itemStack), targetUUID);
                                  sendFeedback(source, "✅ Added " + amount + "x " + itemStack.getName().getString() + " to " + targetName + "'s storage.");
                                  return 1;
                                })
                            )
                        )
                    )
                )
                // --- Add Reward ---
                .then(
                  CommandManager.literal("reward")
                    .then(
                      CommandManager.argument("data", StringArgumentType.greedyString())
                        .executes(context -> {
                          ServerCommandSource source = context.getSource();
                          UUID targetUUID = getPlayerUUID(context);
                          String targetName = getTargetName(context);
                          String data = StringArgumentType.getString(context, "data").trim();
                          Reward reward;
                          if (data.startsWith("id:")) {
                            String id = data.replace("id:", "").trim();
                            reward = RewardsAPI.getRewardTemplate(id);
                          } else {
                            reward = Reward.builder().reward(data).build();
                          }
                          if (reward == null) {
                            sendFeedback(source, "⚠️ Invalid reward data: '" + data + "'.");
                            return 0;
                          }
                          if (!reward.existType()) {
                            sendFeedback(source, "⚠️ Reward type '" + reward.getReward() + "' does not exist for reward: '" + data + "'.");
                            return 0;
                          }

                          DataBaseFactory.dataBaseUsers.isAvailableReward(targetUUID, reward)
                            .whenComplete((isAvailable, throwable) -> {
                              if (throwable != null) {
                                sendFeedback(source, "⚠️ An error occurred while checking reward availability: " + throwable.getMessage());
                                return;
                              }
                              if (!isAvailable) {
                                sendFeedback(source, "⚠️ The reward '" + data + "' is not available for " + targetName + ". It may be unique and already claimed, or the player may be on cooldown for this reward.");
                                return;
                              }
                              DataBaseFactory.dataBaseUsers.addStorage(new StorageRewards(reward), targetUUID)
                                .whenComplete((added, addThrowable) -> {
                                  if (addThrowable != null) {
                                    sendFeedback(source, "⚠️ An error occurred while adding the reward to storage: " + addThrowable.getMessage());
                                    return;
                                  }
                                  if (added) {
                                    sendFeedback(source, "✅ Added reward '" + data + "' to " + targetName + "'s storage.");
                                    return;
                                  }
                                  sendFeedback(source, "⚠️ Failed to add reward '" + data + "' to " + targetName + "'s storage. Please try again.");
                                });
                            });

                          return 1;
                        })
                    )
                )
            )
        )
        // --- View Storage ---
        .then(
          CommandManager.literal("view")
            .requires(source -> PermissionApi.hasPermission(source, PERMISSIONS, 2))
            .then(
              CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.suggestPlayerName(ARG_PLAYER, PERMISSIONS, 2)
                .executes(context -> {
                  ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                  UUID playerUUID = getPlayerUUID(context);
                  String targetName = getTargetName(context);
                  var userCache = CobbleUtils.server.getUserCache();
                  if (userCache == null) return 0;

                  Optional<GameProfile> cachedProfile = userCache.findByName(targetName);
                  if (cachedProfile.isPresent()) {
                    CobbleUtils.language.getStorageMenu().open(player, cachedProfile.get().getId());
                  } else {
                    DataBaseFactory.dataBaseUsers.findUserModel(playerUUID)
                      .whenComplete((userModel, throwable) -> {
                        if (userModel == null) {
                          sendFeedback(context.getSource(), "⚠️ Player " + targetName + " was not found in the database.");
                          return;
                        }
                        CobbleUtils.language.getStorageMenu().open(player, userModel.getPlayerUUID());
                      });
                  }
                  return 1;
                })
            )
        )
        // --- Open another player's storage ---
        .then(
          CommandManager.literal("other")
            .requires(source -> PermissionApi.hasPermission(source, PERMISSIONS, 2))
            .then(
              CommandManager.argument(ARG_PLAYER, EntityArgumentType.player())
                .executes(context -> {
                  ServerPlayerEntity target = EntityArgumentType.getPlayer(context, ARG_PLAYER);
                  CobbleUtils.language.getStorageMenu().open(target, target.getGameProfile().getId());
                  sendFeedback(context.getSource(), "📦 Opened storage for " + target.getName().getString() + ".");
                  return 1;
                })
            )
        )
    );
  }

  private static UUID getPlayerUUID(CommandContext<ServerCommandSource> context) {
    String playerName = context.getArgument(ARG_PLAYER, String.class);
    return CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.getPlayerUUIDWithName(playerName);
  }

  private static String getTargetName(CommandContext<ServerCommandSource> context) {
    return context.getArgument(ARG_PLAYER, String.class);
  }

  private static void sendFeedback(ServerCommandSource source, String message) {
    try {
      source.sendFeedback(() -> Text.of(message), false);
    } catch (Exception e) {
      CobbleUtils.LOGGER.warn("Error sending feedback message: " + e.getMessage());
    }
  }
}
