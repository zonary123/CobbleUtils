package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * @author Carlos Varas Alonso - 12/06/2024 3:48
 */
public class Reload implements Command<CommandSourceStack> {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                              LiteralArgumentBuilder<CommandSourceStack> base) {
    dispatcher.register(
      base.requires(source -> source.hasPermission(2)).then(
        Commands.literal("reload")
          .requires(
            source -> source.hasPermission(2)
          )
          .executes(new Reload()))
    );

    for (String literal : CobbleUtils.config.getCommandparty()) {
      dispatcher.register(
        Commands.literal(literal)
          .then(
            Commands.literal("reload")
              .requires(
                source -> source.hasPermission(2))
              .executes(new Reload())
          )
      );
    }


  }

  @Override public int run(CommandContext<CommandSourceStack> context) {
    CobbleUtils.load();
    return 1;
  }
}
