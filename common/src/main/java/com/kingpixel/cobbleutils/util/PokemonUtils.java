package com.kingpixel.cobbleutils.util;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.*;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.*;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbilityType;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Carlos Varas Alonso - 28/06/2024 18:53
 */
public class PokemonUtils {
  /**
   * Replace the placeholders with the pokemon data
   *
   * @param lore    The lore to replace
   * @param pokemon The pokemon to get the data
   *
   * @return The lore with the replaced placeholders
   */
  public static List<String> replace(List<String> lore, Pokemon pokemon) {
    List<String> finalLore = new ArrayList<>();
    for (String s : lore) {
      if (s.contains("%lorepokemon%")) {
        for (String additionalLine : CobbleUtils.language.getLorepokemon()) {
          replace(pokemon, finalLore, additionalLine);
        }
      } else {
        replace(pokemon, finalLore, s);
      }
    }
    return finalLore;
  }

  private static void replace(Pokemon pokemon, List<String> finalLore, String s) {
    String replaced = replace(s, pokemon);
    for (int i = 0; i < 4; i++) {
      if (pokemon == null) {
        replaced = replaced.replace("%move" + (i + 1) + "%", CobbleUtils.language.getUnknown());
      } else {
        replaced = replaced.replace("%move" + (i + 1) + "%", getMoveTranslate(pokemon.getMoveSet().get(i)));
      }
    }
    replaced = replaced.replace("%lorepokemon%", "");
    finalLore.add(replaced);
  }

  /**
   * Get the showdown id of the pokemon
   *
   * @param pokemon The pokemon to get the id
   *
   * @return The showdown id of the pokemon
   */
  public static String getIdentifierPokemon(Pokemon pokemon) {
    return pokemon.getSpecies().showdownId();
  }

  /**
   * Replace the placeholders with the pokemon data
   *
   * @param pokemon The pokemon to get the data
   *
   * @return The lore with the replaced placeholders
   */
  public static List<String> replaceLore(Pokemon pokemon) {
    return replace(CobbleUtils.language.getLorepokemon(), pokemon);
  }

  /**
   * Replace the placeholders with the pokemon data
   *
   * @param pokemon The pokemon to get the data
   *
   * @return The string with the replaced placeholders
   */
  public static String replace(Pokemon pokemon) {
    return replace(CobbleUtils.language.getPokemonnameformat(), pokemon);
  }

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[^%]+%");

  private static String replacePlaceholders(String message, Pokemon pokemon, Integer index) {
    if (message == null || message.isEmpty()) return "";
    if (!message.contains("%")) return message;

    String indexStr = (index == null || index == 0) ? "" : index.toString();

    if (pokemon == null) {
      return PLACEHOLDER_PATTERN.matcher(message)
        .replaceAll(Matcher.quoteReplacement(CobbleUtils.language.getUnknown()));
    }

    // Calcular 'ah' solo si se necesita
    String ah = "";
    if (message.contains("%ah")) {
      if (isEgg(pokemon)) {
        Pokemon p = PokemonProperties.Companion.parse(pokemon.getSpecies().showdownId()).create();
        String ability = pokemon.getPersistentData().getString("ability");
        p.updateAbility(!ability.isEmpty()
          ? Abilities.INSTANCE.get(ability).create(false, Priority.LOWEST)
          : getRandomAbility(p));
        ah = isAH(p) ? CobbleUtils.language.getAH() : "";
      } else {
        ah = isAH(pokemon) ? CobbleUtils.language.getAH() : "";
      }
    }

    Map<String, String> placeholders = buildPlaceholders(pokemon, indexStr, ah);

    // Reemplazar usando Matcher + StringBuffer
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String placeholder = matcher.group();
      String replacement = placeholders.getOrDefault(placeholder, placeholder); // si no hay reemplazo, dejar igual
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private static Map<String, String> buildPlaceholders(Pokemon pokemon, String indexStr, String ah) {
    Nature nature = pokemon.getNature();

    Map<String, String> map = new HashMap<>();
    map.put("%showdownid" + indexStr + "%", pokemon.showdownId());
    map.put("%level" + indexStr + "%", String.valueOf(pokemon.getLevel()));
    map.put("%nature" + indexStr + "%", getNatureTranslate(nature));
    map.put("%pokemon" + indexStr + "%", isEgg(pokemon)
      ? pokemon.getPersistentData().getString("species")
      : getName(pokemon));
    map.put("%shiny" + indexStr + "%", pokemon.getShiny() ? CobbleUtils.language.getSymbolshiny() : "");
    map.put("%ability" + indexStr + "%", isEgg(pokemon)
      ? pokemon.getPersistentData().getString("ability")
      : getAbilityTranslate(pokemon.getAbility()));
    map.put("%tradeable" + indexStr + "%", pokemon.getTradeable() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());

    // IVs
    map.put("%ivshp" + indexStr + "%", String.valueOf(pokemon.getIvs().get(Stats.HP)));
    map.put("%ivsatk" + indexStr + "%", String.valueOf(pokemon.getIvs().get(Stats.ATTACK)));
    map.put("%ivsdef" + indexStr + "%", String.valueOf(pokemon.getIvs().get(Stats.DEFENCE)));
    map.put("%ivsspa" + indexStr + "%", String.valueOf(pokemon.getIvs().get(Stats.SPECIAL_ATTACK)));
    map.put("%ivsspdef" + indexStr + "%", String.valueOf(pokemon.getIvs().get(Stats.SPECIAL_DEFENCE)));
    map.put("%ivsspeed" + indexStr + "%", String.valueOf(pokemon.getIvs().get(Stats.SPEED)));

    // EVs
    map.put("%evshp" + indexStr + "%", String.valueOf(pokemon.getEvs().get(Stats.HP)));
    map.put("%evsatk" + indexStr + "%", String.valueOf(pokemon.getEvs().get(Stats.ATTACK)));
    map.put("%evsdef" + indexStr + "%", String.valueOf(pokemon.getEvs().get(Stats.DEFENCE)));
    map.put("%evsspa" + indexStr + "%", String.valueOf(pokemon.getEvs().get(Stats.SPECIAL_ATTACK)));
    map.put("%evsspdef" + indexStr + "%", String.valueOf(pokemon.getEvs().get(Stats.SPECIAL_DEFENCE)));
    map.put("%evsspeed" + indexStr + "%", String.valueOf(pokemon.getEvs().get(Stats.SPEED)));

    map.put("%item" + indexStr + "%", ItemUtils.getTranslatedName(pokemon.heldItem()));
    map.put("%size" + indexStr + "%", getSize(pokemon));
    map.put("%form" + indexStr + "%", getForm(pokemon));
    map.put("%up" + indexStr + "%", getStatTranslate(nature.getIncreasedStat()));
    map.put("%down" + indexStr + "%", getStatTranslate(nature.getDecreasedStat()));
    map.put("%ball" + indexStr + "%", getPokeBallTranslate(pokemon.getCaughtBall()));
    map.put("%gender" + indexStr + "%", getGenderTranslate(pokemon.getGender()));
    map.put("%ivs" + indexStr + "%", getIvsAverage(pokemon.getIvs()).toString());
    map.put("%evs" + indexStr + "%", getEvsTotal(pokemon.getEvs()).toString());

    // Moves con seguridad por si no hay suficientes
    List<Move> moves = pokemon.getMoveSet().getMoves();
    for (int i = 0; i < 4; i++) {
      String key = "%move" + indexStr + (i + 1) + "%";
      String value = i < moves.size() ? getMoveTranslate(moves.get(i)) : "";
      map.put(key, value);
    }

    map.put("%owner" + indexStr + "%", getOwnerName(pokemon));
    map.put("%types" + indexStr + "%", getType(pokemon));
    map.put("%rarity" + indexStr + "%", getRarityS(pokemon));
    map.put("%breedable" + indexStr + "%", isBreedable(pokemon) ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
    map.put("%friendship" + indexStr + "%", String.valueOf(pokemon.getFriendship()));
    map.put("%ah" + indexStr + "%", ah);

    String country = pokemon.getPersistentData().getString(CobbleUtilsTags.COUNTRY_TAG);
    map.put("%country" + indexStr + "%", country.isEmpty() ? CobbleUtils.language.getNone() : country);

    map.put("%egggroups" + indexStr + "%", eggGroups(pokemon));
    map.put("%dex" + indexStr + "%", String.valueOf(pokemon.getSpecies().getNationalPokedexNumber()));
    map.put("%labels" + indexStr + "%", pokemon.getForm().getLabels().toString());
    map.put("%aspects" + indexStr + "%", pokemon.getAspects().stream().toList().toString());

    return map;
  }

  private static String getForm(Pokemon pokemon) {
    if (pokemon == null) return CobbleUtils.language.getUnknown();
    String aspect;
    List<String> aspects = pokemon.getAspects().stream().toList();

    if (aspects.isEmpty()) {
      aspect = "Normal";
    } else {
      String s = aspects.get(aspects.size() - 1);
      if (s.contains("-")) {
        String[] e = s.split("-");
        switch (e.length) {
          case 1 -> s = e[0];
          case 2 -> s = e[1];
          case 3 -> s = e[2];
          default -> s = "Normal";
        }
      }
      if (s.equalsIgnoreCase("male")
        || s.equalsIgnoreCase("female")
        || s.equalsIgnoreCase("genderless")
        || s.equalsIgnoreCase("milkable")
        || s.equalsIgnoreCase("family")) {
        aspect = "Normal";
      } else {
        aspect = s;
      }
    }
    return isEgg(pokemon)
      ? (pokemon.getPersistentData().getString("form").isEmpty()
      ? "Normal" : pokemon.getPersistentData().getString("form")) : pokemon.getForm().getName().equalsIgnoreCase("normal")
      ? aspect
      : CobbleUtils.language.getForms().getOrDefault(pokemon.getForm().getName(), pokemon.getForm().getName());
  }

  /**
   * Replace the placeholders with the pokemon data
   *
   * @param message The string to replace
   * @param pokemon The pokemon to get the data
   *
   * @return The string with the replaced placeholders
   */
  public static String replace(String message, Pokemon pokemon) {
    if (message == null || message.isEmpty()) return "";
    if (!message.contains("%")) return message;
    if (message.contains("%lorepokemon%")) {
      StringBuilder loreStringBuilder = new StringBuilder();
      CobbleUtils.language.getLorepokemon().forEach(lore -> loreStringBuilder.append(lore).append("\n"));

      String lorepokemon = loreStringBuilder.toString();
      message = message.replace("%lorepokemon%", lorepokemon);
    }

    return replacePlaceholders(message, pokemon, null); // null indica que no hay índice
  }

  public static boolean isEgg(Pokemon pokemon) {
    return pokemon.getSpecies().showdownId().equalsIgnoreCase("egg");
  }

  public static String eggGroups(Pokemon pokemon) {
    StringBuilder s = new StringBuilder();
    for (EggGroup eggGroup : pokemon.getSpecies().getEggGroups()) {
      s.append("&e").append(eggGroup).append(" ");
    }
    return s.toString();
  }

  /**
   * Check if the pokemon has pokerus
   *
   * @param pokemon The pokemon to check
   *
   * @return If the pokemon has pokerus
   */
  public static boolean isPokerus(Pokemon pokemon) {
    return pokemon.getPersistentData().getBoolean("pokerus");
  }


  /**
   * Replace the placeholders with the pokemon data
   *
   * @param message  The string to replace
   * @param pokemons The pokemon to get the data
   *
   * @return The string with the replaced placeholders
   */
  public static String replace(String message, List<Pokemon> pokemons) {
    if (pokemons.isEmpty()) {
      return message;
    }
    int size = pokemons.size();
    for (int i = 0; i < size; i++) {
      Pokemon pokemon = pokemons.get(i);
      message = replacePlaceholders(message, pokemon, size == 1 ? null : i + 1);
    }

    return message;
  }

  public static Pokemon getFirstEvolution(Pokemon pokemon) {
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("getFirstEvolution(Pokemon pokemon) -> " + pokemon.showdownId());
    }
    Pokemon firstEvolution = pokemon;
    while (firstEvolution.getPreEvolution() != null) {
      firstEvolution = firstEvolution.getPreEvolution().getSpecies().create(1);
      firstEvolution.setForm(firstEvolution.getForm());
      firstEvolution.updateForm();
    }
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("getFirstEvolution() First evolution: " + firstEvolution.showdownId());
    }
    return firstEvolution;
  }

  public static Pokemon getEvolutionPokemonEgg(Pokemon pokemon) {
    return getEvolutionPokemonEgg(pokemon.getSpecies());
  }

  public static Pokemon getEvolutionPokemonEgg(Species species) {
    if (species.showdownId().equals("manaphy"))
      return PokemonSpecies.INSTANCE.getByIdentifier(Identifier.of("cobblemon:phione")).create(1);
    Species firstEvolution = getFirstPreEvolution(species);
    Pokemon specialPokemon = firstEvolution.create(1);

    // Usamos Objects.requireNonNullElseGet para devolver el Pokémon especial si existe, o crear uno nuevo si no
    return Objects.requireNonNullElseGet(specialPokemon, () -> firstEvolution.create(1));
  }

  private static Species getFirstPreEvolution(Species species) {
    while (species.getPreEvolution() != null) {
      Species preEvolution = species.getPreEvolution().getSpecies();

      // Si encontramos un bucle en la cadena evolutiva, rompemos el ciclo
      if (preEvolution.showdownId().equalsIgnoreCase(species.showdownId())) {
        break;
      }

      species = preEvolution;
    }

    return species;
  }


  /**
   *
   */
  public static String getName(Pokemon pokemon) {
    return pokemon.getSpecies().getName();


  }

  /**
   * Check if the pokemon is breedable
   *
   * @param pokemon The pokemon to check
   *
   * @return If the pokemon is breedable
   */
  public static boolean isBreedable(Pokemon pokemon) {
    if (pokemon.getPersistentData() != null && pokemon.getPersistentData().contains("breedable") &&
      !pokemon.getPersistentData().getBoolean("breedable")) {
      return pokemon.getPersistentData().getBoolean("breedable");
    } else {
      return true;
    }
  }

  /**
   * Get the owner name of the pokemon
   *
   * @param pokemon The pokemon to get the owner name
   *
   * @return The owner name of the pokemon
   */
  public static String getOwnerName(Pokemon pokemon) {
    String owner = pokemon.getOriginalTrainerName();
    if (owner == null)
      owner = CobbleUtils.language.getNone();
    return owner;
  }

  /**
   * Get the size of the pokemon
   *
   * @param pokemon The pokemon to get the size
   *
   * @return The size of the pokemon
   */
  public static String getSize(Pokemon pokemon) {
    return getSizeName(pokemon);
  }

  /**
   * Get the total of the IVs
   *
   * @param iVs The IVs to get the total
   *
   * @return The total of the IVs
   */
  public static Integer getIvsTotal(IVs iVs) {
    Integer sum = 0;
    for (Map.Entry<? extends Stat, ? extends Integer> iV : iVs) {
      sum += iV.getValue();
    }
    return sum;
  }

  /**
   * Get the average of the IVs
   *
   * @param iVs The IVs to get the average
   *
   * @return The average of the IVs
   */
  public static Integer getIvsAverage(IVs iVs) {
    if (iVs == null) return 0;
    Integer sum = 0;
    for (Map.Entry<? extends Stat, ? extends Integer> iV : iVs) {
      sum += iV.getValue();
    }
    return sum / 6;
  }

  /**
   * Get the total of the EVs
   *
   * @param eVs The EVs to get the total
   *
   * @return The total of the EVs
   */
  public static Integer getEvsTotal(EVs eVs) {
    if (eVs == null) return 0;
    Integer sum = 0;
    for (Map.Entry<? extends Stat, ? extends Integer> eV : eVs) {
      sum += eV.getValue();
    }
    return sum;
  }

  /**
   * Get the average of the EVs
   *
   * @param eVs The EVs to get the average
   *
   * @return The average of the EVs
   */
  public static Integer getEvsAverage(EVs eVs) {
    Integer sum = 0;
    for (Map.Entry<? extends Stat, ? extends Integer> eV : eVs) {
      sum += eV.getValue();
    }
    return sum / 6;
  }

  /**
   * Get the ability translation
   *
   * @param ability The ability to translate
   *
   * @return The ability translation
   */
  public static String getAbilityTranslate(Ability ability) {
    if (ability == null) return CobbleUtils.language.getNone();

    return "<lang:cobblemon.ability." + ability.getName() + ">";

  }

  /**
   * Get the nature translation
   *
   * @param nature The nature to translate
   *
   * @return The nature translation
   */
  public static String getNatureTranslate(Nature nature) {
    if (nature == null) return CobbleUtils.language.getNone();

    return "<lang:cobblemon.nature." + nature.getName().getPath() + ">";

  }

  public static String getMoveColor(ElementalType type, String lang) {
    if (type == null) return CobbleUtils.language.getNone();

    String color = CobbleUtils.language.getMovecolor().getOrDefault(type.getName(), "");
    if (color.contains("gradient"))
      return color + "<lang:" + lang + ">" + "</gradient>";
    return color + "<lang:" + lang + ">";
  }

  public static String getType(ElementalType type) {
    if (type == null) return CobbleUtils.language.getNone();
    return CobbleUtils.language.getTypes().getOrDefault(type.getName(), type.getName());
  }

  /**
   * Get the type of the pokemon
   *
   * @param pokemon The pokemon to get the type
   *
   * @return The type of the pokemon
   */
  public static String getType(Pokemon pokemon) {

    StringBuilder s = new StringBuilder(CobbleUtils.language.getTypes().getOrDefault(pokemon.getPrimaryType().getName(),
      pokemon.getPrimaryType().getName()));
    if (pokemon.getSecondaryType() != null) {
      s.append(" &7/ ");
      s.append(CobbleUtils.language.getTypes().getOrDefault(pokemon.getSecondaryType().getName(),
        pokemon.getSecondaryType().getName()));
    }
    return s.toString();
  }

  /**
   * Get the move translation
   *
   * @param move The move to translate
   *
   * @return The move translation
   */
  public static String getMoveTranslate(Move move) {
    if (move == null)
      return CobbleUtils.language.getNone();
    return getMoveColor(move.getType(), "cobblemon.move." + move.getName());
  }

  /**
   * Get the stat translation
   *
   * @param stat The stat to translate
   *
   * @return The stat translation
   */
  public static String getStatTranslate(Stat stat) {
    if (stat == null) {
      return "";
    }


    switch (stat.getIdentifier().toTranslationKey()) {
      case "cobblemon.hp":
        return "<lang:cobblemon.ui.stats.hp>";
      case "cobblemon.attack":
        return "<lang:cobblemon.ui.stats.atk>";
      case "cobblemon.defence":
        return "<lang:cobblemon.ui.stats.def>";
      case "cobblemon.special_attack":
        return "<lang:cobblemon.ui.stats.sp_atk>";
      case "cobblemon.special_defence":
        return "<lang:cobblemon.ui.stats.sp_def>";
      case "cobblemon.speed":
        return "<lang:cobblemon.ui.stats.speed>";
      default:
        return "";
    }
  }

  /**
   * Get the pokeball translation
   *
   * @param caughtBall The pokeball to translate
   *
   * @return The pokeball translation
   */
  public static String getPokeBallTranslate(PokeBall caughtBall) {
    if (caughtBall == null) return CobbleUtils.language.getNone();
    return caughtBall == null ? CobbleUtils.language.getNone()
      : Text.translatable("item.cobblemon." + caughtBall.getName().getPath()).getString();
  }

  /**
   * Get the gender translation
   *
   * @param gender The gender to translate
   *
   * @return The gender translation
   */
  public static String getGenderTranslate(Gender gender) {
    if (gender == null) return CobbleUtils.language.getNone();
    return CobbleUtils.language.getGender().getOrDefault(gender.getShowdownName(), gender.getShowdownName());
  }

  /**
   * Get the rarity of the pokemon
   *
   * @param pokemon The pokemon to get the rarity
   *
   * @return The rarity of the pokemon
   */
  public static double getRarity(Pokemon pokemon) {
    return CobbleUtils.spawnRates.getRarity(pokemon);
  }

  /**
   * Get the rarity of the pokemon
   *
   * @param pokemon The pokemon to get the rarity
   *
   * @return The rarity of the pokemon
   */
  public static String getRarityS(Pokemon pokemon) {
    double rarity = getRarity(pokemon);
    if (rarity == -1) return CobbleUtils.language.getUnknown();

    Map<String, Double> rarityMap = CobbleUtils.config.getRarity();

    String rarityResult = "Unknown";
    double closestValue = Double.MAX_VALUE;

    for (Map.Entry<String, Double> entry : rarityMap.entrySet()) {
      double value = entry.getValue();

      if (rarity <= value && value < closestValue) {
        closestValue = value;
        rarityResult = entry.getKey();
      }
    }

    if ("Unknown".equals(rarityResult)) {
      rarityResult = rarityMap.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse("Unknown");
    }

    return rarityResult;
  }


  /**
   * Get the size of the pokemon
   *
   * @param pokemon The pokemon to get the size
   *
   * @return The size of the pokemon
   */
  public static String getSizeName(Pokemon pokemon) {
    String size = pokemon.getPersistentData().getString("size");
    if (size.isEmpty()) return CobbleUtils.language.getUnknown();
    return size;
  }

  /**
   * Get the hidden ability of the pokemon
   *
   * @param pokemon The pokemon to get the hidden ability
   *
   * @return The hidden ability of the pokemon
   */
  public static Ability getAH(Pokemon pokemon) {
    for (PotentialAbility ability : pokemon.getForm().getAbilities()) {
      if (ability.getType() instanceof HiddenAbilityType) {
        return ability.getTemplate().create(false, Priority.LOWEST);
      }
    }
    return getRandomAbility(pokemon);
  }


  /**
   * Check if the pokemon has the hidden ability
   *
   * @param pokemon The pokemon to check
   *
   * @return If the pokemon has the hidden ability
   */
  public static boolean isAH(Pokemon pokemon) {
    int size = 0;
    for (PotentialAbility ability : pokemon.getForm().getAbilities()) {
      size++;
    }
    if (size == 1) return false;
    for (PotentialAbility ability : pokemon.getForm().getAbilities()) {
      if (ability.getType() instanceof HiddenAbilityType) {
        return ability.getTemplate().getName().equalsIgnoreCase(pokemon.getAbility().getTemplate().getName());
      }
    }
    return false;
  }

  /**
   * Check if the pokemon has the hidden ability
   *
   * @param pokemon The pokemon to check
   * @param ability The ability to check
   *
   * @return If the pokemon has the hidden ability
   */
  public static boolean isAH(Pokemon pokemon, AbilityTemplate ability) {
    for (PotentialAbility potentialAbility : pokemon.getForm().getAbilities()) {
      if (potentialAbility.getType() instanceof HiddenAbilityType) {
        if (potentialAbility.getTemplate().create(false, Priority.LOWEST).getName().equalsIgnoreCase(ability.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check if the species has the hidden ability
   *
   * @param pokemon The species to check
   * @param ability The ability to check
   *
   * @return If the species has the hidden ability
   */
  public static boolean isAH(Pokemon pokemon, Ability ability) {
    for (PotentialAbility potentialAbility : pokemon.getForm().getAbilities()) {
      if (potentialAbility.getType() instanceof HiddenAbilityType) {
        if (potentialAbility.getTemplate().create(false, Priority.LOWEST).getName().equalsIgnoreCase(ability.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  public static Ability getRandomAbility(Pokemon pokemon) {
    AbilityPool abilities = pokemon.getForm().getAbilities();
    List<Ability> abilityList = new ArrayList<>();
    for (PotentialAbility potentialAbility : abilities) {
      if (!(potentialAbility.getType() instanceof HiddenAbilityType)) {
        abilityList.add(potentialAbility.getTemplate().create(false, Priority.LOWEST));
      }
    }

    if (abilityList.size() == 1) {
      return abilityList.getFirst();
    }
    return abilityList.get(Utils.RANDOM.nextInt(abilityList.size()));
  }

  public static void isLegalAbility(ServerPlayerEntity player, Pokemon pokemon) {
    boolean legal = isLegalAbility(pokemon);
    try {
      if (pokemon.getAbility().getForced()) {
        pokemon.getAbility().setForced$common(false);
      }
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("Error setting forced ability: " + e.getMessage());
    }
    if (!legal && CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Fix illegal ability: Player: " + player.getGameProfile().getName());
    }
  }

  public static boolean isLegalAbility(Pokemon pokemon) {
    for (PotentialAbility potentialAbility : pokemon.getForm().getAbilities()) {
      if (pokemon.getAbility().getTemplate().getName().equalsIgnoreCase(potentialAbility.getTemplate().getName())) {
        return true;
      }
    }
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Illegal ability: Pokemon: " + getName(pokemon) + "\n Ability: " + getAbilityTranslate(pokemon.getAbility()));
    }
    pokemon.updateAbility(getRandomAbility(pokemon));
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("New ability: Pokemon: " + getName(pokemon) + "\n Ability: " + getAbilityTranslate(pokemon.getAbility()));
    }
    return false;
  }
}
