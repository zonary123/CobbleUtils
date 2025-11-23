package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.pokemon.evolution.PreEvolution;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.riding.stats.RidingStat;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
  // Base configurations
  private float base = 0;
  private float shiny = 0;
  private float hiddenAbility = 0;
  // Variable maps
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
  private Map<String, Map<Object, Float>> persistentDataValues = new HashMap<>();
  // Held item prices
  private HeldItemPrice heldItemPrice = new HeldItemPrice();

  // Dynamic variable resolvers: register any formula variable with a function
  private transient final Map<String, Function<Pokemon, Float>> variableResolvers = new HashMap<>();


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
    persistentDataValues.put(
      "example_data_1",
      Map.of(
        "common", 100f,
        "uncommon", 0f
      )
    );
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
   * To add a new variable, simply put it here.
   */
  private void registerVariables() {
    variableResolvers.clear();
    // Base stat value
    variableResolvers.put("base", this::getBase);
    // Held Item
    variableResolvers.put("heldItem", this::getHeldItem);
    // Shiny
    variableResolvers.put("shiny", p -> p.getShiny() ? shiny : 1.0f);
    // Gender
    variableResolvers.put("gender", this::getGender);
    // Labels
    variableResolvers.put("labels", this::getLabel);
    // Nature
    variableResolvers.put("nature", this::getNature);
    // Ability
    variableResolvers.put("ability", this::getAbility);
    // Form
    variableResolvers.put("form", this::getForm);
    // Ball
    variableResolvers.put("ball", this::getBall);
    // Aspect
    variableResolvers.put("aspect", this::getAspect);
    // Breedable
    variableResolvers.put("breedable", this::getBreedable);
    // Friendship
    variableResolvers.put("friendship", p -> (float) Math.max(p.getFriendship(), 1));
    // Level
    variableResolvers.put("level", p -> (float) Math.max(p.getLevel(), 1));
    // Evolutions count
    variableResolvers.put("evolutions", this::getEvolversCount);
    // Stats IVs and EVs
    variableResolvers.put("ivsTotal", p -> (float) Math.max(PokemonUtils.getIvsTotal(p.getIvs()), 1));
    variableResolvers.put("ivsAverage", p -> (float) Math.max(PokemonUtils.getIvsAverage(p.getIvs()), 1));
    variableResolvers.put("evsTotal", p -> (float) Math.max(PokemonUtils.getEvsTotal(p.getEvs()), 1));
    variableResolvers.put("evsAverage", p -> (float) Math.max(PokemonUtils.getEvsAverage(p.getEvs()), 1));
    for (Stats stats : PokemonUtils.STATS_LIST) {
      var showdownId = stats.getShowdownId();
      // IVs and EVs per stat
      variableResolvers.put("iv_" + showdownId, p -> (float) Math.max(p.getIvs().getOrDefault(stats), 1));
      variableResolvers.put("ev_" + showdownId, p -> (float) Math.max(p.getEvs().getOrDefault(stats), 1));
      // Hyper Trained IVs
      variableResolvers.put("ht_iv_" + showdownId,
        p -> {
          int iv = p.getIvs().getHyperTrainedIVs().getOrDefault(stats, 0);
          return (float) iv;
        });
    }

    // Riding stats
    for (RidingStat value : RidingStat.values()) {
      var showdownId = value.name();
      variableResolvers.put("riding_" + showdownId, p -> p.getRideBoost(value));
    }

    // Persistent data variables are registered dynamically during evaluation
    persistentDataValues.forEach((key, map) -> {
      variableResolvers.put(key, p -> {
        try {
          var persistentData = p.getPersistentData();
          var nbtElement = persistentData.get(key);
          if (nbtElement == null) return 0f;
          var nbtValue = Utils.convertNbtValue(nbtElement);
          if (nbtValue == null) return 0f;
          var value = map.getOrDefault(nbtValue, 0f);
          return value != null ? value : 0f;
        } catch (Exception e) {
          CobbleUtils.LOGGER.error("Error getting persistent data variable '" + key + "' for Pokemon: " + p.getDisplayName(false).getString());
          return 0f;
        }
      });
    });

    if (showVariablesInConsole) {
      CobbleUtils.LOGGER.info("Pokemon formula available variables: " + variableResolvers.keySet());
    }
  }


  private Float getEvolversCount(Pokemon pokemon) {
    int count = 0;
    PreEvolution preEvolution;
    do {
      preEvolution = pokemon.getPreEvolution();
      count++;
    } while (preEvolution != null && preEvolution != pokemon.getPreEvolution());
    return (float) count;
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
    //return pokemonResultCache.get(pokemon, p -> getPokemonExpression(p).evaluate());
  }

  private transient final Cache<Pokemon, @Nullable Double> pokemonResultCache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(Duration.ofSeconds(5))
    .removalListener((key, value, cause) -> {
      if (CobbleUtils.config.isDebug()) {
        CobbleUtils.LOGGER.info("PokemonFormula Cache eviction | Key: " + key + " | Value: " + value + " | Cause: " + cause);
      }
    })
    .build();


  /**
   * Builds the Pokemon-specific expression with dynamic variables set.
   *
   * @param pokemon The Pokemon to evaluate.
   *
   * @return Expression ready to evaluate.
   */
  public Expression getPokemonExpression(Pokemon pokemon) {
    ServerPlayerEntity player = pokemon.getOwnerPlayer();
    Expression expr = getExpression();
    if (showVariablesInConsole) {
      StringBuilder sb = new StringBuilder();
      sb.append("Evaluating Pokemon: ").append(pokemon.getDisplayName(false).getString()).append(" | ID: ").append(pokemon.showdownId()).append(" | Hash: ").append(System.identityHashCode(pokemon));
      CobbleUtils.LOGGER.info(sb.toString());
      if (player != null) {
        player.sendMessage(Text.literal(sb.toString()));
      }
    }
    variableResolvers.forEach((name, resolver) -> {
      float value = resolver.apply(pokemon);
      expr.setVariable(name, value);
      if (showVariablesInConsole) {
        StringBuilder sb = new StringBuilder();
        sb.append("Variable set: ").append(name).append(" = ").append(value);
        CobbleUtils.LOGGER.info(sb.toString());
        if (player != null) {
          player.sendMessage(Text.literal(sb.toString()));
        }
      }
    });
    return expr;
  }


  /**
   * Builds the base expression if not already built.
   *
   * @return The Expression object.
   */
  private transient ExpressionBuilder baseBuilder;

  private Expression getExpression() {
    // Reusar el builder si ya está creado
    if (baseBuilder == null) {
      // Solo registramos las variables una vez
      registerVariables();

      // Creamos el builder y registramos todas las variables disponibles
      baseBuilder = new ExpressionBuilder(formula);
      variableResolvers.keySet().forEach(baseBuilder::variable);

      if (showVariablesInConsole) {
        CobbleUtils.LOGGER.info("[PokemonFormula] ExpressionBuilder created for formula: " + formula);
      }
    }

    // Cada .build() devuelve una expresión independiente (segura en hilos)
    Expression expr = baseBuilder.build();

    if (showVariablesInConsole) {
      CobbleUtils.LOGGER.info("[PokemonFormula] Expression built instance ready for evaluation.");
    }

    return expr;
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
    if (value != 0f) return value;
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
