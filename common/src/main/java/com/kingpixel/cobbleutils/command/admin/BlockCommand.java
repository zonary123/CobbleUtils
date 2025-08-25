package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.DataBaseFactory;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * @author Carlos Varas Alonso - 07/11/2024 4:18
 */
public class BlockCommand {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .then(
          CommandManager.literal("block")
            .then(CommandManager.literal("remove")
              .then(
                CommandManager.argument("world", StringArgumentType.string())
                  .suggests((context, builder) -> {
                    for (ServerWorld world : CobbleUtils.server.getWorlds()) {
                      builder.suggest(world.getRegistryKey().getValue().toString());
                    }
                    return builder.buildFuture();
                  })
                  .executes(context -> {
                    String worldId = StringArgumentType.getString(context, "world");
                    World world = null;
                    for (ServerWorld w : CobbleUtils.server.getWorlds()) {
                      if (w.getRegistryKey().getValue().toString().equals(worldId)) {
                        world = w;
                        break;
                      }
                    }
                    if (world == null) {
                      context.getSource().sendMessage(
                        Text.literal(
                          "World not found: " + worldId
                        )
                      );
                    } else {
                      DataBaseFactory.dataBaseBlock.deleteWorld(world);
                      context.getSource().sendMessage(
                        Text.literal(
                          "All block data removed for world: " + worldId
                            + " , in database: " + CobbleUtils.config.getDatabase().getType()
                        )
                      );
                    }
                    return 1;
                  })
              )
            )
        )
    );
  }
}
