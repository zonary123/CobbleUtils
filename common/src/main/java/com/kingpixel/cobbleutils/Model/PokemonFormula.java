package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.pokemon.evolution.PreEvolution;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.riding.stats.RidingStat;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.NbtUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * PokemonFormula class is responsible for calculating the value of a Pokemon
 * using a dynamic mathematical formula with variables such as IVs, EVs, nature,
 * gender, ball, held items, labels, etc.
 * <p>
 * Author: Carlos Varas Alonso - 19/03/2025 1:37
 */
@Data
public class PokemonFormula {
  private static final String SOURCE_BUILT_IN = "builtin";
  private static final String SOURCE_GLOBAL = "global";
  private static final String SOURCE_CUSTOM = "custom";

  public enum VariableCategory {
    CORE,
    POKEMON,
    STATS,
    RIDING,
    PERSISTENT,
    EXTERNAL
  }

  public interface PokemonFormulaVariableProvider {
    void register(PokemonFormula formula, VariableRegistrar registrar);
  }

  public interface VariableRegistrar {
    void register(String key, Function<Pokemon, Float> resolver, String description, VariableCategory category, int priority);
  }

  public record VariableInfo(
    String key,
    float value,
    String description,
    VariableCategory category,
    int priority,
    String source
  ) {
  }

  private boolean showVariablesInConsole = false;
  private String formula;
  // Base configurations
  private float base = 0;
  private float notShiny = 1;
  private float shiny = 0;
  private float notHiddenAbility = 1;
  private float hiddenAbility = 0;
  // Variable maps
  private Map<String, Float> pokemonBase = new HashMap<>();
  private boolean acumulationTypes = false;
  private Map<String, Float> types = new HashMap<>();
  private Map<String, Float> form = new HashMap<>();
  private Map<Gender, Float> gender = new EnumMap<>(Gender.class);
  private Map<String, Float> nature = new HashMap<>();
  private Map<String, Float> ball = new HashMap<>();
  private boolean accumulationAspects = false;
  private Map<String, Float> aspect = new HashMap<>();
  private boolean accumulationLabels = false;
  private Map<String, Float> labels = new HashMap<>();
  private Map<Boolean, Float> breedable = new HashMap<>();
  private Map<String, Float> rarity = new HashMap<>();
  private Map<String, Map<Object, Float>> persistentDataValues = new HashMap<>();
  // Held item prices
  private HeldItemPrice heldItemPrice = new HeldItemPrice();

  private static final Map<String, VariableResolverEntry> GLOBAL_VARIABLES = new ConcurrentHashMap<>();
  private static final Map<String, PokemonFormulaVariableProvider> PROVIDERS = new ConcurrentHashMap<>();

  // Dynamic variable resolvers for this formula instance
  private transient final Map<String, VariableResolverEntry> customVariables = new ConcurrentHashMap<>();
  private transient final Map<String, VariableResolverEntry> variableResolvers = new LinkedHashMap<>();

  public PokemonFormula() {
    this.formula = "base + types + heldItem + gender + labels + nature + ability + ivsAverage + ivsTotal + " +
      "totalPerfectIvs + evsTotal +" +
      " evsAverage + form + ball + aspect + shiny + breedable + rarity";

    initDefaults();
    registerVariables();
  }

  public static void registerGlobalVariable(String name, Function<Pokemon, Float> resolver) {
    registerGlobalVariable(name, resolver, "Global variable");
  }

  public static void registerGlobalVariable(String name, Function<Pokemon, Float> resolver, String description) {
    registerGlobalVariable(name, resolver, description, VariableCategory.EXTERNAL, 100);
  }

  public static void registerGlobalVariable(
    String name,
    Function<Pokemon, Float> resolver,
    String description,
    VariableCategory category,
    int priority
  ) {
    String normalized = normalizeVariableName(name);
    Objects.requireNonNull(resolver, "resolver cannot be null");
    GLOBAL_VARIABLES.put(normalized, new VariableResolverEntry(
      normalized,
      resolver,
      defaultDescription(description),
      category == null ? VariableCategory.EXTERNAL : category,
      priority,
      SOURCE_GLOBAL
    ));
  }

  public static void unregisterGlobalVariable(String name) {
    GLOBAL_VARIABLES.remove(normalizeVariableName(name));
  }

  public static void registerProvider(String modId, PokemonFormulaVariableProvider provider) {
    Objects.requireNonNull(provider, "provider cannot be null");
    PROVIDERS.put(normalizeVariableName(modId), provider);
  }

  public static void unregisterProvider(String modId) {
    PROVIDERS.remove(normalizeVariableName(modId));
  }

  public void registerCustomVariable(String name, Function<Pokemon, Float> resolver) {
    registerCustomVariable(name, resolver, "Custom variable");
  }

  public void registerCustomVariable(String name, Function<Pokemon, Float> resolver, String description) {
    registerCustomVariable(name, resolver, description, VariableCategory.EXTERNAL, 200);
  }

  public void registerCustomVariable(
    String name,
    Function<Pokemon, Float> resolver,
    String description,
    VariableCategory category,
    int priority
  ) {
    String normalized = normalizeVariableName(name);
    Objects.requireNonNull(resolver, "resolver cannot be null");
    customVariables.put(normalized, new VariableResolverEntry(
      normalized,
      resolver,
      defaultDescription(description),
      category == null ? VariableCategory.EXTERNAL : category,
      priority,
      SOURCE_CUSTOM
    ));
  }

  public void unregisterCustomVariable(String name) {
    customVariables.remove(normalizeVariableName(name));
  }

  /**
   * Returns all variable keys currently available for this formula.
   * Useful to render a menu or debug list.
   */
  public List<String> getAvailableVariables() {
    registerVariables();
    return variableResolvers.values().stream()
      .sorted(variableSort())
      .map(VariableResolverEntry::key)
      .toList();
  }

  /**
   * Returns variable descriptions indexed by key.
   */
  public Map<String, String> getVariableDescriptions() {
    registerVariables();
    Map<String, String> out = new LinkedHashMap<>();
    variableResolvers.values().stream()
      .sorted(variableSort())
      .forEach(entry -> out.put(entry.key(), entry.description()));
    return out;
  }

  /**
   * Evaluates every variable for the given pokemon and returns key/value pairs.
   */
  public Map<String, Float> evaluateVariables(Pokemon pokemon) {
    Map<String, Float> values = new LinkedHashMap<>();
    for (VariableInfo info : evaluateVariableInfo(pokemon)) {
      values.put(info.key(), info.value());
    }
    return values;
  }

  /**
   * Evaluates all variables with metadata for debug menus/logging.
   */
  public List<VariableInfo> evaluateVariableInfo(Pokemon pokemon) {
    registerVariables();
    List<VariableInfo> values = new ArrayList<>();
    variableResolvers.values().stream()
      .sorted(variableSort())
      .forEach(entry -> values.add(new VariableInfo(
        entry.key(),
        safeResolve(entry, pokemon),
        entry.description(),
        entry.category(),
        entry.priority(),
        entry.source()
      )));
    return values;
  }

  /**
   * Returns a multi-line debug dump with all variables and current values.
   */
  public String getVariablesDebugDump(Pokemon pokemon) {
    StringBuilder sb = new StringBuilder();
    sb.append("Pokemon formula variables for ")
      .append(pokemon.getDisplayName(false).getString())
      .append(" (formula=")
      .append(formula)
      .append(")\n");

    evaluateVariableInfo(pokemon).forEach(info -> sb
      .append(" - ")
      .append(info.key())
      .append(" = ")
      .append(info.value())
      .append(" [")
      .append(info.category())
      .append(" | ")
      .append(info.source())
      .append("]\n"));

    return sb.toString().trim();
  }

  /**
   * Sends all current variable values to the player chat for quick debugging.
   */
  public void sendVariablesDebug(ServerPlayerEntity player, Pokemon pokemon) {
    if (player == null || pokemon == null) {
      return;
    }
    player.sendMessage(Text.literal(getVariablesDebugDump(pokemon)), false);
  }

  /**
   * Initializes default values for maps.
   */
  private void initDefaults() {
    pokemonBase.put("example", 0f);
    form.put("example", 0f);
    aspect.put("example", 0f);
    for (ElementalType elementalType : ElementalTypes.all()) {
      types.put(elementalType.getShowdownId(), 0f);
    }
    for (Gender g : Gender.values()) {
      gender.put(g, 0f);
    }
    nature.put("example", 0f);
    ball.put("cobblemon:poke_ball", 0f);
    labels.put("legendary", 0f);
    breedable.put(true, 0f);
    breedable.put(false, 0f);
    persistentDataValues.put(
      "example_data_1",
      Map.of(
        "common", 100f,
        "uncommon", 0f
      )
    );
    rarity.put("common", 0f);
    persistentDataValues.put(
      "example_data_2",
      Map.of(
        false, 0f,
        true, 0f
      )
    );
  }

  /**
   * Registers all variable resolvers used inside the formula.
   * Built-ins are loaded first, then provider/global/custom variables (same key overrides).
   */
  private void registerVariables() {
    variableResolvers.clear();

    registerBuiltInVariable("base", this::getBase, "Base value by pokemon id", VariableCategory.CORE, 0);
    registerBuiltInVariable("types", this::getTypes, "Type bonus (sum or max)", VariableCategory.POKEMON, 10);
    registerBuiltInVariable("heldItem", this::getHeldItem, "Held item configured value", VariableCategory.POKEMON, 20);
    registerBuiltInVariable("shiny", p -> p.getShiny() ? shiny : notShiny, "Shiny bonus", VariableCategory.POKEMON, 30);
    registerBuiltInVariable("gender", this::getGender, "Gender multiplier", VariableCategory.POKEMON, 40);
    registerBuiltInVariable("labels", this::getLabel, "Label bonus (sum or max)", VariableCategory.POKEMON, 50);
    registerBuiltInVariable("nature", this::getNature, "Nature bonus", VariableCategory.POKEMON, 60);
    registerBuiltInVariable("ability", this::getAbility, "Ability bonus", VariableCategory.POKEMON, 70);
    registerBuiltInVariable("form", this::getForm, "Pokemon form bonus", VariableCategory.POKEMON, 80);
    registerBuiltInVariable("ball", this::getBall, "Caught ball bonus", VariableCategory.POKEMON, 90);
    registerBuiltInVariable("aspect", this::getAspect, "Aspect bonus (sum or max)", VariableCategory.POKEMON, 100);
    registerBuiltInVariable("breedable", this::getBreedable, "Breedable bonus", VariableCategory.POKEMON, 110);
    registerBuiltInVariable("friendship", p -> (float) Math.max(p.getFriendship(), 1), "Friendship value", VariableCategory.POKEMON, 120);
    registerBuiltInVariable("level", p -> (float) Math.max(p.getLevel(), 1), "Pokemon level", VariableCategory.POKEMON, 130);
    registerBuiltInVariable("evolutions", this::getEvolversCount, "Pre-evolution depth", VariableCategory.POKEMON, 140);
    registerBuiltInVariable("rarity", p -> rarity.getOrDefault(PokemonUtils.getRarityS(p), 0f), "Rarity bonus", VariableCategory.POKEMON, 150);

    registerBuiltInVariable("ivsTotal", p -> (float) Math.max(PokemonUtils.getIvsTotal(p.getIvs()), 1), "Total IVs", VariableCategory.STATS, 160);
    registerBuiltInVariable("ivsAverage", p -> (float) Math.max(PokemonUtils.getIvsAverage(p.getIvs()), 1), "Average IVs", VariableCategory.STATS, 170);
    registerBuiltInVariable("totalPerfectIvs", p -> (float) Math.max(PokemonUtils.getTotalPerfectIvs(p.getIvs()), 0), "Perfect IV count", VariableCategory.STATS, 180);
    registerBuiltInVariable("evsTotal", p -> (float) Math.max(PokemonUtils.getEvsTotal(p.getEvs()), 1), "Total EVs", VariableCategory.STATS, 190);
    registerBuiltInVariable("evsAverage", p -> (float) Math.max(PokemonUtils.getEvsAverage(p.getEvs()), 1), "Average EVs", VariableCategory.STATS, 200);

    for (Stats stats : PokemonUtils.STATS_LIST) {
      String showdownId = stats.getShowdownId();
      registerBuiltInVariable("iv_" + showdownId, p -> (float) Math.max(p.getIvs().getOrDefault(stats), 1), "IV by stat " + showdownId, VariableCategory.STATS, 210);
      registerBuiltInVariable("ev_" + showdownId, p -> (float) Math.max(p.getEvs().getOrDefault(stats), 1), "EV by stat " + showdownId, VariableCategory.STATS, 220);
      registerBuiltInVariable("ht_iv_" + showdownId, p -> (float) p.getIvs().getHyperTrainedIVs().getOrDefault(stats, 0), "Hyper trained IV by stat " + showdownId, VariableCategory.STATS, 230);
    }

    for (RidingStat value : RidingStat.values()) {
      String key = "riding_" + value.name();
      registerBuiltInVariable(key, p -> p.getRideBoost(value), "Riding boost " + value.name(), VariableCategory.RIDING, 240);
    }

    persistentDataValues.forEach((key, map) -> registerBuiltInVariable(
      key,
      p -> {
        try {
          var persistentData = p.getPersistentData();
          var nbtElement = persistentData.get(key);
          if (nbtElement == null) return 0f;
          var nbtValue = NbtUtils.convertNbtValue(nbtElement);
          if (nbtValue == null) return 0f;
          var value = map.getOrDefault(nbtValue, 0f);
          return value != null ? value : 0f;
        } catch (Exception e) {
          CobbleUtils.LOGGER_RAW.error("Error getting persistent data variable '{}' for pokemon {}", key, p.getDisplayName(false).getString(), e);
          return 0f;
        }
      },
      "Persistent data variable",
      VariableCategory.PERSISTENT,
      250
    ));

    registerProviderVariables();

    GLOBAL_VARIABLES.forEach((key, entry) -> variableResolvers.put(key, entry.withSource(SOURCE_GLOBAL)));
    customVariables.forEach((key, entry) -> variableResolvers.put(key, entry.withSource(SOURCE_CUSTOM)));

    if (showVariablesInConsole) {
      CobbleUtils.LOGGER_RAW.info("Pokemon formula available variables: {}", variableResolvers.keySet());
    }
  }

  private void registerProviderVariables() {
    PROVIDERS.forEach((providerId, provider) -> {
      try {
        provider.register(this, (key, resolver, description, category, priority) -> {
          String normalized = normalizeVariableName(key);
          variableResolvers.put(normalized, new VariableResolverEntry(
            normalized,
            resolver,
            defaultDescription(description),
            category == null ? VariableCategory.EXTERNAL : category,
            priority,
            providerId
          ));
        });
      } catch (Exception e) {
        CobbleUtils.LOGGER_RAW.error("Error registering pokemon formula provider '{}'", providerId, e);
      }
    });
  }

  private void registerBuiltInVariable(
    String key,
    Function<Pokemon, Float> resolver,
    String description,
    VariableCategory category,
    int priority
  ) {
    variableResolvers.put(key, new VariableResolverEntry(
      key,
      resolver,
      defaultDescription(description),
      category,
      priority,
      SOURCE_BUILT_IN
    ));
  }

  private static String normalizeVariableName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Variable name cannot be null/blank");
    }
    return name.trim();
  }

  private static String defaultDescription(String description) {
    return (description == null || description.isBlank()) ? "No description" : description;
  }

  private static Comparator<VariableResolverEntry> variableSort() {
    return Comparator
      .comparing(VariableResolverEntry::category)
      .thenComparingInt(VariableResolverEntry::priority)
      .thenComparing(VariableResolverEntry::key);
  }

  private float safeResolve(VariableResolverEntry entry, Pokemon pokemon) {
    try {
      Float value = entry.resolver().apply(pokemon);
      if (value == null || value.isNaN() || value.isInfinite()) {
        return 0f;
      }
      return value;
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Error resolving pokemon formula variable '{}'", entry.key(), e);
      return 0f;
    }
  }

  private Float getTypes(Pokemon pokemon) {
    float value = 0f;
    var elementalTypes = pokemon.getForm().getTypes();
    for (ElementalType type : elementalTypes) {
      if (acumulationTypes) {
        value += types.getOrDefault(type.getShowdownId(), 0f);
      } else {
        value = Math.max(value, types.getOrDefault(type.getShowdownId(), 0f));
      }
    }
    return value;
  }

  private Float getEvolversCount(Pokemon pokemon) {
    PreEvolution preEvolution = pokemon.getPreEvolution();
    return preEvolution == null ? 0f : 1f;
  }

  /**
   * Gets the calculated value of a Pokemon.
   */
  public Double getPokemonValue(Pokemon pokemon) {
    try {
      if (formula == null || formula.isEmpty()) return 0.0;
      return getPokemonExpression(pokemon).evaluate();
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Error evaluating pokemon formula for {}", pokemon.getDisplayName(false).getString(), e);
      return 0.0;
    }
  }

  /**
   * Builds the Pokemon-specific expression with dynamic variables set.
   */
  public Expression getPokemonExpression(Pokemon pokemon) {
    ServerPlayerEntity player = pokemon.getOwnerPlayer();
    Expression expr = getExpression();
    if (showVariablesInConsole) {
      String head = "Evaluating Pokemon: " + pokemon.getDisplayName(false).getString() + " | ID: " + pokemon.showdownId() + " | Hash: " + System.identityHashCode(pokemon);
      CobbleUtils.LOGGER_RAW.info(head);
      if (player != null) {
        player.sendMessage(Text.literal(head), false);
      }
    }

    variableResolvers.values().forEach(entry -> {
      float value = safeResolve(entry, pokemon);
      expr.setVariable(entry.key(), value);
      if (showVariablesInConsole) {
        String line = "Variable set: " + entry.key() + " = " + value;
        CobbleUtils.LOGGER_RAW.info(line);
        if (player != null) {
          player.sendMessage(Text.literal(line), false);
        }
      }
    });
    return expr;
  }

  private Expression getExpression() {
    registerVariables();
    ExpressionBuilder builder = new ExpressionBuilder((formula == null || formula.isEmpty()) ? "0" : formula);
    variableResolvers.values().forEach(entry -> builder.variable(entry.key()));

    if (showVariablesInConsole) {
      CobbleUtils.LOGGER_RAW.info("[PokemonFormula] Expression built for formula: {}", formula);
    }

    return builder.build();
  }

  // ---- Variable resolvers ----

  private float getHeldItem(Pokemon pokemon) {
    ItemStack itemStack = pokemon.heldItem();
    if (itemStack.isEmpty()) return 0f;
    String heldItem = itemStack.getItem().toString();
    BigDecimal price = heldItemPrice.getHeldItemPrices().getOrDefault(heldItem, heldItemPrice.getDefaultPrice());
    return price.floatValue();
  }

  private float getAspect(Pokemon pokemon) {
    float value = 0f;
    float aspectValue;
    for (String pokemonAspect : pokemon.getAspects()) {
      if (accumulationAspects) {
        aspectValue = aspect.getOrDefault(pokemonAspect, 0f);
        value += aspectValue;
      } else {
        aspectValue = aspect.getOrDefault(pokemonAspect, 0f);
        value = Math.max(value, aspectValue);
      }
    }
    return value;
  }

  private float getBase(Pokemon pokemon) {
    return pokemonBase.getOrDefault(pokemon.showdownId(), base);
  }

  private float getForm(Pokemon pokemon) {
    return form.getOrDefault(pokemon.getForm().formOnlyShowdownId(), 0f);
  }

  private float getBall(Pokemon pokemon) {
    var caughtBall = pokemon.getCaughtBall();
    String identifier = caughtBall.item().toString();
    float value = ball.getOrDefault(identifier, 0f);
    if (value != 0f) return value;
    return ball.getOrDefault(pokemon.getCaughtBall().getName().toTranslationKey(), 0f);
  }

  private float getGender(Pokemon pokemon) {
    return gender.getOrDefault(pokemon.getGender(), 0f);
  }

  private float getAbility(Pokemon pokemon) {
    return PokemonUtils.isAH(pokemon) ? hiddenAbility : notHiddenAbility;
  }

  private float getLabel(Pokemon pokemon) {
    var pokemonLabels = pokemon.getForm().getLabels();
    float value = 0f;
    for (String label : pokemonLabels) {
      if (accumulationLabels) {
        value += this.labels.getOrDefault(label, 0f);
      } else {
        value = Math.max(value, this.labels.getOrDefault(label, 0f));
      }
    }
    return value;
  }

  private float getNature(Pokemon pokemon) {
    Nature pokemonNature = pokemon.getNature();
    String identifier = pokemonNature.getName().toString();
    float value = nature.getOrDefault(identifier, 0f);
    if (value != 0f) return value;
    return nature.getOrDefault(pokemonNature.getDisplayName(), 0f);
  }

  private float getBreedable(Pokemon pokemon) {
    if (!pokemon.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED)) {
      return breedable.getOrDefault(PokemonUtils.isBreedable(pokemon), 0f);
    }
    return 0f;
  }

  private record VariableResolverEntry(
    String key,
    Function<Pokemon, Float> resolver,
    String description,
    VariableCategory category,
    int priority,
    String source
  ) {
    private VariableResolverEntry withSource(String updatedSource) {
      return new VariableResolverEntry(key, resolver, description, category, priority, updatedSource);
    }
  }

  /**
   * Helper class for held item prices.
   */
  @Data
  private static class HeldItemPrice {
    private BigDecimal defaultPrice = BigDecimal.ZERO;
    private Map<String, BigDecimal> heldItemPrices = new HashMap<>();

    public HeldItemPrice() {
      heldItemPrices.put("cobblemon:poke_ball", BigDecimal.valueOf(100));
      heldItemPrices.put("cobblemon:great_ball", BigDecimal.valueOf(200));
      heldItemPrices.put("cobblemon:ultra_ball", BigDecimal.valueOf(300));
    }
  }
}
