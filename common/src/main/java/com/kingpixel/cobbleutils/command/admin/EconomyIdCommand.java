package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 18/03/2025 21:52
 */
public class EconomyIdCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .then(
          CommandManager.literal("economys")
            .requires(source -> PermissionApi.hasPermission(
              source,
              "cobbleutils.command.economys",
              2
            ))
            .executes(context -> {
              List<String> economys = new ArrayList<>();
              EconomyApi.getEconomys().forEach((economyId, economy) -> economys.add(economy.getIdentify()));
              context.getSource().sendMessage(
                Text.literal("Economys -> " + economys)
              );
              return 1;
            })
        )
    );
  }
}
