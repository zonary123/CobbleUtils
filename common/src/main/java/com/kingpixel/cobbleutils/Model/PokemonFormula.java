package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
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
  private boolean showVariablesInConsole = false;
  private String formula;
  private Expression expression;

  // Base configurations
  private float base = 0;
  private float shiny = 0;
  private float hiddenAbility = 0;

  private Map<String, Float> pokemonBase = new HashMap<>();
  private Map<String, Float> form = new HashMap<>();
  private Map<Gender, Float> gender = new EnumMap<>(Gender.class);
  private Map<String, Float> nature = new HashMap<>();
  private Map<String, Float> ball = new HashMap<>();
  private boolean accumulationAspects = false;
  private Map<String, Float> aspect = new HashMap<>();
  private boolean accumulationLabels = false;
  private Map<String, Float> labels = new HashMap<>();
  private Map<Boolean, Float> breedable = new HashMap<>();

  private HeldItemPrice heldItemPrice = new HeldItemPrice();

  // Dynamic variable resolvers: register any formula variable with a function
  transient
  private final Map<String, Function<Pokemon, Float>> variableResolvers = new HashMap<>();

  // Cache for performance - avoid recalculating the same Pokemon values
  transient
  private final Cache<Integer, @Nullable Double> pokemonResultCache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterAccess(Duration.ofMinutes(1))
    .removalListener((key, value, cause) -> {
      // Debug message when cache entries are removed
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("[DEBUG] Pokemon cache entry removed: " + key +
          " | Value: " + value + " | Cause: " + cause);
      }
    })
    .build();

  public PokemonFormula() {
    this.formula = "base + heldItem + gender + labels + nature + ability + ivsAverage + ivsTotal + evsTotal +" +
      " evsAverage + form + ball + aspect + shiny + breedable";

    initDefaults();
    registerVariables();
  }

  /**
   * Initializes default values for maps.
   */
  private void initDefaults() {
    pokemonBase.put("example", 0f);
    form.put("example", 0f);
    aspect.put("example", 0f);
    for (Gender g : Gender.values()) gender.put(g, 0f);
    nature.put("example", 0f);
    ball.put("cobblemon:poke_ball", 0f);
    labels.put("legendary", 0f);
    breedable.put(true, 0f);
    breedable.put(false, 0f);
  }

  /**
   * Registers all variable resolvers used inside the formula.
   * To add a new variable, simply put it here.
   */
  private void registerVariables() {
    variableResolvers.clear();
    variableResolvers.put("base", this::getBase);
    variableResolvers.put("heldItem", this::getHeldItem);
    variableResolvers.put("shiny", p -> p.getShiny() ? shiny : 1.0f);
    variableResolvers.put("gender", this::getGender);
    variableResolvers.put("labels", this::getLabel);
    variableResolvers.put("nature", this::getNature);
    variableResolvers.put("ability", this::getAbility);
    variableResolvers.put("form", this::getForm);
    variableResolvers.put("ball", this::getBall);
    variableResolvers.put("aspect", this::getAspect);
    variableResolvers.put("ivsAverage", p -> (float) Math.max(PokemonUtils.getIvsAverage(p.getIvs()), 1));
    variableResolvers.put("ivsTotal", p -> (float) Math.max(PokemonUtils.getIvsTotal(p.getIvs()), 1));
    variableResolvers.put("evsTotal", p -> (float) Math.max(PokemonUtils.getEvsTotal(p.getEvs()), 1));
    variableResolvers.put("evsAverage", p -> (float) Math.max(PokemonUtils.getEvsAverage(p.getEvs()), 1));
    variableResolvers.put("breedable", this::getBreedable);
    variableResolvers.put("friendship", p -> (float) Math.max(p.getFriendship(), 1));
    variableResolvers.put("level", p -> (float) Math.max(p.getLevel(), 1));

    if (CobbleUtils.config.isDebug() || showVariablesInConsole) {
      CobbleUtils.LOGGER.info("[DEBUG] Pokemon formula available variables: " + variableResolvers.keySet());
    }
  }

  /**
   * Gets the cached or calculated value of a Pokemon. (Pokemon not have a hashCode method so we cant use that for cache)
   *
   * @param pokemon The Pokemon to evaluate.
   *
   * @return The computed value.
   */
  public Double getPokemonValue(Pokemon pokemon) {
    return getPokemonExpression(pokemon).evaluate();
  }

  /**
   * Builds the Pokemon-specific expression with dynamic variables set.
   *
   * @param pokemon The Pokemon to evaluate.
   *
   * @return Expression ready to evaluate.
   */
  public Expression getPokemonExpression(Pokemon pokemon) {
    Expression expr = getExpression();
    if (CobbleUtils.config.isDebug() || showVariablesInConsole) {
      CobbleUtils.LOGGER.info("[DEBUG] Evaluating Pokemon: " + pokemon.getDisplayName().getString() +
        " | ID: " + pokemon.showdownId() + " | Hash: " + System.identityHashCode(pokemon));
    }
    variableResolvers.forEach((name, resolver) -> {
      float value = resolver.apply(pokemon);
      expr.setVariable(name, value);

      if (CobbleUtils.config.isDebug() || showVariablesInConsole) {
        CobbleUtils.LOGGER.info("[DEBUG] Variable set: " + name + " = " + value);
      }
    });
    return expr;
  }

  /**
   * Builds the base expression if not already built.
   *
   * @return The Expression object.
   */
  private Expression getExpression() {
    if (expression != null) return expression;
    registerVariables();
    ExpressionBuilder builder = new ExpressionBuilder(formula);
    variableResolvers.keySet().forEach(builder::variable);
    expression = builder.build();

    if (CobbleUtils.config.isDebug() || showVariablesInConsole) {
      CobbleUtils.LOGGER.info("[DEBUG] Expression built with formula: " + formula);
    }
    return expression;
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
    if(value != 0f) return value;
    return ball.getOrDefault(pokemon.getCaughtBall().getName().toTranslationKey(), 0f);
  }

  private float getGender(Pokemon pokemon) {
    return gender.getOrDefault(pokemon.getGender(), 0f);
  }

  private float getAbility(Pokemon pokemon) {
    return PokemonUtils.isAH(pokemon) ? hiddenAbility : 0f;
  }

  private float getLabel(Pokemon pokemon) {
    var labels = pokemon.getForm().getLabels();
    float value = 0f;
    for (String label : labels) {
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
    if(value != 0f) return value;
    return nature.getOrDefault(pokemon.getNature().getDisplayName(), 0f);
  }

  private float getBreedable(Pokemon pokemon) {
    if (!pokemon.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED)) {
      return breedable.getOrDefault(PokemonUtils.isBreedable(pokemon), 0f);
    }
    return 0f;
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
