package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionAPI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 28/06/2024 20:04
 */
public class ShinyToken {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {

    dispatcher.register(
      base
        .then(
          CommandManager.literal("shinytoken")
            .requires(source -> PermissionAPI.hasPermission(source, List.of("cobbleutils.shinytoken", "cobbleutils.admin"), 2))
            .then(
              CommandManager.argument("player", EntityArgumentType.player())
                .then(
                  CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                    .executes(context -> {
                      run(context);
                      return 1;
                    })
                )
            )
        )
    );

  }

  public static void run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
    int amount = IntegerArgumentType.getInteger(context, "amount");
    ItemStack itemStack = CobbleUtils.config.getShinytoken().getItemStack(amount);
    NbtCompound nbtCompound = new NbtCompound();
    nbtCompound.putBoolean("shinytoken", true);
    itemStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbtCompound));
    player.getInventory().offerOrDrop(itemStack);
  }
}
