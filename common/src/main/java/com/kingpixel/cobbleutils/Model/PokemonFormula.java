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
import java.util.Map;

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

  private static final Map<String, Expression> expressions = new HashMap<>();
  private String formula = "base + heldItem + gender + labels + nature + ability + ivsAverage + ivsTotal + evsTotal +" +
    " " +
    "evsAverage + form + ball + aspect + shiny + breedable";
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

  public Expression getPokemonExpression(Pokemon pokemon, String identifier) {
    Expression expression = getExpression(identifier);
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

  private float getHeldItem(Pokemon pokemon) {
    ItemStack itemStack = pokemon.heldItem();
    if (itemStack.isEmpty()) return 0;
    String heldItem = itemStack.getItem().toString();
    BigDecimal price = heldItemPrice.getHeldItemPrices().getOrDefault(heldItem, heldItemPrice.getDefaultPrice());
    return price.floatValue();
  }

  private float getAspect(Pokemon pokemon) {
    return pokemon.getAspects().stream()
      .filter(this.aspect::containsKey)
      .map(this.aspect::get)
      .findFirst()
      .orElse(0f);
  }

  private Expression getExpression(String identifier) {
    return expressions.computeIfAbsent(identifier, id -> {
      ExpressionBuilder builder = new ExpressionBuilder(this.formula);
      builder.variable("base");
      builder.variable("heldItem");
      builder.variable("shiny");
      builder.variable("gender");
      builder.variable("labels");
      builder.variable("nature");
      builder.variable("ability");
      builder.variable("form");
      builder.variable("ball");
      builder.variable("aspect");
      builder.variable("ivsAverage");
      builder.variable("ivsTotal");
      builder.variable("evsTotal");
      builder.variable("evsAverage");
      builder.variable("breedable");
      return builder.build();
    });
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
    return pokemon.getForm().getLabels().stream()
      .map(label -> this.labels.getOrDefault(label, 0f))
      .reduce(0f, this.accumulationLabels ? Float::sum : Math::max);
  }

  private float getNature(Pokemon pokemon) {
    return nature.getOrDefault(pokemon.getNature().getDisplayName(), 0.0f);
  }
}