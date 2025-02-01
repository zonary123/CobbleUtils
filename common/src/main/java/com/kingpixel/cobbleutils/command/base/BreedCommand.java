package com.kingpixel.cobbleutils.command.base;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.breeding.models.EggData;
import com.kingpixel.cobbleutils.features.breeding.ui.PlotBreedingUI;
import com.kingpixel.cobbleutils.features.breeding.ui.PlotSelectPokemonUI;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.LuckPermsUtil;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class BreedCommand implements Command<ServerCommandSource> {
  private static final Map<UUID, Long> cooldowns = new HashMap<>();

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.executes(BreedCommand::executeDefault)
        .then(CommandManager.literal("other")
          .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.daycare_breedother",
            "cobbleutils.admin")))
          .then(CommandManager.argument("player", EntityArgumentType.players())
            .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.daycare_breedother",
              "cobbleutils.admin")))
            .executes(BreedCommand::executeForOtherPlayer))
        )
        .then(CommandManager.argument("male", PartySlotArgumentType.Companion.partySlot())
          .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.daycare_breedpokemons",
            "cobbleutils.admin")))
          .then(CommandManager.argument("female", PartySlotArgumentType.Companion.partySlot())
            .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.daycare_breedpokemons"
              , "cobbleutils.admin")))
            .executes(BreedCommand::executeBreeding)
            .then(CommandManager.argument("player", EntityArgumentType.players())
              .requires(source -> LuckPermsUtil.checkPermission(source, 2, List.of("cobbleutils.daycare_breedpokemons_other", "cobbleutils.admin")))
              .executes(BreedCommand::executeBreedingForOtherPlayer))))
    );
  }

  private static int executeDefault(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
    PlotBreedingUI.open(player);
    return 1;
  }

  private static int executeForOtherPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");

    PlotBreedingUI.open(player);
    return 1;
  }

  private static int executeBreeding(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
    return processBreeding(context, player, player);
  }

  private static int executeBreedingForOtherPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
    ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
    return processBreeding(context, executor, targetPlayer);
  }

  private static int processBreeding(CommandContext<ServerCommandSource> context, ServerPlayerEntity executor, ServerPlayerEntity targetPlayer) {
    if (isCooldownActive(targetPlayer)) return 0;

    Pokemon male = PartySlotArgumentType.Companion.getPokemon(context, "male");
    Pokemon female = PartySlotArgumentType.Companion.getPokemon(context, "female");

    if (!arePokemonsValid(male, female, targetPlayer)) {
      return 0;
    }

    Pokemon egg = createEgg(male, female, targetPlayer);

    if (egg != null) {
      addEggToParty(targetPlayer, egg);
      applyCooldown(targetPlayer);
      return 1;
    } else {
      targetPlayer.sendMessage(AdventureTranslator.toNative("Failed to create egg."));
    }

    return 0;
  }

  private static boolean isCooldownActive(ServerPlayerEntity player) {
    Long cooldown = cooldowns.get(player.getUuid());
    if (PlayerUtils.isCooldown(cooldown)) {
      player.sendMessage(AdventureTranslator.toNative(CobbleUtils.language.getMessageCooldown()
        .replace("%prefix%", CobbleUtils.breedconfig.getPrefix())
        .replace("%cooldown%", PlayerUtils.getCooldown(new Date(cooldown)))));
      return true;
    }
    return false;
  }

  private static boolean arePokemonsValid(Pokemon male, Pokemon female, ServerPlayerEntity player) {
    if (male == null || female == null || male.getUuid().equals(female.getUuid())) {
      PlayerUtils.sendMessage(
        player,
        PokemonUtils.replace(
          CobbleUtils.breedconfig.getNotCompatible()
            .replace("%prefix%", CobbleUtils.breedconfig.getPrefix()),
          List.of(male, female)),
        CobbleUtils.breedconfig.getPrefix()
      );
      return false;
    }

    try {
      if (!PlotSelectPokemonUI.arePokemonsCompatible(male, female, player, true)) {
        return false;
      }
    } catch (Exception e) {
      PlayerUtils.sendMessage(player, "Error verifying Pokémon compatibility.", CobbleUtils.breedconfig.getPrefix());
      return false;
    }
    return true;
  }

  private static Pokemon createEgg(Pokemon male, Pokemon female, ServerPlayerEntity player) {
    try {
      if (male.getGender() == Gender.FEMALE) return null;
      if (female.getGender() == Gender.MALE) return null;

      return EggData.createEgg(male, female, player);
    } catch (NoPokemonStoreException e) {
      PlayerUtils.sendMessage(player, "Failed to create egg: no available storage.", CobbleUtils.breedconfig.getPrefix());
      return null;
    }
  }

  private static void addEggToParty(ServerPlayerEntity player, Pokemon egg) {
    Cobblemon.INSTANCE.getStorage().getParty(player).add(egg);
  }

  private static void applyCooldown(ServerPlayerEntity player) {
    long cooldownTime = LuckPermsUtil.hasOp(player) ?
      1L :
      TimeUnit.SECONDS.toMillis(CobbleUtils.breedconfig.getCooldowninstaBreedInSeconds());

    cooldowns.put(player.getUuid(), new Date().getTime() + cooldownTime);
  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 0;
  }
}
