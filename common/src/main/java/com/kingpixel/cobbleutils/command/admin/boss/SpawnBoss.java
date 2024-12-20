package com.kingpixel.cobbleutils.command.admin.boss;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokemon.effects.IllusionEffect;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.config.BossConfig;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.kingpixel.cobbleutils.util.Utils;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
              .executes(context -> {
                if (!context.getSource().isExecutedByPlayer()) {
                  return 0;
                }
                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                String boss = StringArgumentType.getString(context, "boss");
                spawnBoss(boss, player.getWorld(), player.getPos());
                return 1;
              }).then(
                CommandManager.literal("user")
                  .then(
                    CommandManager.argument("player", EntityArgumentType.player())
                      .executes(context -> {
                        ServerPlayerEntity player = EntityArgumentType.getPlayer(context,
                          "player");
                        Vec3d pos = player.getPos();
                        String boss = StringArgumentType.getString(context, "boss");
                        spawnBoss(boss, player.getWorld(), pos);
                        return 1;
                      })))
              .then(
                CommandManager.literal("coords")
                  .then(
                    CommandManager.argument("pos", Vec3ArgumentType.vec3())
                      .then(
                        CommandManager.argument("world", DimensionArgumentType.dimension())
                          .executes(context -> {
                            Vec3d pos = Vec3ArgumentType.getVec3(context, "pos");
                            String boss = StringArgumentType.getString(context, "boss");
                            World level = DimensionArgumentType.getDimensionArgument(context,
                              "world");
                            spawnBoss(boss, level, pos);
                            return 1;
                          })))))));

  }

  private static void spawnBoss(String boss, World level, Vec3d pos) {
    BossConfig bossConfig = BossConfig.getBossConfigByRarity(boss);


    int n = Utils.RANDOM.nextInt(bossConfig.getPokemons().size());
    PokemonEntity pokemonEntity = PokemonProperties.Companion
      .parse(bossConfig.getPokemons().get(n) + " " + bossConfig.getPokemonData() + " uncatchable=yes")
      .create().sendOut(level.getServer().getOverworld(), pos, new IllusionEffect(), e -> Unit.INSTANCE);

    bossConfig.apply(pokemonEntity);
    level.spawnEntity(pokemonEntity);
  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 0;
  }

}