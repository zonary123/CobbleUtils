package com.kingpixel.cobbleutils.util;

import com.cobblemon.mod.common.api.Priority;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Carlos Varas Alonso - 28/06/2024 18:53
 */
public class PokemonUtils {

  private static String CACHED_LORE;

  private static String getCachedLore() {
    if (CACHED_LORE == null) {
      CACHED_LORE = String.join("\n", CobbleUtils.language.getLorepokemon());
    }
    return CACHED_LORE;
  }

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
    List<String> result = new ArrayList<>();

    for (String line : lore) {
      if (line.contains("%lorepokemon%")) {
        for (String extra : CobbleUtils.language.getLorepokemon()) {
          result.add(replacePlaceholders(extra, placeholders));
        }
      } else {
        result.add(replacePlaceholders(line, placeholders));
      }
    }

    return result;
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

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(\\w+)%");


  private static String replacePlaceholders(String message, Map<String, String> placeholders) {
    if (message == null || !message.contains("%") || placeholders.isEmpty()) return message;

    Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String key = "%" + matcher.group(1) + "%";
      String replacement = placeholders.getOrDefault(key, key);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }


  private static final Map<String, Function<Pokemon, String>> PLACEHOLDER_FUNCTIONS = Map.ofEntries(
    Map.entry("%showdownid%", (p) -> p != null ? p.showdownId() : CobbleUtils.language.getUnknown()),
    Map.entry("%level%", (p) -> p != null ? String.valueOf(p.getLevel()) : CobbleUtils.language.getUnknown()),
    Map.entry("%nature%", (p) -> p != null ? getNatureTranslate(p.getNature()) : CobbleUtils.language.getUnknown()),
    Map.entry("%pokemon%", (p) -> p != null ? (isEgg(p) ? p.getPersistentData().getString("pokemon") : getTranslatedName(p)) : CobbleUtils.language.getUnknown()),
    Map.entry("%shiny%", (p) -> p != null ? (p.getShiny() ? CobbleUtils.language.getSymbolshiny() : "") : ""),
    Map.entry("%ability%", (p) -> p != null ? (isEgg(p) ? "<lang:cobblemon.ability." + p.getPersistentData().getString("ability") + ">" : getAbilityTranslate(p.getAbility())) : CobbleUtils.language.getUnknown()),
    Map.entry("%tradeable%", (p) -> p != null ? (p.getTradeable() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo()) : CobbleUtils.language.getUnknown()),
    // IVs
    Map.entry("%ivshp%", (p) -> p != null ? String.valueOf(p.getIvs().get(Stats.HP)) : "0"),
    Map.entry("%ivsatk%", (p) -> p != null ? String.valueOf(p.getIvs().get(Stats.ATTACK)) : "0"),
    Map.entry("%ivsdef%", (p) -> p != null ? String.valueOf(p.getIvs().get(Stats.DEFENCE)) : "0"),
    Map.entry("%ivsspa%", (p) -> p != null ? String.valueOf(p.getIvs().get(Stats.SPECIAL_ATTACK)) : "0"),
    Map.entry("%ivsspdef%", (p) -> p != null ? String.valueOf(p.getIvs().get(Stats.SPECIAL_DEFENCE)) : "0"),
    Map.entry("%ivsspeed%", (p) -> p != null ? String.valueOf(p.getIvs().get(Stats.SPEED)) : "0"),
    // EVs
    Map.entry("%evshp%", (p) -> p != null ? String.valueOf(p.getEvs().get(Stats.HP)) : "0"),
    Map.entry("%evsatk%", (p) -> p != null ? String.valueOf(p.getEvs().get(Stats.ATTACK)) : "0"),
    Map.entry("%evsdef%", (p) -> p != null ? String.valueOf(p.getEvs().get(Stats.DEFENCE)) : "0"),
    Map.entry("%evsspa%", (p) -> p != null ? String.valueOf(p.getEvs().get(Stats.SPECIAL_ATTACK)) : "0"),
    Map.entry("%evsspdef%", (p) -> p != null ? String.valueOf(p.getEvs().get(Stats.SPECIAL_DEFENCE)) : "0"),
    Map.entry("%evsspeed%", (p) -> p != null ? String.valueOf(p.getEvs().get(Stats.SPEED)) : "0"),
    // Otros atributos
    Map.entry("%item%", (p) -> p != null ? ItemUtils.getTranslatedName(p.heldItem()) : CobbleUtils.language.getNone()),
    Map.entry("%size%", (p) -> p != null ? getSize(p) : CobbleUtils.language.getUnknown()),
    Map.entry("%form%", (p) -> p != null ? getForm(p) : CobbleUtils.language.getUnknown()),
    Map.entry("%up%", (p) -> p != null ? getStatTranslate(p.getNature().getIncreasedStat()) : ""),
    Map.entry("%down%", (p) -> p != null ? getStatTranslate(p.getNature().getDecreasedStat()) : ""),
    Map.entry("%ball%", (p) -> p != null ? getPokeBallTranslate(p.getCaughtBall()) : CobbleUtils.language.getUnknown()),
    Map.entry("%gender%", (p) -> p != null ? getGenderTranslate(p.getGender()) : ""),
    Map.entry("%ivs%", (p) -> p != null ? getIvsAverage(p.getIvs()).toString() : "0"),
    Map.entry("%evs%", (p) -> p != null ? getEvsTotal(p.getEvs()).toString() : "0"),
    Map.entry("%ivspercent%", (p) -> p != null ? String.format("%.2f", getIvsPercent(p.getIvs())) : "0"),
    Map.entry("%evspercent%", (p) -> p != null ? String.format("%.2f", getEvsPercent(p.getEvs())) : "0"),
    Map.entry("%move1%", (p) -> p != null ? getMoveSafe(p, 0) : CobbleUtils.language.getNone()),
    Map.entry("%move2%", (p) -> p != null ? getMoveSafe(p, 1) : CobbleUtils.language.getNone()),
    Map.entry("%move3%", (p) -> p != null ? getMoveSafe(p, 2) : CobbleUtils.language.getNone()),
    Map.entry("%move4%", (p) -> p != null ? getMoveSafe(p, 3) : CobbleUtils.language.getNone()),
    Map.entry("%owner%", (p) -> p != null ? getOwnerName(p) : CobbleUtils.language.getNone()),
    Map.entry("%types%", (p) -> p != null ? getType(p) : CobbleUtils.language.getNone()),
    Map.entry("%rarity%", (p) -> p != null ? getRarityS(p) : CobbleUtils.language.getNone()),
    Map.entry("%breedable%", (p) -> p != null ? (isBreedable(p) ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo()) : CobbleUtils.language.getNo()),
    Map.entry("%friendship%", (p) -> p != null ? String.valueOf(p.getFriendship()) : "0"),
    Map.entry("%ah%", (p) -> p != null ? (isAH(p) ? CobbleUtils.language.getHA() : "") : ""),
    Map.entry("%ha%", (p) -> p != null ? (isAH(p) ? CobbleUtils.language.getHA() : "") : ""),
    Map.entry("%country%", (p) -> p != null ? Optional.ofNullable(p.getPersistentData().getString(CobbleUtilsTags.COUNTRY_TAG)).orElse(CobbleUtils.language.getNone()) : CobbleUtils.language.getNone()),
    Map.entry("%egggroups%", (p) -> p != null ? eggGroups(p) : CobbleUtils.language.getNone()),
    Map.entry("%dex%", (p) -> p != null ? String.valueOf(p.getSpecies().getNationalPokedexNumber()) : "0"),
    Map.entry("%labels%", (p) -> p != null ? p.getForm().getLabels().toString() : ""),
    Map.entry("%aspects%", (p) -> p != null ? p.getAspects().stream().toList().toString() : "")
  );


  private static Map<String, String> buildPlaceholders(Pokemon pokemon, String indexStr) {
    Map<String, String> result = new HashMap<>();
    indexStr = (indexStr == null) ? "" : indexStr;

    for (Map.Entry<String, Function<Pokemon, String>> entry : PLACEHOLDER_FUNCTIONS.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue().apply(pokemon);

      if (!indexStr.isEmpty()) {
        String indexedKey = key.substring(0, key.length() - 1) + indexStr + "%";
        result.put(indexedKey, value);
      } else {
        result.put(key, value);
      }
    }
    return result;
  }


  private static String getMoveSafe(Pokemon p, int index) {
    List<Move> moves = p.getMoveSet().getMoves();
    return index < moves.size() ? getMoveTranslate(moves.get(index)) : CobbleUtils.language.getNone();
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
      message = message.replace("%lorepokemon%", getCachedLore());
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
    for (Map.Entry<? extends Stat, ? extends Integer> iV : iVs) sum += iV.getValue();
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
    for (Map.Entry<? extends Stat, ? extends Integer> iV : iVs) sum += iV.getValue();
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
    for (Map.Entry<? extends Stat, ? extends Integer> eV : eVs) sum += eV.getValue();
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
    for (Map.Entry<? extends Stat, ? extends Integer> eV : eVs) sum += eV.getValue();
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
    if (color.contains("gradient")) return color + "<lang:" + lang + ">" + "</gradient>";
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
    if (move == null) return CobbleUtils.language.getNone();
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
    if (stat == null) return "";
    return switch (stat.getIdentifier().toTranslationKey()) {
      case "cobblemon.hp" -> "<lang:cobblemon.ui.stats.hp>";
      case "cobblemon.attack" -> "<lang:cobblemon.ui.stats.atk>";
      case "cobblemon.defence" -> "<lang:cobblemon.ui.stats.def>";
      case "cobblemon.special_attack" -> "<lang:cobblemon.ui.stats.sp_atk>";
      case "cobblemon.special_defence" -> "<lang:cobblemon.ui.stats.sp_def>";
      case "cobblemon.speed" -> "<lang:cobblemon.ui.stats.speed>";
      default -> "";
    };
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
