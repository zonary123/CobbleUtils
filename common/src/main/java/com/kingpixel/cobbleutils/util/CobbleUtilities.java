package com.kingpixel.cobbleutils.util;

import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 10/06/2024 21:09
 */
public class CobbleUtilities {

  public static String getNameItem(String id) {
    ItemStack itemStack = Utils.parseItemId(id);
    Item item = itemStack.getItem();
    return item.getName(itemStack).getString();
  }

  /**
   * Check if a player is in a battle
   *
   * @param player The player to check
   *
   * @return If the player is in a battle
   */
  public static boolean isBattle(ServerPlayerEntity player) {
    PokemonBattle battle;
    try {
      battle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player);
    } catch (Exception e) {
      player.sendMessage(AdventureTranslator.toNative(CobbleUtils.language.getMessagearebattle()));
      return false;
    }
    assert battle != null;
    boolean pvn = battle.isPvN();
    boolean pvp = battle.isPvP();
    boolean pvw = battle.isPvW();
    return pvn || pvp || pvw;
  }

  /**
   * Give a random item to a player
   *
   * @param player The player to give the item
   * @param type   The type of item to give
   * @param amount The amount of items to give
   * @param random If the item should be different
   */
  public static void giveRandomItem(ServerPlayerEntity player, String type, int amount, boolean random) {
    try {
      ItemChance item;

      if (random) {
        for (int i = 0; i < amount; i++) {
          ItemChance.giveReward(player, CobbleUtils.poolItems.getRandomItem(type), 1);
        }
      } else {
        item = CobbleUtils.poolItems.getRandomItem(type);
        ItemChance.giveReward(player, item, amount);

        String message = CobbleUtils.language.getMessagerandomitem()
          .replace("%item%", item.getTitle())
          .replace("%amount%", String.valueOf(amount))
          .replace("%type%", type);


        PlayerUtils.sendMessage(player, message, CobbleUtils.config.getPrefix());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Give a random pokemon to a player
   *
   * @param player The player to give the pokemon
   * @param type   The type of pokemon to give
   *
   * @throws NoPokemonStoreException If the pokemon store is empty
   */
  public static void giveRandomPokemon(ServerPlayerEntity player, String type) throws NoPokemonStoreException {
    String pokemonName = CobbleUtils.poolPokemons.getRandomPokemon(type);
    if (pokemonName == null) {
      player.sendMessage(AdventureTranslator.toNative("Invalid type."));
      return;
    }
    Pokemon pokemon = Utils.createPokemonParse(pokemonName);
    Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemon);
  }

  /**
   * Convert seconds to time
   *
   * @param totalSeconds The total seconds
   *
   * @return The time in a string
   */
  public static String convertSecondsToTime(int totalSeconds) {
    long seconds = totalSeconds;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    long days = hours / 24;

    long remainingHours = hours % 24;
    long remainingMinutes = minutes % 60;
    long remainingSeconds = seconds % 60;

    StringBuilder result = new StringBuilder();

    if (days > 0) {
      String dayString = days != 1 ? CobbleUtils.language.getDays().replace("%s", String.valueOf(days))
        : CobbleUtils.language.getDay().replace("%s", String.valueOf(days));
      result.append(dayString);
    }
    if (remainingHours > 0) {
      String hourString = remainingHours != 1 ? CobbleUtils.language.getHours().replace("%s",
        String.valueOf(remainingHours))
        : CobbleUtils.language.getHour().replace("%s", String.valueOf(remainingHours));
      result.append(hourString);
    }
    if (remainingMinutes > 0) {
      String minuteString = remainingMinutes != 1 ? CobbleUtils.language.getMinutes().replace("%s",
        String.valueOf(remainingMinutes))
        : CobbleUtils.language.getMinute().replace("%s",
        String.valueOf(remainingMinutes));
      result.append(minuteString);
    }
    if (remainingSeconds > 0) {
      String secondString = remainingSeconds != 1 ? CobbleUtils.language.getSeconds().replace("%s",
        String.valueOf(remainingSeconds))
        : CobbleUtils.language.getSecond().replace("%s",
        String.valueOf(remainingSeconds));
      result.append(secondString);
    }
    if (result.isEmpty())
      return CobbleUtils.language.getNocooldown();

    return result.toString().trim();
  }

  public static GooeyButton fillItem() {
    return GooeyButton.builder().display(Utils.parseItemId(CobbleUtils.config.getFill())).build();
  }

  /**
   * Give a random amount of money to a player
   *
   * @param player The player to give the money
   * @param type   The type of money to give
   *
   * @return If the money was given successfully
   */
  public static boolean giveRandomMoney(ServerPlayerEntity player, String type) {
    return CobbleUtils.poolMoney.getRandomMoney(player, type);
  }

  /**
   * Execute a command
   *
   * @param command The command to execute
   *
   * @return If the command was executed successfully
   */
  public static boolean executeCommand(String command) {
    CommandDispatcher<ServerCommandSource> disparador = CobbleUtils.server.getCommandManager().getDispatcher();
    try {
      ServerCommandSource serverSource = CobbleUtils.server.getCommandSource();
      ParseResults<ServerCommandSource> parse = disparador.parse(command, serverSource);
      disparador.execute(parse);
      return true;
    } catch (CommandSyntaxException e) {
      System.err.println("Error to execute command: " + command);
      e.printStackTrace();
      return false;
    }
  }


  /**
   * Get an ItemStack from a string with NBT
   *
   * @param item The item with NBT
   *
   * @return The ItemStack
   */
  public static ItemStack getItem(String item) {
    try {
      ItemStack itemStack = ItemStack.EMPTY;
      NbtComponent nbtComponent = NbtComponent.of(NbtHelper.fromNbtProviderString(item));
      itemStack.set(DataComponentTypes.CUSTOM_DATA, nbtComponent);
      return itemStack;
    } catch (Exception e) {
      CobbleUtils.LOGGER.fatal("Failed to parse item for NBT: " + item);
      CobbleUtils.LOGGER.fatal("Stacktrace: ");
    }
    /*try {
      return ItemStack.fromNbt(NbtHelper.fromNbtProviderString(item));
    } catch (CommandSyntaxException e) {
      CobbleUtils.LOGGER.fatal("Failed to parse item for NBT: " + item);
      CobbleUtils.LOGGER.fatal("Stacktrace: ");
      e.printStackTrace();
    }*/
    return ItemStack.EMPTY;
  }
}
