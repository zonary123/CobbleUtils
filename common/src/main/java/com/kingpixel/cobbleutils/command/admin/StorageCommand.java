package com.kingpixel.cobbleutils.command.admin;

import com.cobblemon.mod.common.command.argument.PokemonPropertiesArgumentType;
import com.cobblemon.mod.common.command.argument.PokemonStoreArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.command.suggests.CobbleUtilsSuggests;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.models.StorageItemStack;
import com.kingpixel.cobbleutils.database.users.models.StoragePokemon;
import com.kingpixel.cobbleutils.database.users.models.StorageRewards;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 07/11/2024 4:18
 */
public class StorageCommand {
  private static final String ARG_PLAYER = "player";
  private static final String ARG_POKEMON = "pokemon";
  private static final String ARG_ITEM = "item";
  private static final String ARG_AMOUNT = "amount";
  private static final List<String> permissions = List.of("cobbleutils.admin");

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry) {
    dispatcher.register(
      CommandManager.literal("storage")
        .executes(context -> {
          ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
          CobbleUtils.language.getStorageMenu().open(player, player.getUuid());
          return 1;
        }).then(
          CommandManager.literal("add")
            .requires(source -> PermissionApi.hasPermission(source, permissions, 2))
            .then(
              CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.suggestPlayerName(ARG_PLAYER, permissions, 2)
                .then(
                  CommandManager.literal(ARG_POKEMON)
                    .then(
                      CommandManager.literal("slot")
                        .then(
                          CommandManager.argument("slot", PokemonStoreArgumentType.Companion.pokemonStore())
                            .executes(context -> {
                              Pokemon pokemon = PokemonPropertiesArgumentType.Companion.getPokemonProperties(context, "slot").create();
                              CobbleUtils.EXECUTOR_COBBLEUTILS.execute(() ->
                                DataBaseFactory.dataBaseUsers.addStorage(
                                  new StoragePokemon(pokemon),
                                  getPlayerUUID(context)
                                )
                              );
                              return 1;
                            })
                        )
                    ).then(
                      CommandManager.argument(ARG_POKEMON, PokemonPropertiesArgumentType.Companion.properties())
                        .executes(context -> {
                          Pokemon pokemon = PokemonPropertiesArgumentType.Companion.getPokemonProperties(context, ARG_POKEMON).create();
                          CobbleUtils.EXECUTOR_COBBLEUTILS.execute(() ->
                            DataBaseFactory.dataBaseUsers.addStorage(
                              new StoragePokemon(pokemon),
                              getPlayerUUID(context)
                            )
                          );
                          return 1;
                        })
                    )
                )
                .then(
                  CommandManager.literal("itemstack")
                    .then(
                      CommandManager.literal("hand")
                        .executes(context -> {
                          ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                          var itemStack = player.getMainHandStack().copy();
                          CobbleUtils.EXECUTOR_COBBLEUTILS.execute(() ->
                            DataBaseFactory.dataBaseUsers.addStorage(
                              new StorageItemStack(itemStack),
                              getPlayerUUID(context)
                            )
                          );
                          return 1;
                        })
                    ).then(
                      CommandManager.literal("command")
                        .then(
                          CommandManager.argument(ARG_AMOUNT, IntegerArgumentType.integer())
                            .then(
                              CommandManager.argument(ARG_ITEM, ItemStackArgumentType.itemStack(registry))
                                .executes(context -> {
                                  int amount = IntegerArgumentType.getInteger(context, ARG_AMOUNT);
                                  var itemStack = ItemStackArgumentType.getItemStackArgument(context, ARG_ITEM).createStack(amount, true);
                                  CobbleUtils.EXECUTOR_COBBLEUTILS.execute(() ->
                                    DataBaseFactory.dataBaseUsers.addStorage(
                                      new StorageItemStack(itemStack),
                                      getPlayerUUID(context)
                                    )
                                  );
                                  return 1;
                                })
                            )
                        )
                    )
                )
                .then(
                  CommandManager.literal("reward")
                    .then(
                      CommandManager.argument("data", StringArgumentType.greedyString())
                        .executes(context -> {
                          String data = StringArgumentType.getString(context, "data");
                          CobbleUtils.EXECUTOR_COBBLEUTILS.execute(() ->
                            DataBaseFactory.dataBaseUsers.addStorage(
                              new StorageRewards(new ItemChance(data, 100)),
                              getPlayerUUID(context)
                            )
                          );
                          return 1;
                        })
                    )
                )
            )
        ).then(
          CommandManager.literal("view")
            .requires(source -> PermissionApi.hasPermission(source, permissions, 2))
            .then(
              CobbleUtilsSuggests.SUGGESTS_PLAYER_OFFLINE_AND_ONLINE.suggestPlayerName(ARG_PLAYER, permissions, 2)
                .executes(context -> {
                  ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                  CobbleUtils.EXECUTOR_COBBLEUTILS.execute(() -> {
                    UUID playerUUID = getPlayerUUID(context);
                    var user = DataBaseFactory.dataBaseUsers.findUserByUUID(playerUUID);
                    if (user != null) CobbleUtils.language.getStorageMenu().open(player, playerUUID);
                  });
                  return 1;
                })
            )
        ).then(
          CommandManager.literal("other")
            .requires(source -> PermissionApi.hasPermission(source, permissions, 2))
            .then(
              CommandManager.argument(ARG_PLAYER, EntityArgumentType.player())
                .executes(context -> {
                  ServerPlayerEntity target = EntityArgumentType.getPlayer(context, ARG_PLAYER);
                  CobbleUtils.EXECUTOR_COBBLEUTILS.execute(() -> CobbleUtils.language.getStorageMenu().open(target, target.getUuid()));
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
}
