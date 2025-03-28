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
  private String formula = "base + gender + labels + nature + ability + ivsAverage + ivsTotal + evsTotal + evsAverage".intern();
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
    for (@NotNull Gender value : Gender.values()) {
      gender.put(value, 0.0F);
    }
  }

  public static void removeFormula(String identifier) {
    expressions.remove(identifier);
  }

  public Expression getPokemonExpression(Pokemon pokemon, String identifier) {
    Expression expression = getExpression(identifier);
    expression.setVariable("base", getBase(pokemon));
    expression.setVariable("shiny", pokemon.getShiny() ? shiny : 0.0);
    expression.setVariable("gender", getGender(pokemon));
    expression.setVariable("labels", getLabel(pokemon));
    expression.setVariable("nature", getNature(pokemon));
    expression.setVariable("ability", getAbility(pokemon));
    expression.setVariable("form", getForm(pokemon));
    expression.setVariable("ball", getBall(pokemon));
    expression.setVariable("aspect", getAspect(pokemon));
    expression.setVariable("ivsAverage", PokemonUtils.getIvsAverage(pokemon.getIvs()));
    expression.setVariable("ivsTotal", PokemonUtils.getIvsTotal(pokemon.getIvs()));
    expression.setVariable("evsTotal", PokemonUtils.getEvsTotal(pokemon.getEvs()));
    expression.setVariable("evsAverage", PokemonUtils.getEvsAverage(pokemon.getEvs()));
    return expression;
  }

  private float getAspect(Pokemon pokemon) {
    for (String pAspect : pokemon.getAspects()) {
      if (this.aspect.containsKey(pAspect)) return this.aspect.get(pAspect);
    }
    return 0;
  }

  private Expression getExpression(String identifier) {
    return expressions.computeIfAbsent(identifier.intern(), id -> {
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
    return pokemonBase.getOrDefault(pokemon.showdownId().intern(), this.base);
  }

  private float getForm(Pokemon pokemon) {
    return this.form.getOrDefault(pokemon.getForm().formOnlyShowdownId().intern(), 0.0f);
  }

  private float getBall(Pokemon pokemon) {
    return this.ball.getOrDefault(pokemon.getCaughtBall().getName().toTranslationKey().intern(), 0.0f);
  }

  private float getGender(Pokemon pokemon) {
    return gender.getOrDefault(pokemon.getGender(), 0.0f);
  }

  private float getAbility(Pokemon pokemon) {
    return PokemonUtils.isAH(pokemon) ? this.hiddenAbility : this.ability;
  }

  private float getLabel(Pokemon pokemon) {
    float value = 0;
    if (this.accumulationLabels) {
      for (String label : pokemon.getForm().getLabels()) {
        value += this.labels.getOrDefault(label.intern(), 0.0f);
      }
    } else {
      for (String label : pokemon.getForm().getLabels()) {
        value = Math.max(value, this.labels.getOrDefault(label.intern(), 0.0f));
      }
    }
    return value;
  }

  private float getNature(Pokemon pokemon) {
    return nature.getOrDefault(pokemon.getNature().getDisplayName().intern(), 0.0f);
  }
}