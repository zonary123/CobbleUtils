package com.kingpixel.cobbleutils.command.admin;

import com.cobblemon.mod.common.command.argument.PokemonPropertiesArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.kingpixel.cobbleutils.database.users.models.StorageItemStack;
import com.kingpixel.cobbleutils.database.users.models.StoragePokemon;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 07/11/2024 4:18
 */
public class StorageCommand {


  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry) {
    dispatcher.register(
      CommandManager.literal("storage")
        .executes(context -> {
          ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
          CobbleUtils.language.getStorageMenu().open(player);
          return 1;
        }).then(
          CommandManager.literal("add")
            .then(
              CommandManager.literal("pokemon")
                .then(CommandManager.argument("pokemon", PokemonPropertiesArgumentType.Companion.properties())
                  .executes(context -> {
                    Pokemon pokemon = PokemonPropertiesArgumentType.Companion.getPokemonProperties(context, "pokemon").create();
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    CobbleUtils.EXECUTOR_COBBLEUTILS.execute(() -> DataBaseFactory.dataBaseUsers.addStorage(new StoragePokemon(UUID.randomUUID(), pokemon), player.getUuid()));
                    return 1;
                  }))
            ).then(
              CommandManager.literal("itemstack")
                .then(
                  CommandManager.argument("data", ItemStackArgumentType.itemStack(registry))
                    .executes(context -> {
                      var itemStack = ItemStackArgumentType.getItemStackArgument(context, "itemstack").createStack(1, true);
                      ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                      CobbleUtils.EXECUTOR_COBBLEUTILS.execute(() -> DataBaseFactory.dataBaseUsers.addStorage(new StorageItemStack(UUID.randomUUID(), itemStack),
                        player.getUuid()));
                      return 1;
                    })
                )
            )
        )
    );
  }
}
