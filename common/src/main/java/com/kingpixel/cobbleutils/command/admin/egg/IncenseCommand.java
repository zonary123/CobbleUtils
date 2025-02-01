package com.kingpixel.cobbleutils.command.admin.egg;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 12/08/2024 13:07
 */
public class IncenseCommand implements Command<ServerCommandSource> {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.then(
        CommandManager.literal("incense")

          .then(
            CommandManager.argument("item", StringArgumentType.greedyString())
              .suggests((context, builder) -> {
                CobbleUtils.breedconfig.getIncenses().forEach(incense -> builder.suggest(incense.getName()));
                return builder.buildFuture();
              })
              .executes(context -> {
                if (!context.getSource().isExecutedByPlayer()) {
                  return 0;
                }
                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                return getIncense(context, player, 1);
              }).then(
                CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                  .executes(context -> {
                    if (!context.getSource().isExecutedByPlayer()) {
                      return 0;
                    }
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    return getIncense(context, player, IntegerArgumentType.getInteger(context, "amount"));
                  })
                  .then(
                    CommandManager.literal("other")
                      .then(
                        CommandManager.argument("player", EntityArgumentType.players())
                          .executes(
                            context -> {
                              ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                              return getIncense(context, player, IntegerArgumentType.getInteger(context, "amount"));
                            }
                          )
                      )
                  )
              )
          )
      )
    );

  }

  private static int getIncense(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, int amount) {
    ItemStack itemStack = CobbleUtils.breedconfig.getIncenses().stream().filter(
        incense -> incense.getName().equalsIgnoreCase(StringArgumentType.getString(context, "item")))
      .findFirst()
      .map(incense -> incense.getItemStack().copy())
      .orElse(null);

    return giveIncense(player, itemStack, amount);
  }

  private static int giveIncense(ServerPlayerEntity player, ItemStack itemStack, int amount) {
    if (itemStack == null) return 0;
    ItemStack stack = itemStack.copy();
    stack.setCount(amount);
    if (!player.getInventory().insertStack(itemStack)) {
      player.dropItem(itemStack, false);
    }
    return 1;
  }

  @Override public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 0;
  }
}
