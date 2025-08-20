package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Carlos Varas Alonso - 19/03/2025 1:37
 */
@Data
public class PokemonFormula {

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

  private String formula = "base + heldItem + gender + labels + nature + ability + ivsAverage + ivsTotal + evsTotal +" +
    " evsAverage + form + ball + aspect + shiny + breedable";
  private float base = 0;
  private float shiny = 0;
  private float hiddenAbility = 0;
  private Map<String, Float> pokemonBase = new HashMap<>();
  private HeldItemPrice heldItemPrice = new HeldItemPrice();
  private Map<String, Float> form = new HashMap<>();
  private Map<String, Float> aspect = new HashMap<>();
  private Map<Gender, Float> gender = new EnumMap<>(Gender.class);
  private Map<String, Float> nature = new HashMap<>();
  private Map<String, Float> ball = new HashMap<>();
  private boolean accumulationLabels = false;
  private Map<String, Float> labels = new HashMap<>();
  private Map<Boolean, Float> breedable = new HashMap<>();

  public PokemonFormula() {
    pokemonBase.put("example", 0f);
    form.put("example", 0f);
    aspect.put("example", 0f);
    for (@NotNull Gender value : Gender.values()) {
      gender.put(value, 0.0F);
    }
    nature.put("example", 0f);
    ball.put("cobblemon:poke_ball", 0f);
    labels.put("legendary", 0f);
    breedable.put(true, 0f);
    breedable.put(false, 0f);
  }

  public static void removeFormula(String identifier) {
    expressions.remove(identifier);
  }

  // Cache for expressions to avoid re-parsing the same formula multiple times
  private static final Map<String, Expression> expressions = new ConcurrentHashMap<>();
  // Memoization cache for Pokémon results
  private static final Map<String, Map<Pokemon, Double>> pokemonResultCache = new ConcurrentHashMap<>();

  /**
   * Obtains the value of a Pokémon based on the provided identifier.
   * Uses memoization to cache results for performance.
   *
   * @param pokemon    The Pokémon to evaluate.
   * @param identifier The identifier for the formula to use.
   *
   * @return The calculated value for the Pokémon.
   */
  public Double getPokemonValue(Pokemon pokemon, String identifier) {
    Map<Pokemon, Double> cache = pokemonResultCache.computeIfAbsent(identifier, k -> new LinkedHashMap<>() {
      @Override
      protected boolean removeEldestEntry(Map.Entry<Pokemon, Double> eldest) {
        return size() > 5000;
      }
    });

    if (cache.containsKey(pokemon))
      return cache.get(pokemon);

    Expression expression = getPokemonExpression(pokemon, identifier);
    double result = expression.evaluate();

    cache.put(pokemon, result);
    return result;
  }

  public Expression getPokemonExpression(Pokemon pokemon, String identifier) {
    Expression expression = getExpression(identifier);

    // Variables dinámicas
    expression.setVariable("base", getBase(pokemon));
    expression.setVariable("heldItem", getHeldItem(pokemon));
    expression.setVariable("shiny", pokemon.getShiny() ? shiny : 1.0);
    expression.setVariable("gender", getGender(pokemon));
    expression.setVariable("labels", getLabel(pokemon));
    expression.setVariable("nature", getNature(pokemon));
    expression.setVariable("ability", getAbility(pokemon));
    expression.setVariable("form", getForm(pokemon));
    expression.setVariable("ball", getBall(pokemon));
    expression.setVariable("aspect", getAspect(pokemon));

    // IVs y EVs
    int ivsAverage = Math.max(PokemonUtils.getIvsAverage(pokemon.getIvs()), 1);
    int ivsTotal = Math.max(PokemonUtils.getIvsTotal(pokemon.getIvs()), 1);
    int evsTotal = Math.max(PokemonUtils.getEvsTotal(pokemon.getEvs()), 1);
    int evsAverage = Math.max(PokemonUtils.getEvsAverage(pokemon.getEvs()), 1);

    expression.setVariable("ivsAverage", ivsAverage);
    expression.setVariable("ivsTotal", ivsTotal);
    expression.setVariable("evsTotal", evsTotal);
    expression.setVariable("evsAverage", evsAverage);

    boolean isBreedable = PokemonUtils.isBreedable(pokemon);
    float resultBreedable = 0;
    if (!pokemon.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED)) {
      resultBreedable = breedable.getOrDefault(isBreedable, 0f);
    }
    expression.setVariable("breedable", resultBreedable);

    return expression;
  }

  private Expression getExpression(String identifier) {
    return expressions.computeIfAbsent(identifier.intern(), id -> {
      ExpressionBuilder builder = new ExpressionBuilder(this.formula)
        .variable("base")
        .variable("heldItem")
        .variable("shiny")
        .variable("gender")
        .variable("labels")
        .variable("nature")
        .variable("ability")
        .variable("form")
        .variable("ball")
        .variable("aspect")
        .variable("ivsAverage")
        .variable("ivsTotal")
        .variable("evsTotal")
        .variable("evsAverage")
        .variable("breedable");
      return builder.build();
    });
  }

  // Clase clave inmutable para la caché
  private record PokemonKey(Pokemon pokemon, String identifier) {

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      PokemonKey that = (PokemonKey) o;
      return pokemon.equals(that.pokemon) && identifier.equals(that.identifier);
    }

  }

  private float getHeldItem(Pokemon pokemon) {
    ItemStack itemStack = pokemon.heldItem();
    if (itemStack.isEmpty()) return 0;
    String heldItem = itemStack.getItem().toString();
    BigDecimal price = heldItemPrice.getHeldItemPrices().getOrDefault(heldItem, heldItemPrice.getDefaultPrice());
    return price.floatValue();
  }

  private float getAspect(Pokemon pokemon) {
    var aspects = pokemon.getAspects();
    for (String aspect : aspects) {
      if (this.aspect.containsKey(aspect)) {
        return this.aspect.get(aspect);
      }
    }
    return 0f;
  }

  private float getBase(Pokemon pokemon) {
    return pokemonBase.getOrDefault(pokemon.showdownId(), this.base);
  }

  private float getForm(Pokemon pokemon) {
    return this.form.getOrDefault(pokemon.getForm().formOnlyShowdownId(), 0.0f);
  }

  private float getBall(Pokemon pokemon) {
    return this.ball.getOrDefault(pokemon.getCaughtBall().getName().toTranslationKey(), 0.0f);
  }

  private float getGender(Pokemon pokemon) {
    return gender.getOrDefault(pokemon.getGender(), 0.0f);
  }

  private float getAbility(Pokemon pokemon) {
    return PokemonUtils.isAH(pokemon) ? this.hiddenAbility : 0.0f;
  }

  private float getLabel(Pokemon pokemon) {
    float result = 0f;
    for (String label : pokemon.getForm().getLabels()) {
      float value = this.labels.getOrDefault(label, 0f);
      if (this.accumulationLabels) {
        result += value;
      } else {
        result = Math.max(result, value);
      }
    }
    return result;
  }

  private float getNature(Pokemon pokemon) {
    return nature.getOrDefault(pokemon.getNature().getDisplayName(), 0.0f);
  }
}
