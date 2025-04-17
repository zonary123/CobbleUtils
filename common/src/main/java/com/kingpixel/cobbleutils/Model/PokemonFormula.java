package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Carlos Varas Alonso - 19/03/2025 1:37
 */
@Data
public class PokemonFormula {
  private static final Map<String, Expression> expressions = new HashMap<>();
  private String formula = "base + gender + labels + nature + ability + ivsAverage + ivsTotal + evsTotal + evsAverage";
  private float base = 0;
  private float shiny = 0;
  private float ability = 0;
  private float hiddenAbility = 0;
  private Map<String, Float> pokemonBase = new HashMap<>();
  private Map<String, Float> form = new HashMap<>();
  private Map<String, Float> aspect = new HashMap<>();
  private Map<Gender, Float> gender = new EnumMap<>(Gender.class);
  private Map<String, Float> nature = new HashMap<>();
  private Map<String, Float> ball = new HashMap<>();
  private boolean accumulationLabels = false;
  private Map<String, Float> labels = new HashMap<>();

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
  }

  public static void removeFormula(String identifier) {
    expressions.remove(identifier);
  }

  public Expression getPokemonExpression(Pokemon pokemon, String identifier) {
    Expression expression = getExpression(identifier);
    expression.setVariable("base", getBase(pokemon));
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

    return expression;
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
    return PokemonUtils.isAH(pokemon) ? this.hiddenAbility : this.ability;
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