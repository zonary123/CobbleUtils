package com.kingpixel.cobbleutils.util;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityPool;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    Map<String, String> placeholders = buildPlaceholders(pokemon, null);

    return lore.stream()
      .flatMap(s -> s.contains("%lorepokemon%")
        ? CobbleUtils.language.getLorepokemon().stream()
        .map(additionalLine -> replacePlaceholders(additionalLine, placeholders))
        : Stream.of(replacePlaceholders(s, placeholders)))
      .collect(Collectors.toCollection(ArrayList::new));
  }

  private static void replace(Pokemon pokemon, List<String> finalLore, String s) {
    String replaced = replace(s, pokemon);
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

  private static String replacePlaceholders(String message, Map<String, String> placeholders) {
    if (message == null || !message.contains("%")) return message;
    if (placeholders.isEmpty()) return message;
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      if (!message.contains("%")) break;
      message = message.replace(entry.getKey(), entry.getValue());
    }
    return message;
  }

  private static Map<String, String> buildPlaceholders(Pokemon pokemon, String indexStr) {
    Map<String, String> map = new HashMap<>();
    indexStr = indexStr == null ? "" : indexStr;
    String finalIndexStr = indexStr;
    BiConsumer<String, String> put = (key, value) -> map.put("%" + key + finalIndexStr + "%", value != null ? value : CobbleUtils.language.getUnknown());

    if (pokemon == null) {
      List<String> keys = List.of("showdownid", "level", "nature", "pokemon", "shiny", "ability", "tradeable",
        "ivshp", "ivsatk", "ivsdef", "ivsspa", "ivsspdef", "ivsspeed",
        "evshp", "evsatk", "evsdef", "evsspa", "evsspdef", "evsspeed",
        "item", "size", "form", "up", "down", "ball", "gender", "ivs", "evs", "ivspercent", "evspercent",
        "move1", "move2", "move3", "move4", "owner", "types", "rarity", "breedable",
        "friendship", "ah", "ha", "country", "egggroups", "dex", "labels", "aspects");
      keys.forEach(k -> put.accept(k, CobbleUtils.language.getUnknown()));
      return map;
    }

    Nature nature = pokemon.getNature();
    put.accept("showdownid", pokemon.showdownId());
    put.accept("level", String.valueOf(pokemon.getLevel()));
    put.accept("nature", getNatureTranslate(nature));
    put.accept("pokemon", isEgg(pokemon)
      ? pokemon.getPersistentData().getString("pokemon")
      : getTranslatedName(pokemon));
    put.accept("shiny", pokemon.getShiny() ? CobbleUtils.language.getSymbolshiny() : "");
    put.accept("ability", isEgg(pokemon)
      ? "<lang:cobblemon.ability." + pokemon.getPersistentData().getString("ability") + ">"
      : getAbilityTranslate(pokemon.getAbility()));
    put.accept("tradeable", pokemon.getTradeable() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());

    // IVs y EVs
    for (Stats stat : Stats.values()) {
      String name = "";
      switch (stat) {
        case HP -> name = "hp";
        case ATTACK -> name = "atk";
        case DEFENCE -> name = "def";
        case SPECIAL_ATTACK -> name = "spa";
        case SPECIAL_DEFENCE -> name = "spdef";
        case SPEED -> name = "speed";
      }
      put.accept("ivs" + name, String.valueOf(pokemon.getIvs().get(stat)));
      put.accept("evs" + name, String.valueOf(pokemon.getEvs().get(stat)));
    }

    put.accept("item", ItemUtils.getTranslatedName(pokemon.heldItem()));
    put.accept("size", getSize(pokemon));
    put.accept("form", getForm(pokemon));
    put.accept("up", getStatTranslate(nature.getIncreasedStat()));
    put.accept("down", getStatTranslate(nature.getDecreasedStat()));
    put.accept("ball", getPokeBallTranslate(pokemon.getCaughtBall()));
    put.accept("gender", getGenderTranslate(pokemon.getGender()));
    put.accept("ivs", getIvsAverage(pokemon.getIvs()).toString());
    put.accept("evs", getEvsTotal(pokemon.getEvs()).toString());
    put.accept("ivspercent", String.format("%.2f", getIvsPercent(pokemon.getIvs())));
    put.accept("evspercent", String.format("%.2f", getEvsPercent(pokemon.getEvs())));

    List<Move> moves = pokemon.getMoveSet().getMoves();
    for (int i = 0; i < 4; i++) {
      put.accept("move" + (i + 1), i < moves.size() ? getMoveTranslate(moves.get(i)) : CobbleUtils.language.getNone());
    }

    put.accept("owner", getOwnerName(pokemon));
    put.accept("types", getType(pokemon));
    put.accept("rarity", getRarityS(pokemon));
    put.accept("breedable", isBreedable(pokemon) ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
    put.accept("friendship", String.valueOf(pokemon.getFriendship()));

    StringBuilder ah = new StringBuilder();
    if (isEgg(pokemon)) {
      Pokemon p = PokemonProperties.Companion.parse(pokemon.getSpecies().showdownId()).create();
      String ability = pokemon.getPersistentData().getString("ability");
      p.updateAbility(!ability.isEmpty()
        ? Abilities.INSTANCE.get(ability).create(false, Priority.LOWEST)
        : getRandomAbility(p));
      ah.append(isAH(p) ? CobbleUtils.language.getHA() : "");
    } else {
      ah.append(isAH(pokemon) ? CobbleUtils.language.getHA() : "");
    }
    put.accept("ah", ah.toString());
    put.accept("ha", ah.toString());

    String country = pokemon.getPersistentData().getString(CobbleUtilsTags.COUNTRY_TAG);
    put.accept("country", country.isEmpty() ? CobbleUtils.language.getNone() : country);

    put.accept("egggroups", eggGroups(pokemon));
    put.accept("dex", String.valueOf(pokemon.getSpecies().getNationalPokedexNumber()));
    put.accept("labels", pokemon.getForm().getLabels().toString());
    put.accept("aspects", pokemon.getAspects().stream().toList().toString());

    return map;
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

    Map<String, String> placeholders = buildPlaceholders(pokemon, "");

    if (message.contains("%lorepokemon%")) {
      StringBuilder loreStringBuilder = new StringBuilder();
      CobbleUtils.language.getLorepokemon().forEach(lore -> loreStringBuilder.append(lore).append("\n"));
      message = message.replace("%lorepokemon%", loreStringBuilder.toString());
    }

    return replacePlaceholders(message, placeholders);
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
    if (pokemons.isEmpty()) return message;

    int size = pokemons.size();
    for (int i = 0; i < size; i++) {
      Pokemon pokemon = pokemons.get(i);
      Map<String, String> placeholders = buildPlaceholders(pokemon, String.valueOf(size == 1 ? "" : i + 1));
      message = replacePlaceholders(message, placeholders);
    }
    return message;
  }

  public static boolean isEgg(Pokemon pokemon) {
    return pokemon.getSpecies().showdownId().equals("egg");
  }

  public static String eggGroups(Pokemon pokemon) {
    StringBuilder s = new StringBuilder();
    for (EggGroup eggGroup : pokemon.getSpecies().getEggGroups()) {
      s.append("&e").append(eggGroup).append(" ");
    }
    return s.toString();
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

  public static Pokemon getFirstEvolution(Pokemon pokemon) {
    Pokemon firstEvolution = pokemon;
    while (firstEvolution.getPreEvolution() != null) {
      firstEvolution = firstEvolution.getPreEvolution().getSpecies().create(1);
      firstEvolution.setForm(firstEvolution.getForm());
      firstEvolution.updateForm();
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
  public static String getTranslatedName(Pokemon pokemon) {
    return "<lang:cobblemon.species." + pokemon.getSpecies().showdownId() + ".name>";
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
    int sum = 0;
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
    int sum = 0;
    for (Map.Entry<? extends Stat, ? extends Integer> iV : iVs) {
      sum += iV.getValue();
    }
    return sum / 6;
  }

  public static float getIvsPercent(IVs iVs) {
    if (iVs == null) return 0f;
    int sum = getIvsTotal(iVs);
    return (float) sum / 186 * 100;
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
    int sum = 0;
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

  public static float getEvsPercent(EVs eVs) {
    if (eVs == null) return 0f;
    int sum = getEvsTotal(eVs);
    return (float) sum / 510 * 100;
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

  private static final Map<String, String> cacheRarityStrings = new HashMap<>();

  /**
   * Get the rarity of the pokemon
   *
   * @param pokemon The pokemon to get the rarity
   *
   * @return The rarity of the pokemon
   */
  public static String getRarityS(Pokemon pokemon) {
    String cached = cacheRarityStrings.get(pokemon.showdownId());
    if (cached != null) return cached;
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
      double maxValue = Double.MIN_VALUE;
      for (Map.Entry<String, Double> entry : rarityMap.entrySet()) {
        double value = entry.getValue();
        if (value > maxValue) {
          maxValue = value;
          rarityResult = entry.getKey();
        }
      }
    }
    cacheRarityStrings.put(pokemon.showdownId(), rarityResult);
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
   * Check if the pokemon has the hidden ability
   *
   * @param pokemon The pokemon to check
   *
   * @return If the pokemon has the hidden ability
   */
  public static boolean isAH(Pokemon pokemon) {
    int size = 0;
    var map = pokemon.getForm().getAbilities().getMapping();
    List<String> abilities = new ArrayList<>();
    for (Map.Entry<Priority, List<PotentialAbility>> entry : map.entrySet()) {
      var list = entry.getValue();
      if (list != null && !list.isEmpty()) {
        for (PotentialAbility ability : list) {
          String name = ability.getTemplate().getName();
          if (abilities.contains(name)) continue;
          size++;
          abilities.add(name);
        }
      }
    }
    if (size <= 1) {
      return false;
    }
    for (PotentialAbility ability : pokemon.getForm().getAbilities()) {
      if (ability.getType() instanceof HiddenAbilityType) {
        return ability.getTemplate().getName().equalsIgnoreCase(pokemon.getAbility().getTemplate().getName());
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
    return abilityList.get(Utils.getRandom().nextInt(abilityList.size()));
  }

  public static boolean isLegalAbility(Pokemon pokemon) {
    for (PotentialAbility potentialAbility : pokemon.getForm().getAbilities()) {
      if (pokemon.getAbility().getTemplate().getName().equalsIgnoreCase(potentialAbility.getTemplate().getName())) {
        return true;
      }
    }
    PokemonProperties.Companion.parse("hiddenability=no").apply(pokemon);
    return false;
  }
}
