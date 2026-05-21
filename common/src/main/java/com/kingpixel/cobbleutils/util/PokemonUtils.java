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
import com.cobblemon.mod.common.api.riding.stats.RidingStat;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.*;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbilityType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Carlos Varas Alonso - 28/06/2024 18:53
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PokemonUtils {
  public static final List<Stats> STATS_LIST = List.of(
    Stats.HP,
    Stats.ATTACK,
    Stats.DEFENCE,
    Stats.SPECIAL_ATTACK,
    Stats.SPECIAL_DEFENCE,
    Stats.SPEED
  );
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
   * @return The showdown id of the pokemon
   */
  public static String getIdentifierPokemon(Pokemon pokemon) {
    return pokemon.getSpecies().showdownId();
  }

  /**
   * Replace the placeholders with the pokemon data
   *
   * @param pokemon The pokemon to get the data
   * @return The lore with the replaced placeholders
   */
  public static List<String> replaceLore(Pokemon pokemon) {
    return replace(CobbleUtils.language.getLorepokemon(), pokemon);
  }

  /**
   * Replace the placeholders with the pokemon data
   *
   * @param pokemon The pokemon to get the data
   * @return The string with the replaced placeholders
   */
  public static String replace(Pokemon pokemon) {
    return replace(CobbleUtils.language.getPokemonnameformat(), pokemon);
  }

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(\\w+)%");


  private static String replacePlaceholders(String message, Map<String, String> placeholders) {
    if (message == null || !message.contains("%") || placeholders.isEmpty()) return message;

    Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
    StringBuilder sb = new StringBuilder();

    while (matcher.find()) {
      String key = "%" + matcher.group(1) + "%";
      String replacement = placeholders.getOrDefault(key, key);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private static final String UNKNOWN = CobbleUtils.language.getUnknown();
  private static final String NONE = CobbleUtils.language.getNone();
  private static final String EMPTY = "";

  // Helper para evitar ternarios repetidos
  private static String safe(Pokemon p, Function<Pokemon, String> func) {
    return p != null ? func.apply(p) : UNKNOWN;
  }

  private static final Map<String, Function<Pokemon, String>> PLACEHOLDER_FUNCTIONS = Map.<String, Function<Pokemon, String>>ofEntries(
    Map.entry("%showdownid%", p -> safe(p, Pokemon::showdownId)),
    Map.entry("%level%", p -> safe(p, poke -> String.valueOf(poke.getLevel()))),
    Map.entry("%nature%", p -> safe(p, poke -> getNatureTranslate(poke.getNature()))),
    Map.entry("%pokemon%", p -> safe(p, poke -> isEgg(poke) ? poke.getPersistentData().getString("pokemon") : getTranslatedName(poke))),
    Map.entry("%shiny%", p -> safe(p, poke -> poke.getShiny() ? CobbleUtils.language.getSymbolshiny() : EMPTY)),
    Map.entry("%ability%", p -> safe(p, poke -> isEgg(poke) ? "<lang:cobblemon.ability." + poke.getPersistentData().getString("ability") + ">" : getAbilityTranslate(poke.getAbility()))),
    Map.entry("%tradeable%", p -> safe(p, poke -> poke.getTradeable() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo())),
    // IVs
    Map.entry("%ivshp%", p -> safe(p, poke -> String.valueOf(getIv(poke, Stats.HP)))),
    Map.entry("%ivsatk%", p -> safe(p, poke -> String.valueOf(getIv(poke, Stats.ATTACK)))),
    Map.entry("%ivsdef%", p -> safe(p, poke -> String.valueOf(getIv(poke, Stats.DEFENCE)))),
    Map.entry("%ivsspa%", p -> safe(p, poke -> String.valueOf(getIv(poke, Stats.SPECIAL_ATTACK)))),
    Map.entry("%ivsspdef%", p -> safe(p, poke -> String.valueOf(getIv(poke, Stats.SPECIAL_DEFENCE)))),
    Map.entry("%ivsspeed%", p -> safe(p, poke -> String.valueOf(getIv(poke, Stats.SPEED)))),
    // HyperTrained IVs
    Map.entry("%htivshp%", p -> safe(p,
      poke -> String.valueOf(poke.getIvs().getHyperTrainedIVs().getOrDefault(Stats.HP, getIv(poke, Stats.HP))))),
    Map.entry("%htivsatk%", p -> safe(p,
      poke -> String.valueOf(poke.getIvs().getHyperTrainedIVs().getOrDefault(Stats.ATTACK, getIv(poke, Stats.ATTACK))))),
    Map.entry("%htivsdef%", p -> safe(p, poke -> String.valueOf(poke.getIvs().getHyperTrainedIVs().getOrDefault(Stats.DEFENCE, getIv(poke, Stats.DEFENCE))))),
    Map.entry("%htivsspa%", p -> safe(p, poke -> String.valueOf(poke.getIvs().getHyperTrainedIVs().getOrDefault(Stats.SPECIAL_ATTACK, getIv(poke, Stats.SPECIAL_ATTACK))))),
    Map.entry("%htivsspdef%", p -> safe(p, poke -> String.valueOf(poke.getIvs().getHyperTrainedIVs().getOrDefault(Stats.SPECIAL_DEFENCE, getIv(poke, Stats.SPECIAL_DEFENCE))))),
    Map.entry("%htivsspeed%", p -> safe(p, poke -> String.valueOf(poke.getIvs().getHyperTrainedIVs().getOrDefault(Stats.SPEED, getIv(poke, Stats.SPEED))))),
    // Riding IVs
    Map.entry("%riding_speed%", p -> safe(p, poke -> String.valueOf(poke.getRideBoost(RidingStat.SPEED)))),
    Map.entry("%riding_stamina%", p -> safe(p, poke -> String.valueOf(poke.getRideBoost(RidingStat.STAMINA)))),
    Map.entry("%riding_acceleration%", p -> safe(p, poke -> String.valueOf(poke.getRideBoost(RidingStat.ACCELERATION)))),
    Map.entry("%riding_jump%", p -> safe(p, poke -> String.valueOf(poke.getRideBoost(RidingStat.JUMP)))),
    Map.entry("%riding_handling%", p -> safe(p, poke -> String.valueOf(poke.getRideBoost(RidingStat.SKILL)))),
    // EVs
    Map.entry("%evshp%", p -> safe(p, poke -> String.valueOf(poke.getEvs().get(Stats.HP)))),
    Map.entry("%evsatk%", p -> safe(p, poke -> String.valueOf(poke.getEvs().get(Stats.ATTACK)))),
    Map.entry("%evsdef%", p -> safe(p, poke -> String.valueOf(poke.getEvs().get(Stats.DEFENCE)))),
    Map.entry("%evsspa%", p -> safe(p, poke -> String.valueOf(poke.getEvs().get(Stats.SPECIAL_ATTACK)))),
    Map.entry("%evsspdef%", p -> safe(p, poke -> String.valueOf(poke.getEvs().get(Stats.SPECIAL_DEFENCE)))),
    Map.entry("%evsspeed%", p -> safe(p, poke -> String.valueOf(poke.getEvs().get(Stats.SPEED)))),
    // Otros atributos
    Map.entry("%item%", p -> safe(p, poke -> ItemUtils.getTranslatedName(poke.heldItem()))),
    Map.entry("%size%", p -> safe(p, PokemonUtils::getSize)),
    Map.entry("%form%", p -> safe(p, PokemonUtils::getForm)),
    Map.entry("%up%", p -> safe(p, poke -> getStatTranslate(poke.getNature().getIncreasedStat()))),
    Map.entry("%down%", p -> safe(p, poke -> getStatTranslate(poke.getNature().getDecreasedStat()))),
    Map.entry("%ball%", p -> safe(p, poke -> getPokeBallTranslate(poke.getCaughtBall()))),
    Map.entry("%gender%", p -> safe(p, poke -> getGenderTranslate(poke.getGender()))),
    Map.entry("%ivs%", p -> safe(p, poke -> getIvsAverage(poke.getIvs()).toString())),
    Map.entry("%evs%", p -> safe(p, poke -> getEvsTotal(poke.getEvs()).toString())),
    Map.entry("%ivspercent%", p -> safe(p, poke -> String.format("%.2f", getIvsPercent(poke.getIvs())))),
    Map.entry("%evspercent%", p -> safe(p, poke -> String.format("%.2f", getEvsPercent(poke.getEvs())))),
    Map.entry("%move1%", p -> safe(p, poke -> getMoveSafe(poke, 0))),
    Map.entry("%move2%", p -> safe(p, poke -> getMoveSafe(poke, 1))),
    Map.entry("%move3%", p -> safe(p, poke -> getMoveSafe(poke, 2))),
    Map.entry("%move4%", p -> safe(p, poke -> getMoveSafe(poke, 3))),
    Map.entry("%owner%", p -> safe(p, PokemonUtils::getOwnerName)),
    Map.entry("%types%", p -> safe(p, PokemonUtils::getType)),
    Map.entry("%rarity%", p -> safe(p, PokemonUtils::getRarityS)),
    Map.entry("%breedable%", p -> safe(p, poke -> isBreedable(poke) ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo())),
    Map.entry("%friendship%", p -> safe(p, poke -> String.valueOf(poke.getFriendship()))),
    Map.entry("%ah%", p -> safe(p, poke -> isAH(poke) ? CobbleUtils.language.getHA() : EMPTY)),
    Map.entry("%ha%", p -> safe(p, poke -> isAH(poke) ? CobbleUtils.language.getHA() : EMPTY)),
    Map.entry("%country%", p -> safe(p, poke -> {
      String c = poke.getPersistentData().getString(CobbleUtilsTags.COUNTRY_TAG);
      return (c == null || c.isEmpty()) ? NONE : c;
    })),
    Map.entry("%egggroups%", p -> safe(p, PokemonUtils::eggGroups)),
    Map.entry("%dex%", p -> safe(p, poke -> String.valueOf(poke.getSpecies().getNationalPokedexNumber()))),
    Map.entry("%labels%", p -> safe(p, poke -> poke.getForm().getLabels().toString())),
    Map.entry("%aspects%", p -> safe(p, poke -> poke.getAspects().toString()))
  );

  private static int getIv(Pokemon pokemon, Stat stat) {
    return pokemon.getIvs().getOrDefault(stat);
  }

  public static Map<String, String> buildPlaceholders(Pokemon pokemon, String indexStr) {
    Map<String, String> result = HashMap.newHashMap(PLACEHOLDER_FUNCTIONS.size());
    String index = (indexStr == null) ? EMPTY : indexStr;

    for (Map.Entry<String, Function<Pokemon, String>> entry : PLACEHOLDER_FUNCTIONS.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue().apply(pokemon);
      if (!index.isEmpty()) {
        key = key.substring(0, key.length() - 1) + index + "%";
      }
      result.put(key, value);
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
    for (EggGroup eggGroup : pokemon.getForm().getEggGroups()) {
      s.append("&e").append(eggGroup).append(" ");
    }
    return s.toString();
  }

  private static final Set<String> NORMAL_ASPECTS = Set.of(
    "male", "female", "genderless", "milkable", "family", "bucket-"
  );

  private static String getForm(Pokemon pokemon) {
    if (pokemon == null) return CobbleUtils.language.getUnknown();

    if (isEgg(pokemon)) {
      String eggForm = pokemon.getPersistentData().getString("form");
      return eggForm.isEmpty() ? "Normal" : eggForm;
    }

    String formName = pokemon.getForm().getName();
    if (!formName.equalsIgnoreCase("normal")) {
      return CobbleUtils.language.getForms().getOrDefault(formName, formName);
    }

    for (String raw : pokemon.getAspects()) {
      String s = raw;
      int dashIndex = s.lastIndexOf('-');
      if (dashIndex != -1 && dashIndex + 1 < s.length()) {
        s = s.substring(dashIndex + 1);
      }
      if (!NORMAL_ASPECTS.contains(s.toLowerCase())) {
        return s;
      }
    }

    return "Normal";
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
      return PokemonSpecies.getByIdentifier(Identifier.of("cobblemon:phione")).create(1);
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
   * @return The size of the pokemon
   */
  public static String getSize(Pokemon pokemon) {
    return getSizeName(pokemon);
  }

  /**
   * Get the total of the IVs
   *
   * @param iVs The IVs to get the total
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
   * Get the total number of max IVs (31)
   *
   * @param iVs The IVs to get the number max Ivs
   * @return The number of the maxed IVs
   */
  public static Integer getTotalPerfectIvs(IVs iVs) {
    int count = 0;
    for (Map.Entry<? extends Stat, ? extends Integer> iV : iVs) {
      if (iV.getValue() == 31) count++;
    }
    return count;
  }

  /**
   * Get the total of the EVs
   *
   * @param eVs The EVs to get the total
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
   * @return The nature translation
   */
  public static String getNatureTranslate(Nature nature) {
    if (nature == null) return CobbleUtils.language.getNone();
    return "<lang:cobblemon.nature." + nature.getName().getPath() + ">";
  }

  public static String getMoveColor(ElementalType type, String lang) {
    if (type == null) return CobbleUtils.language.getNone();
    String color = CobbleUtils.language.getMovecolor().getOrDefault(type.getShowdownId(), "");
    if (color.contains("gradient")) return color + "<lang:" + lang + ">" + "</gradient>";
    return color + "<lang:" + lang + ">";
  }

  public static String getType(ElementalType type) {
    if (type == null) return CobbleUtils.language.getNone();
    return CobbleUtils.language.getTypes().getOrDefault(type.getShowdownId(), type.getShowdownId());
  }

  /**
   * Get the type of the pokemon
   *
   * @param pokemon The pokemon to get the type
   * @return The type of the pokemon
   */
  public static String getType(Pokemon pokemon) {
    if (pokemon == null) return "";
    String key = pokemon.getPrimaryType().getShowdownId() + (pokemon.getSecondaryType() != null ? ":" + pokemon.getSecondaryType().getShowdownId() : "");
    return typeCache.get(key, k -> {
      StringBuilder s = new StringBuilder(getType(pokemon.getPrimaryType()));
      if (pokemon.getSecondaryType() != null) {
        s.append(" &7/ ");
        s.append(getType(pokemon.getSecondaryType()));
      }
      return s.toString();
    });
  }

  private static final Cache<String, String> typeCache = Caffeine.newBuilder()
    .maximumSize(400) // un poco más que el máximo posible de combinaciones
    .expireAfterAccess(60, TimeUnit.MINUTES)
    .build();

  /**
   * Get the move translation
   *
   * @param move The move to translate
   * @return The move translation
   */


  public static String getMoveTranslate(Move move) {
    if (move == null) return CobbleUtils.language.getNone();

    String key = move.getName(); // asumimos que el nombre es único

    return moveTranslateCache.get(key, k ->
      getMoveColor(move.getType(), "cobblemon.move." + move.getName())
    );
  }

  private static final Cache<String, String> moveTranslateCache = Caffeine.newBuilder()
    .maximumSize(1000) // ajusta según cantidad de moves
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .build();

  /**
   * Get the stat translation
   *
   * @param stat The stat to translate
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
   * @return The pokeball translation
   */
  public static String getPokeBallTranslate(PokeBall caughtBall) {
    if (caughtBall == null) return CobbleUtils.language.getNone();

    String key = caughtBall.getName().getPath();

    // Devuelve del cache si existe, si no calcula y guarda
    return pokeBallCache.computeIfAbsent(key, k ->
      Text.translatable("item.cobblemon." + k).getString()
    );
  }

  private static final Map<String, String> pokeBallCache = new ConcurrentHashMap<>();

  /**
   * Get the gender translation
   *
   * @param gender The gender to translate
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
   * @return The size of the pokemon
   */
  public static String getSizeName(Pokemon pokemon) {
    String size = pokemon.getPersistentData().getString("size");
    if (size.isEmpty()) return CobbleUtils.language.getUnknown();
    return CobbleUtils.language.getSizes().getOrDefault(size, size);
  }

  /**
   * Check if the pokemon has the hidden ability
   *
   * @param pokemon The pokemon to check
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