package com.kingpixel.cobbleutils.command.admin.boss;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.command.argument.PokemonPropertiesArgumentType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.config.BossConfig;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kotlin.Unit;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 03/08/2024 5:29
 */
public class SpawnBoss implements Command<ServerCommandSource> {

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    if (!CobbleUtils.config.isBoss()) return;
    dispatcher.register(
      base.then(
        CommandManager.literal("spawnboss")
          .requires(source -> LuckPermsUtil.checkPermission(source, 4, List.of("cobbleutils.spawnboss", "cobbleutils.admin")))
          .then(
            CommandManager.argument("boss", StringArgumentType.string())
              .suggests((context, builder) -> {
                BossConfig.typeBoss.forEach((s, bossConfig) -> {
                  if (bossConfig.isActive()) {
                    builder.suggest(s);
                  }
                });
                return builder.buildFuture();
              })
              .then(
                CommandManager.literal("user")
                  .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                      .then(
                        CommandManager.argument("pokemon", PokemonPropertiesArgumentType.Companion.properties())
                          .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            Vec3d pos = player.getPos();
                            PokemonProperties pokemonProperties =
                              PokemonPropertiesArgumentType.Companion.getPokemonProperties(context, "pokemon");
                            String boss = StringArgumentType.getString(context, "boss");
                            spawnBoss(boss, pokemonProperties, player.getServerWorld(), pos);
                            return 1;
                          })
                      )
                  )
              )
              .then(
                CommandManager.literal("coords")
                  .then(
                    CommandManager.argument("pos", Vec3ArgumentType.vec3())
                      .then(
                        CommandManager.argument("world", DimensionArgumentType.dimension())
                          .then(
                            CommandManager.argument("pokemon", PokemonPropertiesArgumentType.Companion.properties())
                              .executes(context -> {
                                Vec3d pos = Vec3ArgumentType.getVec3(context, "pos");
                                String boss = StringArgumentType.getString(context, "boss");
                                ServerWorld level = DimensionArgumentType.getDimensionArgument(context,
                                  "world");
                                spawnBoss(boss, PokemonPropertiesArgumentType.Companion.getPokemonProperties(context, "pokemon"), level, pos);
                                return 1;
                              })
                          )
                      )
                  )
              )
          )
      )
    );

  }

  private static void spawnBoss(String boss, PokemonProperties pokemonProperties, ServerWorld level, Vec3d pos) {
    try {
      BossConfig bossConfig = BossConfig.getBossConfigByRarity(boss);

      PokemonEntity pokemonEntity = pokemonProperties
        .create().sendOut(level, pos, null, e -> Unit.INSTANCE);

      bossConfig.apply(pokemonEntity);
      level.spawnEntity(pokemonEntity);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 0;
  }

}