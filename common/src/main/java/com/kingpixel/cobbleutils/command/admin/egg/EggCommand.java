package com.kingpixel.cobbleutils.command.admin.egg;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.command.argument.PokemonPropertiesArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 23/07/2024 22:18
 */
public class EggCommand implements Command<ServerCommandSource> {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.then(
        CommandManager.literal("egg")
          .requires(source ->
            LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.egg.create", "cobbleutils.admin")))
          .then(
            CommandManager.argument("player", EntityArgumentType.player())
              .then(
                CommandManager.argument("pokemon", PokemonPropertiesArgumentType.Companion.properties())
                  .executes(context -> {
                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                    Pokemon pokemon = PokemonPropertiesArgumentType.Companion.getPokemonProperties(context, "pokemon").create();
                    handleEgg(player, pokemon);
                    return 1;
                  })
              )
          )
      )
    );

  }

  private static void handleEgg(ServerPlayerEntity player, Pokemon pokemon) {
    Species species = pokemon.getSpecies();
    Pokemon egg = PokemonProperties.Companion.parse("egg type_egg=" + pokemon.showdownId()).create();

    egg.setShiny(pokemon.getShiny());

    egg.createPokemonProperties(List.of(
      PokemonPropertyExtractor.IVS,
      PokemonPropertyExtractor.GENDER)).apply(egg);

    egg.getPersistentData().putString("species", species.showdownId());
    egg.getPersistentData().putString("nature", pokemon.getNature().getName().getPath());
    egg.getPersistentData().putString("ability", pokemon.getAbility().getName());
    egg.getPersistentData().putString("form",
      pokemon.getForm().getAspects().isEmpty() ? "" : pokemon.getForm().getAspects().get(0));
    egg.getPersistentData().putInt("level", 1);
    egg.getPersistentData().putInt("steps", CobbleUtils.breedconfig.getSteps());
    egg.getPersistentData().putInt("cycles", pokemon.getSpecies().getEggCycles());
    egg.setNickname(Text.literal("Egg " + pokemon.getSpecies().getTranslatedName().getString()));
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Egg create: " + egg.getPersistentData());
    }
    egg.getPersistentData().putInt("HP", pokemon.getIvs().getOrDefault(Stats.HP));
    egg.getPersistentData().putInt("Attack", pokemon.getIvs().getOrDefault(Stats.ATTACK));
    egg.getPersistentData().putInt("Defense", pokemon.getIvs().getOrDefault(Stats.DEFENCE));
    egg.getPersistentData().putInt("SpecialAttack", pokemon.getIvs().getOrDefault(Stats.SPECIAL_ATTACK));
    egg.getPersistentData().putInt("SpecialDefense", pokemon.getIvs().getOrDefault(Stats.SPECIAL_DEFENCE));
    egg.getPersistentData().putInt("Speed", pokemon.getIvs().getOrDefault(Stats.SPEED));
    Cobblemon.INSTANCE.getStorage().getParty(player).add(egg);
  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {

    return 1;
  }
}