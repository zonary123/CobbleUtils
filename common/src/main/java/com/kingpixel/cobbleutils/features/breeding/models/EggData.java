package com.kingpixel.cobbleutils.features.breeding.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.moves.BenchedMove;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.pokemon.feature.ChoiceSpeciesFeatureProvider;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatureProvider;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatures;
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.item.CobblemonItem;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbilityType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import com.kingpixel.cobbleutils.Model.PokemonChance;
import com.kingpixel.cobbleutils.Model.PokemonData;
import com.kingpixel.cobbleutils.Model.ScalePokemonData;
import com.kingpixel.cobbleutils.events.ScaleEvent;
import com.kingpixel.cobbleutils.features.breeding.events.HatchEggEvent;
import com.kingpixel.cobbleutils.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.cobblemon.mod.common.CobblemonItems.*;

/**
 * @author Carlos Varas Alonso - 23/07/2024 23:01
 */
@Getter
@Setter
@ToString
public class EggData {
  private String species;
  private int level;
  private double steps;
  private int cycles;
  private String ability;
  private String size;
  private String form;
  private String moves;
  private String ball;
  private String nature;
  private boolean random;
  private int HP;
  private int Attack;
  private int Defense;
  private int SpecialAttack;
  private int SpecialDefense;
  private int Speed;

  public static void convertToEgg(PokemonEntity pokemonEntity) {
    Pokemon pokemon = pokemonEntity.getPokemon();

    if (pokemon.hasLabels(
      CobblemonPokemonLabels.LEGENDARY,
      CobblemonPokemonLabels.ULTRA_BEAST,
      CobblemonPokemonLabels.MYTHICAL,
      CobblemonPokemonLabels.PARADOX
    )) return;
    Pokemon eggSpecie = PokemonUtils.getEvolutionPokemonEgg(pokemon.getSpecies());

    // Form
    String form = getForm(pokemon);

    Pokemon firstEvolution = PokemonProperties.Companion.parse(eggSpecie.getSpecies().showdownId() + " " + form).create();

    pokemon.setLevel(1);
    pokemon.getPersistentData().putBoolean("SpawnEgg", true);
    pokemon.getPersistentData().putString("species", firstEvolution.showdownId());
    pokemon.getPersistentData().putInt("level", 1);
    pokemon.getPersistentData().putDouble("steps", CobbleUtils.breedconfig.getSteps());
    pokemon.getPersistentData().putInt("cycles", firstEvolution.getSpecies().getEggCycles());
    pokemon.getPersistentData().putString("ability", firstEvolution.getAbility().getName());
    pokemon.getPersistentData().putString("form", form);

    String type_egg;
    if (CobbleUtils.breedconfig.isAspectEggByType()) {
      type_egg = pokemon.getPrimaryType().getName().toLowerCase();
    } else {
      type_egg = pokemon.getSpecies().showdownId();
    }

    PokemonProperties.Companion.parse("egg type_egg=" + type_egg).apply(pokemon);

    pokemonEntity.setAiDisabled(true);
    pokemonEntity.setMovementSpeed(0);
    ((Entity) pokemonEntity).setInvulnerable(true);
    pokemonEntity.setNoGravity(true);
  }

  public void EggToPokemon(ServerPlayerEntity player, Pokemon egg) {
    var party = Cobblemon.INSTANCE.getStorage().getParty(player);
    var pc = Cobblemon.INSTANCE.getStorage().getPC(player);
    if (egg.getPersistentData().getBoolean("Hatched")) {
      PlayerUtils.sendMessage(
        player,
        "Please delete this egg is a error",
        CobbleUtils.breedconfig.getPrefix(),
        TypeMessage.CHAT
      );
      party.remove(egg);
      pc.remove(egg);
      return;
    }
    String pokemonId = species == null || species.isEmpty() ? "rattata" : species;
    AbilityTemplate abilityTemplate;
    if (ability.isEmpty()) {
      abilityTemplate =
        PokemonUtils.getRandomAbility(PokemonProperties.Companion.parse(pokemonId + " " + form).create()).getTemplate();
    } else {
      abilityTemplate = Abilities.INSTANCE.get(this.ability);
    }

    PokemonProperties pokemonProperties = PokemonProperties.Companion.parse(pokemonId + " " + form + " ability=" + abilityTemplate.getName());


    Pokemon pokemon = pokemonProperties.create();

    pokemon.getPersistentData().copyFrom(egg.getPersistentData());
    egg.getPersistentData().putBoolean("Hatched", true);
    pokemon.setShiny(egg.getShiny());
    party.remove(egg);
    pc.remove(egg);


    Nature nature;
    try {
      nature = Natures.INSTANCE.getNature(this.nature);
      if (nature == null) {
        nature = Natures.INSTANCE.getRandomNature();
        CobbleUtils.LOGGER.error("Error to get Nature: " + this.nature);
      }
    } catch (Exception e) {
      nature = Natures.INSTANCE.getRandomNature();
      CobbleUtils.LOGGER.error("Error to get Nature: " + nature + " - " + e.getMessage());
    }
    pokemon.setNature(nature);

    pokemon.setLevel(level);
    pokemon.heal();

    if (moves != null && !moves.isEmpty()) {
      try {
        // Parsear el JSON string como un JsonObject
        JsonObject jsonObject = JsonParser.parseString(moves).getAsJsonObject();

        // Obtener el JsonArray bajo la clave "moves"
        JsonArray jsonArray = jsonObject.getAsJsonArray("moves");
        for (JsonElement element : jsonArray) {
          MoveTemplate moveTemplate = Moves.INSTANCE.getByName(element.getAsString());
          if (moveTemplate == null) continue;
          Move move = moveTemplate.create();
          JsonObject moveJson = move.saveToJSON(new JsonObject());
          BenchedMove benchedMove = BenchedMove.Companion.loadFromJSON(moveJson);
          pokemon.getBenchedMoves().add(benchedMove);
        }
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Error to process JSON ARRAY: " + e.getMessage());
      }
    }


    removePersistent(pokemon);
    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Egg to Pokemon: " + pokemon.getSpecies().showdownId());
    }
    party.add(pokemon);
    HatchEggEvent.HATCH_EGG_EVENT.emit(player, pokemon);
  }

  private void removePersistent(Pokemon pokemon) {
    pokemon.getPersistentData().remove("species");
    pokemon.getPersistentData().remove("level");
    pokemon.getPersistentData().remove("steps");
    pokemon.getPersistentData().remove("nature");
    pokemon.getPersistentData().remove("ability");
    pokemon.getPersistentData().remove("cycles");
    pokemon.getPersistentData().remove("form");
    pokemon.getPersistentData().remove("random");
    pokemon.getPersistentData().remove("moves");
    pokemon.getPersistentData().remove("nature");
    if (!ball.isEmpty()) {
      try {
        PokeBall pokeBall = PokeBalls.INSTANCE.getPokeBall(Identifier.of(ball));
        pokemon.setCaughtBall(pokeBall);
      } catch (Exception ignored) {
        if (CobbleUtils.config.isDebug()) {
          CobbleUtils.LOGGER.error("Error to get PokeBall: " + ball);
        }
      }
    }
    pokemon.getPersistentData().remove("ball");
    applyPersistentIvs(pokemon, "HP", Stats.HP);
    applyPersistentIvs(pokemon, "Attack", Stats.ATTACK);
    applyPersistentIvs(pokemon, "Defense", Stats.DEFENCE);
    applyPersistentIvs(pokemon, "SpecialAttack", Stats.SPECIAL_ATTACK);
    applyPersistentIvs(pokemon, "SpecialDefense", Stats.SPECIAL_DEFENCE);
    applyPersistentIvs(pokemon, "Speed", Stats.SPEED);
  }

  private static void applyPersistentIvs(Pokemon pokemon, String tag, Stats stats) {
    if (pokemon.getPersistentData().contains(tag)) {
      pokemon.setIV(stats, pokemon.getPersistentData().getInt(tag));
      pokemon.getPersistentData().remove(tag);
    }
  }


  public static EggData from(Pokemon pokemon) {
    if (pokemon == null) return null;
    EggData eggData = new EggData();
    eggData.setSpecies(pokemon.getPersistentData().getString("species"));
    eggData.setLevel(pokemon.getPersistentData().getInt("level"));
    eggData.setSteps(pokemon.getPersistentData().getDouble("steps"));
    eggData.setAbility(pokemon.getPersistentData().getString("ability"));
    eggData.setCycles(pokemon.getPersistentData().getInt("cycles"));
    eggData.setSize(pokemon.getPersistentData().getString("size"));
    eggData.setForm(pokemon.getPersistentData().getString("form"));
    eggData.setRandom(pokemon.getPersistentData().getBoolean("random"));
    eggData.setMoves(pokemon.getPersistentData().getString("moves"));
    eggData.setBall(pokemon.getPersistentData().getString("ball"));
    eggData.setNature(pokemon.getPersistentData().getString("nature"));
    if (pokemon.getPersistentData().contains("HP")) {
      eggData.setHP(pokemon.getPersistentData().getInt("HP"));
    }
    if (pokemon.getPersistentData().contains("Attack")) {
      eggData.setAttack(pokemon.getPersistentData().getInt("Attack"));
    }
    if (pokemon.getPersistentData().contains("Defense")) {
      eggData.setDefense(pokemon.getPersistentData().getInt("Defense"));
    }
    if (pokemon.getPersistentData().contains("SpecialAttack")) {
      eggData.setSpecialAttack(pokemon.getPersistentData().getInt("SpecialAttack"));
    }
    if (pokemon.getPersistentData().contains("SpecialDefense")) {
      eggData.setSpecialDefense(pokemon.getPersistentData().getInt("SpecialDefense"));
    }
    if (pokemon.getPersistentData().contains("Speed")) {
      eggData.setSpeed(pokemon.getPersistentData().getInt("Speed"));
    }
    return eggData;
  }


  public void steps(ServerPlayerEntity player, Pokemon pokemon, double stepsremove) {
    this.steps -= stepsremove;

    if (steps <= 0) {
      this.cycles--;
      this.steps = getMaxStepsPerCycle();
      pokemon.setNickname(Text.literal("Egg " + cycles + "/" + (int) steps));
    }

    updateSteps(pokemon);

    if (this.cycles <= 0) EggToPokemon(player, pokemon);
  }

  private int getMaxStepsPerCycle() {
    if (cycles > 0) {
      return CobbleUtils.breedconfig.getSteps();
    } else {
      return 0;
    }
  }

  private void updateSteps(Pokemon pokemon) {
    pokemon.getPersistentData().putDouble("steps", this.steps);
    pokemon.getPersistentData().putInt("cycles", this.cycles);
    pokemon.setCurrentHealth(0);
    pokemon.setHealTimer(0);

    if ((int) steps % 16 == 0) {
      String stepsFormat = String.format("%.0f", this.steps);
      String extraInfo = "";
      if (CobbleUtils.breedconfig.isExtraInfo()) {
        extraInfo = this.cycles + "/" + stepsFormat;
      }
      if (random) {
        pokemon.setNickname(
          Text.literal(CobbleUtils.breedconfig.getNameRandomEgg() + " " + extraInfo));
      } else {
        pokemon.setNickname(Text.literal(CobbleUtils.breedconfig.getNameEgg()
          .replace("%pokemon%", species) + " " + extraInfo));
      }
    }

  }

  public static Pokemon createEgg(
    Pokemon male, Pokemon female, ServerPlayerEntity player, PlotBreeding plotBreeding)
    throws NoPokemonStoreException {

    if (plotBreeding.getEggs().size() >= CobbleUtils.breedconfig.getMaxeggperplot())
      return null;

    return createEgg(male, female, player);
  }

  public static Pokemon createEgg(
    Pokemon male, Pokemon female, ServerPlayerEntity player) throws NoPokemonStoreException {


    if (isDitto(female)) {
      Pokemon temp = male;
      male = female;
      female = temp;
    }

    Pokemon usePokemonToEgg;
    Pokemon egg;
    boolean random = false;

    if (isDitto(male)) {
      if (isDitto(female)) {
        if (!CobbleUtils.breedconfig.isDoubleditto())
          return null;
        do {
          usePokemonToEgg = CobbleUtils.breedconfig.getPokemonsForDoubleDitto().generateRandomPokemon(CobbleUtils.MOD_ID, "breeding");
        } while (usePokemonToEgg.isUltraBeast() || usePokemonToEgg.isLegendary());
        random = true;
      } else {
        if (!CobbleUtils.breedconfig.isDitto())
          return null;
        usePokemonToEgg = female;
      }
    } else if (male.getSpecies().showdownId().equalsIgnoreCase(female.getSpecies().showdownId())) {
      usePokemonToEgg = female;
    } else {
      if (isCompatible(male, female)) {
        usePokemonToEgg = female;
      } else {
        return null;
      }
    }

    egg = EggData.pokemonToEgg(usePokemonToEgg, false, female);

    if (!egg.showdownId().equalsIgnoreCase("egg")) {
      player.sendMessage(
        AdventureTranslator.toNative(
          "%prefix% The CobbleUtils datapack is not installed, please notify the Owner/Admin about this.",
          CobbleUtils.breedconfig.getPrefix()
        )
      );
      return PokemonProperties.Companion.parse("rattata").create();
    }

    if (random) egg.getPersistentData().putBoolean("random", true);


    mechanicsLogic(male, female, usePokemonToEgg, egg);


    ScaleEvent.solveScale(egg);

    PlayerUtils.sendMessage(player, PokemonUtils.replace(CobbleUtils.breedconfig.getCreateEgg()
        .replace("%egg%", egg.getPersistentData().getString("species")),
      List.of(male, female, egg)), CobbleUtils.breedconfig.getPrefix());

    return egg;
  }


  public static boolean isCompatible(Pokemon male, Pokemon female) {
    if (male.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED)) return false;
    if (female.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED)) return false;
    if (isDitto(male) || isDitto(female)) return true;
    return female.getForm().getEggGroups().stream()
      .anyMatch(eggGroup -> male.getForm().getEggGroups().contains(eggGroup));
  }

  public static boolean isDitto(Pokemon pokemon) {
    if (pokemon == null) return false;
    return pokemon.getSpecies().showdownId().equalsIgnoreCase("ditto") ||
      pokemon.getForm().getEggGroups().contains(EggGroup.DITTO);
  }

  private static void mechanicsLogic(Pokemon male, Pokemon female, Pokemon usePokemonToEgg, Pokemon egg) {
    // Especie para el huevo
    Pokemon eggSpecie = PokemonUtils.getEvolutionPokemonEgg(usePokemonToEgg.getSpecies());

    // Form
    String form = getForm(female);

    Pokemon firstEvolution = PokemonProperties.Companion.parse(eggSpecie.getSpecies().showdownId() + " " + form).create();


    // IVS
    applyInitialIvs(egg, male, female);

    // Nature (Done)
    applyNature(male, female, egg);

    // Ability (Done)
    applyAbility(male, female, firstEvolution, egg);

    // Shiny Rate (Done)
    applyShinyRate(male, female, egg);

    // PokeBall (Done)
    applyPokeBall(male, female, egg);

    // Egg Moves (Done)
    applyEggMoves(male, female, firstEvolution, egg);

    egg.getPersistentData().putString("Date_Breed", new Date().toString());
  }

  private static List<String> getMoves(Pokemon pokemon) {
    List<String> s = new ArrayList<>();
    pokemon.getMoveSet().forEach(move -> s.add(move.getName()));
    pokemon.getBenchedMoves().forEach(move -> s.add(move.getMoveTemplate().getName()));
    return s;
  }

  private static void applyEggMoves(Pokemon male, Pokemon female, Pokemon firstEvolution, Pokemon egg) {
    if (Utils.RANDOM.nextDouble(100) >= CobbleUtils.breedconfig.getSuccessItems().getPercentageEggMoves()) return; //
    // default 0%
    List<String> moves = new ArrayList<>(getMoves(male));
    moves.addAll(getMoves(female));


    List<String> names = new ArrayList<>();
    for (MoveTemplate eggMove : firstEvolution.getForm().getMoves().getEggMoves()) {
      if (moves.contains(eggMove.getName())) {
        names.add(eggMove.getName());
      }
    }

    if (!names.isEmpty()) {
      JsonArray jsonArray = new JsonArray();
      names.forEach(jsonArray::add);

      JsonObject jsonObject = new JsonObject();
      jsonObject.add("moves", jsonArray);

      egg.getPersistentData().putString("moves", jsonObject.toString());
    }
  }

  private static void applyPokeBall(Pokemon male, Pokemon female, Pokemon egg) {
    if (CobbleUtils.breedconfig.isObtainPokeBallFromMother()) {
      if (female.getSpecies().showdownId().equalsIgnoreCase(male.getSpecies().showdownId())) {
        if (Utils.RANDOM.nextBoolean()) {
          egg.setCaughtBall(male.getCaughtBall());
        } else {
          egg.setCaughtBall(female.getCaughtBall());
        }
      } else {
        egg.setCaughtBall(female.getCaughtBall());
      }
      Identifier pokeBall = female.getCaughtBall().getName();
      egg.getPersistentData().putString("ball", pokeBall.getNamespace() + ":" + pokeBall.getPath());
    }
  }

  private static void applyShinyRate(Pokemon male, Pokemon female, Pokemon egg) {
    float shinyrate = Cobblemon.INSTANCE.getConfig().getShinyRate();
    float multiplier = CobbleUtils.breedconfig.getMultiplierShiny();

    if (multiplier > 0) {
      if (male.getShiny())
        shinyrate /= multiplier;
      if (female.getShiny())
        shinyrate /= multiplier;
    }

    if (CobbleUtils.breedconfig.isMethodmasuda()) {
      String maleCountry = male.getPersistentData().getString(CobbleUtilsTags.COUNTRY_TAG);
      String femaleCountry = female.getPersistentData().getString(CobbleUtilsTags.COUNTRY_TAG);
      if (!maleCountry.isEmpty() && !femaleCountry.isEmpty()) {
        if (!maleCountry.equalsIgnoreCase(femaleCountry)) {
          shinyrate /= CobbleUtils.breedconfig.getMultipliermasuda();
        }
      }
    }

    shinyrate = (int) Math.max(1, shinyrate);
    if (shinyrate <= 1) {
      egg.setShiny(true);
    } else {
      egg.setShiny(Utils.RANDOM.nextInt((int) shinyrate) == 0);
    }
  }

  private static void applyNature(Pokemon male, Pokemon female, Pokemon egg) {
    boolean hasDoubleEverstone = male.heldItem().getItem() == EVERSTONE && female.heldItem().getItem() == EVERSTONE;
    boolean hasEverstone = male.heldItem().getItem() == EVERSTONE || female.heldItem().getItem() == EVERSTONE;
    boolean isSuccess = Utils.RANDOM.nextDouble() * 100 <= CobbleUtils.breedconfig.getSuccessItems().getPercentageEverStone();

    if (isSuccess && hasEverstone) {
      if (hasDoubleEverstone) {
        egg.setNature(Utils.RANDOM.nextBoolean() ? male.getNature() : female.getNature());
      } else {
        egg.setNature(male.heldItem().getItem() == EVERSTONE ? male.getNature() : female.getNature());
      }
    } else {
      egg.setNature(Natures.INSTANCE.getRandomNature());
    }
    egg.getPersistentData().putString("nature", egg.getNature().getName().getPath());
  }


  private static final List<Stats> stats =
    new ArrayList<>(Arrays.stream(Stats.values()).filter(stats1 -> stats1 != Stats.EVASION && stats1 != Stats.ACCURACY).toList());

  /**
   * Apply the initial IVs to the egg
   *
   * @param egg    The egg
   * @param male   The male pokemon
   * @param female The female pokemon
   */
  private static void applyInitialIvs(Pokemon egg, Pokemon male, Pokemon female) {
    List<Pokemon> pokemons = List.of(male, female);
    List<Pokemon> bracelets = new ArrayList<>();
    List<Stats> cloneStats = new ArrayList<>(stats);

    int numIvsToTransfer = CobbleUtils.breedconfig.getDefaultNumIvsToTransfer();

    for (Pokemon pokemon : pokemons) {
      if (pokemon.heldItem().getItem() instanceof CobblemonItem item) {
        if (isPowerItem(item)) {
          bracelets.add(pokemon);
        } else if (item.equals(DESTINY_KNOT)) {
          numIvsToTransfer = CobbleUtils.breedconfig.getNumberIvsDestinyKnot();
        }
      }
    }


    for (Pokemon bracelet : bracelets) {
      applyIvsPower(bracelet, egg, (CobblemonItem) bracelet.heldItem().getItem(), cloneStats);
      numIvsToTransfer--;
    }
    applyIvs(male, female, egg, numIvsToTransfer, cloneStats);

    cloneStats.forEach(rStats -> {
      int random = Utils.RANDOM.nextInt(CobbleUtils.breedconfig.getMaxIvsRandom() + 1);
      if (CobbleUtils.breedconfig.isShowIvs()) {
        egg.setIV(rStats, random);
      } else {
        egg.setIV(rStats, 0);
      }
      egg.getPersistentData().putInt(getName(rStats), random);
    });
  }

  private static void applyIvs(Pokemon male, Pokemon female, Pokemon egg, int amount, List<Stats> stats) {
    if (Utils.RANDOM.nextDouble(100) >= CobbleUtils.breedconfig.getSuccessItems().getPercentageDestinyKnot() && haveDestinyKnot(female, male))
      return;

    for (int i = 0; i < amount; i++) {
      Stats stat = stats.remove(Utils.RANDOM.nextInt(stats.size()));
      Pokemon selectedParent = Utils.RANDOM.nextBoolean() ? male : female;
      if (CobbleUtils.breedconfig.isShowIvs()) {
        egg.setIV(stat, selectedParent.getIvs().getOrDefault(stat));
      } else {
        egg.setIV(stat, 0);
      }
      egg.getPersistentData().putInt(getName(stat), selectedParent.getIvs().getOrDefault(stat));
    }
  }

  private static String getName(Stats stats) {
    return switch (stats) {
      case HP -> "HP";
      case ATTACK -> "Attack";
      case DEFENCE -> "Defense";
      case SPECIAL_ATTACK -> "SpecialAttack";
      case SPECIAL_DEFENCE -> "SpecialDefense";
      case SPEED -> "Speed";
      default -> "";
    };
  }

  private static boolean haveDestinyKnot(Pokemon female, Pokemon male) {
    return female.heldItem().getItem().equals(DESTINY_KNOT) || male.heldItem().getItem().equals(DESTINY_KNOT);
  }


  private static boolean isPowerItem(CobblemonItem item) {
    if (item == null) return false;
    return item.equals(POWER_WEIGHT) || item.equals(POWER_BRACER) || item.equals(POWER_BELT) || item.equals(POWER_ANKLET) || item.equals(POWER_LENS) || item.equals(POWER_BAND);
  }

  private static void applyIvsPower(Pokemon select, Pokemon egg, CobblemonItem bracelet, List<Stats> stats) {
    if (bracelet == null) return;
    if (Utils.RANDOM.nextDouble(100) >= CobbleUtils.breedconfig.getSuccessItems().getPercentagePowerItem()) return;
    Stats stat = null;
    if (bracelet.equals(POWER_WEIGHT)) {
      stat = Stats.HP;
      if (CobbleUtils.breedconfig.isShowIvs()) {
        egg.setIV(stat, select.getIvs().getOrDefault(stat));
      } else {
        egg.setIV(stat, 0);
      }
      egg.getPersistentData().putInt("HP", select.getIvs().getOrDefault(stat));
    } else if (bracelet.equals(POWER_BRACER)) {
      stat = Stats.ATTACK;
      if (CobbleUtils.breedconfig.isShowIvs()) {
        egg.setIV(stat, select.getIvs().getOrDefault(stat));
      } else {
        egg.setIV(stat, 0);
      }
      egg.getPersistentData().putInt("Attack", select.getIvs().getOrDefault(stat));
    } else if (bracelet.equals(POWER_BELT)) {
      stat = Stats.DEFENCE;
      if (CobbleUtils.breedconfig.isShowIvs()) {
        egg.setIV(stat, select.getIvs().getOrDefault(stat));
      } else {
        egg.setIV(stat, 0);
      }
      egg.getPersistentData().putInt("Defense", select.getIvs().getOrDefault(stat));
    } else if (bracelet.equals(POWER_ANKLET)) {
      stat = Stats.SPEED;
      if (CobbleUtils.breedconfig.isShowIvs()) {
        egg.setIV(stat, select.getIvs().getOrDefault(stat));
      } else {
        egg.setIV(stat, 0);
      }
      egg.getPersistentData().putInt("Speed", select.getIvs().getOrDefault(stat));
    } else if (bracelet.equals(POWER_LENS)) {
      stat = Stats.SPECIAL_ATTACK;
      if (CobbleUtils.breedconfig.isShowIvs()) {
        egg.setIV(stat, select.getIvs().getOrDefault(stat));
      } else {
        egg.setIV(stat, 0);
      }
      egg.getPersistentData().putInt("SpecialAttack", select.getIvs().getOrDefault(stat));
    } else if (bracelet.equals(POWER_BAND)) {
      stat = Stats.SPECIAL_DEFENCE;
      if (CobbleUtils.breedconfig.isShowIvs()) {
        egg.setIV(stat, select.getIvs().getOrDefault(stat));
      } else {
        egg.setIV(stat, 0);
      }
      egg.getPersistentData().putInt("SpecialDefense", select.getIvs().getOrDefault(stat));
    }
    stats.remove(stat);
  }

  private static void applyAbility(Pokemon male, Pokemon female, Pokemon firstEvolution, Pokemon egg) {
    // Double ditto
    if (isDitto(male) && isDitto(female)) {
      Ability randomAbility = PokemonUtils.getRandomAbility(firstEvolution);
      egg.getPersistentData().putString("ability", randomAbility.getName());
      return;
    }

    if (isDitto(female)) {
      egg.getPersistentData().putString("ability", PokemonUtils.getRandomAbility(firstEvolution).getName());
      return;
    }

    List<Ability> normalAbilities = new ArrayList<>();
    List<Ability> hiddenAbilities = new ArrayList<>();

    for (PotentialAbility ability : firstEvolution.getForm().getAbilities()) {
      if (ability.getType() instanceof HiddenAbilityType) {
        hiddenAbilities.add(ability.getTemplate().create(false, Priority.NORMAL));
      } else {
        normalAbilities.add(ability.getTemplate().create(false, Priority.NORMAL));
      }
    }

    boolean femaleAh = PokemonUtils.isAH(female);
    boolean success = Utils.RANDOM.nextDouble(100) <= CobbleUtils.breedconfig.getSuccessItems().getPercentageTransmitAH();

    if (femaleAh && success) {
      egg.getPersistentData().putString("ability", PokemonUtils.getAH(firstEvolution).getName());
    } else {
      Ability femaleAbility = female.getAbility();
      if (normalAbilities.contains(femaleAbility)) {
        double probability = normalAbilities.size() == 2 ? 80.0 : 100.0;
        if (Utils.RANDOM.nextDouble(100) <= probability) {
          egg.getPersistentData().putString("ability", femaleAbility.getName());
        } else {
          normalAbilities.remove(femaleAbility);
          egg.getPersistentData().putString("ability", normalAbilities.get(Utils.RANDOM.nextInt(normalAbilities.size())).getName());
        }
      } else {
        egg.getPersistentData().putString("ability", PokemonUtils.getRandomAbility(firstEvolution).getName());
      }
    }
  }

  private static Pokemon pokemonToEgg(Pokemon usePokemon, boolean dittos, Pokemon female) {
    String specie = getExcepcionalSpecie(usePokemon);
    return EggData.applyPersistent(usePokemon, specie, dittos, female);
  }


  private static String getExcepcionalSpecie(Pokemon pokemon) {
    if (CobbleUtils.breedconfig.getIncenses().isEmpty()) return null;
    String s = null;
    for (Incense incense : CobbleUtils.breedconfig.getIncenses()) {
      s = incense.getChild(pokemon);
      if (s != null) break;
    }
    return s;
  }

  @Getter
  public static class EggForm {
    private String form;
    private List<String> pokemons;

    public EggForm(String form, List<String> pokemons) {
      this.form = form;
      this.pokemons = pokemons;
    }
  }

  @Getter
  public static class EggSpecialForm {
    private String form;
    private List<PokemonData> pokemons;

    public EggSpecialForm(String form, List<PokemonData> pokemons) {
      this.form = form;
      this.pokemons = pokemons;
    }
  }


  @Getter
  @Setter
  public static class PokemonRareMecanic {
    private List<PokemonChance> pokemons;

    public PokemonRareMecanic(List<PokemonChance> pokemons) {
      this.pokemons = pokemons;
    }
  }

  private static String getForm(Pokemon pokemon) {
    StringBuilder form = new StringBuilder();


    switch (pokemon.getSpecies().showdownId()) {
      case "perrserker":
      case "sirfetchd":
      case "mrrime":
      case "cursola":
      case "obstagoon":
      case "runerigus":
        return "galarian";
      case "clodsire":
        return "paldean";
      case "overqwil":
      case "sneasler":
        return "hisuian";
    }


    String configForm = null;

    for (EggForm eggForm : CobbleUtils.breedconfig.getEggForms()) {
      if (eggForm.getPokemons().contains(pokemon.showdownId())) {
        configForm = eggForm.getForm();
        break;
      }
    }


    if (configForm != null) {
      if (CobbleUtils.breedconfig.getBlacklistForm().contains(configForm)) configForm = "";
      return configForm;
    }

    List<String> aspects = pokemon.getForm().getAspects();

    if (aspects.isEmpty()) {
      form = new StringBuilder();
    } else {
      form.append(aspects.getFirst());
    }


    form = new StringBuilder(form.toString().replace("-", "_"));

    int lastUnderscoreIndex = form.lastIndexOf("_");

    if (lastUnderscoreIndex != -1) {
      form = new StringBuilder(form.substring(0, lastUnderscoreIndex) + "=" + form.substring(lastUnderscoreIndex + 1));
    }


    // Form Regional
    for (String label : pokemon.getForm().getLabels()) {
      if (label.contains("regional")) {
        form.append(" ").append("region_bias=").append(pokemon.getForm().formOnlyShowdownId());
      }
    }

    for (SpeciesFeatureProvider<?> speciesFeatureProvider : SpeciesFeatures.INSTANCE.getFeaturesFor(pokemon.getSpecies())) {
      if (speciesFeatureProvider instanceof ChoiceSpeciesFeatureProvider choice) {
        var choiceForm = choice.get(pokemon);
        if (choiceForm != null) {
          String name = choiceForm.getName();
          String value = choiceForm.getValue();
          if (CobbleUtils.breedconfig.getBlacklistFeatures().contains(name)
            || CobbleUtils.breedconfig.getBlacklistFeatures().contains(value)
            || CobbleUtils.breedconfig.getBlacklistForm().contains(name)
            || CobbleUtils.breedconfig.getBlacklistForm().contains(value)) continue;
          if (CobbleUtils.config.isDebug()) {
            CobbleUtils.LOGGER.info("Feature -> Name: " + name + " Value: " + value);
          }
          form.append(" ").append(name).append("=").append(value);
        }
      }
    }

    if (CobbleUtils.breedconfig.getBlacklistForm().contains(form.toString()))
      form = new StringBuilder();

    if (CobbleUtils.config.isDebug()) {
      CobbleUtils.LOGGER.info("Form: " + form);
    }

    return form.toString();
  }

  private static Pokemon applyPersistent(Pokemon pokemon, String lure_species, boolean dittos,
                                         Pokemon female) {
    Pokemon firstEvolution;

    if (lure_species == null) {
      firstEvolution = PokemonUtils.getEvolutionPokemonEgg(pokemon.getSpecies());
    } else {
      firstEvolution = PokemonProperties.Companion.parse(lure_species).create();
    }

    String form;
    if (CobbleUtils.breedconfig.isAspectEggByType()) {
      form = pokemon.getPrimaryType().getName().toLowerCase();
    } else {
      form = pokemon.showdownId();
    }

    Pokemon egg = PokemonProperties.Companion.parse("egg type_egg=" + form).create();

    // TODO: Remove the message when the pokemon is healed
    egg.setFaintedTimer(999999999);
    egg.setHealTimer(999999999);


    egg.getPersistentData().putString("species", firstEvolution.getSpecies().showdownId());
    egg.getPersistentData().putString("nature", pokemon.getNature().getName().getPath());
    egg.getPersistentData().putString("ability", pokemon.getAbility().getTemplate().getName().toLowerCase().trim());
    egg.getPersistentData().putString("form", getForm(female));
    egg.getPersistentData().putInt("level", 1);
    egg.getPersistentData().putDouble("steps", CobbleUtils.breedconfig.getSteps());
    egg.getPersistentData().putInt("cycles", pokemon.getSpecies().getEggCycles());

    egg.setScaleModifier(ScalePokemonData.getScalePokemonData(pokemon).getRandomPokemonSize().getSize());
    if (dittos) {
      egg.setNickname(Text.literal(CobbleUtils.breedconfig.getNameAbandonedEgg()));
      egg.getPersistentData().putBoolean("random", true);
    } else {
      egg.setNickname(AdventureTranslator.toNativeComponent(
        PokemonUtils.replace(
          CobbleUtils.breedconfig.getNameEgg()
            .replace("%pokemon%", firstEvolution.getSpecies().showdownId()),
          pokemon)));
    }
    return egg;
  }

  public String getInfo() {
    return String.format("§aSpecies: §f%s §aLevel: §f%d §aSteps: §f%.2f §aCycles: §f%d §aNature: §f%s §aAbility: §f%s" +
        "§aForm: §f%s §aMoves: §f%s" +
        " " +
        " §aHP: §f%d §aAttack: §f%d §aDefense: §f%d §aSpecialAttack: §f%d §aSpecialDefense: §f%d §aSpeed: §f%d",
      species, level, steps, cycles, nature, ability, form, moves, HP, Attack, Defense, SpecialAttack, SpecialDefense,
      Speed);
  }
}