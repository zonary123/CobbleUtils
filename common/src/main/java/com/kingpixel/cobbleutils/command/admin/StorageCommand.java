package com.kingpixel.cobbleutils.command.admin;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.command.argument.PokemonPropertiesArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.api.RewardsApi;
import com.kingpixel.cobbleutils.command.suggests.CobbleUtilsSuggests;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.database.users.models.StorageItemStack;
import com.kingpixel.cobbleutils.database.users.models.StoragePokemon;
import com.kingpixel.cobbleutils.database.users.models.StorageRewards;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
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
                              Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot").clone(true, DynamicRegistryManager.EMPTY);
                              return addStorageSafe(context, new StoragePokemon(pokemon),
                                "Pokémon " + pokemon.getDisplayName(false).getString());
                            })
                        )
                    )
                    .then(
                      CommandManager.literal("properties")
                        .then(
                          CommandManager.argument(ARG_POKEMON, PokemonPropertiesArgumentType.Companion.properties())
                            .executes(context -> {
                              Pokemon pokemon = PokemonPropertiesArgumentType.Companion.getPokemonProperties(context, ARG_POKEMON).create();
                              return addStorageSafe(context, new StoragePokemon(pokemon),
                                "Pokémon " + pokemon.getDisplayName(false).getString());
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
                          ServerPlayerEntity sender = context.getSource().getPlayerOrThrow();
                          var itemStack = sender.getMainHandStack().copy();
                          if (itemStack.isEmpty()) {
                            sendFeedback(context.getSource(), "⚠️ You must be holding an item to use this command.");
                            return 0;
                          }
                          return addStorageSafe(context, new StorageItemStack(itemStack),
                            itemStack.getCount() + "x " + itemStack.getName().getString());
                        })
                    )
                    .then(
                      CommandManager.literal("command")
                        .then(
                          CommandManager.argument(ARG_AMOUNT, IntegerArgumentType.integer())
                            .then(
                              CommandManager.argument(ARG_ITEM, ItemStackArgumentType.itemStack(registry))
                                .executes(context -> {
                                  int amount = IntegerArgumentType.getInteger(context, ARG_AMOUNT);
                                  var itemStack = ItemStackArgumentType.getItemStackArgument(context, ARG_ITEM).createStack(amount, true);
                                  if (itemStack.isEmpty()) {
                                    sendFeedback(context.getSource(), "⚠️ The specified item stack is empty.");
                                    return 0;
                                  }
                                  return addStorageSafe(context, new StorageItemStack(itemStack),
                                    amount + "x " + itemStack.getName().getString());
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
                          String data = StringArgumentType.getString(context, "data");
                          ItemChance itemChance = null;
                          if (data.startsWith("id:")) {
                            itemChance = RewardsApi.getReward(data.substring(3));
                          } else {
                            itemChance = new ItemChance(data, 100);
                          }
                          if (itemChance == null) {
                            sendFeedback(context.getSource(), "⚠️ Reward '" + data + "' not found.");
                            return 0;
                          }
                          return addStorageSafe(context, new StorageRewards(itemChance), "reward '" + data + "'");
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

                  var user = DataBaseFactory.dataBaseUsers.findUser(playerUUID);
                  if (user != null) {
                    CobbleUtils.language.getStorageMenu().open(player, playerUUID);
                    sendFeedback(context.getSource(), "📦 Displaying storage for " + targetName + ".");
                  } else {
                    sendFeedback(context.getSource(), "⚠️ Player " + targetName + " was not found.");
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
                  CobbleUtils.language.getStorageMenu().open(target, target.getUuid());
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

  /**
   * Validates target player and adds storage. Returns 1 on success, 0 on failure.
   */
  private static int addStorageSafe(CommandContext<ServerCommandSource> context, Storage storage, String description) {
    ServerCommandSource source = context.getSource();
    String targetName = getTargetName(context);
    UUID targetUUID = getPlayerUUID(context);

    if (targetUUID == null) {
      sendFeedback(source, "⚠️ Player '" + targetName + "' not found.");
      return 0;
    }

    // Ensure user exists in DB before adding storage
    var user = DataBaseFactory.dataBaseUsers.findUser(targetUUID);
    if (user == null) {
      user = new com.kingpixel.cobbleutils.database.users.UserModel(targetUUID);
      user.fix();
      DataBaseFactory.dataBaseUsers.saveOrUpdateUser(user);
    }

    DataBaseFactory.dataBaseUsers.addStorage(storage, targetUUID);
    sendFeedback(source, "✅ Added " + description + " to " + targetName + "'s storage.");
    return 1;
  }

  private static void sendFeedback(ServerCommandSource source, String message) {
    try {
      source.sendFeedback(() -> Text.of(message), false);
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.warn("Error sending feedback message: " + e.getMessage());
    }
  }
}
