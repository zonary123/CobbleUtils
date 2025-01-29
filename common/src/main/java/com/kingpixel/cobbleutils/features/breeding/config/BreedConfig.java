package com.kingpixel.cobbleutils.features.breeding.config;

import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.*;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.config.Lang;
import com.kingpixel.cobbleutils.features.breeding.models.EggData;
import com.kingpixel.cobbleutils.features.breeding.models.Incense;
import com.kingpixel.cobbleutils.features.breeding.models.SelectMenu;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


/**
 * @author Carlos Varas Alonso - 29/04/2024 0:14
 */
@Getter
@ToString
@Data
public class BreedConfig {
  private boolean active;
  private String prefix;
  private boolean showIvs;
  private boolean changeuipasture;
  private boolean shifttoopen;
  private boolean obtainAspect;
  private boolean methodmasuda;
  private boolean aspectEggByType;
  private boolean ditto;
  private boolean doubleditto;
  private boolean spawnEggWorld;
  private boolean extraInfo;
  private int raritySpawnEgg;
  private boolean obtainPokeBallFromMother;
  private List<String> eggcommand;
  private List<String> abilityAcceleration;
  private List<String> permittedVehicles;
  private String titleselectplot;
  private String titleplot;
  private String titleemptyplot;
  private String titleselectpokemon;
  private String nameAbandonedEgg;
  private String nameEgg;
  private String nameRandomEgg;
  private String permissionAutoClaim;
  private int defaultNumIvsToTransfer;
  private int maxIvsRandom;
  private int numberIvsDestinyKnot;
  private SuccessItems successItems;
  private float multipliermasuda;
  private float multiplierShiny;
  private int checkEggToBreedInSeconds;
  private int tickstocheck;
  private int cooldown;
  private Map<String, Integer> cooldowns;
  private int defaultNumberPlots;
  private int maxeggperplot;
  private int rowmenuselectplot;
  private int rowmenuplot;
  private int rowmenuselectpokemon;

  private int steps;
  private int cooldowninstaBreedInSeconds;
  private int cooldowninstaHatchInSeconds;

  private Sound soundCreateEgg;
  private String createEgg;
  private String notcancreateEgg;
  private String notbreedable;
  private String notdoubleditto;
  private String notditto;
  private String notCompatible;
  private String blacklisted;

  private List<Integer> plotSlots;
  private List<String> whitelist;
  private List<String> blacklist;
  private List<String> blacklistForm;
  private List<String> blacklistLabels;
  private List<String> blacklistFeatures;

  private ItemModel closeItem;
  private ItemModel plotItem;
  private ItemModel plotThereAreEggs;
  private ItemModel maleSelectItem;
  private ItemModel femaleSelectItem;
  private ItemModel infoItem;
  private ItemModel emptySlots;
  private SelectMenu selectMenu;

  private List<Integer> maleSlots;
  private List<Integer> eggSlots;
  private List<Integer> femaleSlots;

  private List<EggData.EggForm> eggForms;
  private List<EggData.PokemonRareMecanic> pokemonRareMechanics;
  private List<Incense> incenses;
  private FilterPokemons pokemonsForDoubleDitto;


  public BreedConfig() {
    this.active = true;
    this.prefix = "&7[<#82d448>Breeding&7] &8» &a";
    this.showIvs = false;
    this.eggcommand = List.of("daycare", "pokebreed", "breed");
    this.abilityAcceleration = List.of("magmaarmor",
      "flamebody",
      "steamengine");
    this.permittedVehicles = List.of("minecraft:boat", "minecraft:minecart", "minecraft:horse");
    this.titleselectplot = "<#82d448>Select Plot";
    this.titleplot = "<#82d448>Plot";
    this.titleemptyplot = "<#82d448>Plot";
    this.titleselectpokemon = "<#82d448>Select Pokemon";
    this.extraInfo = true;
    this.obtainAspect = false;
    this.changeuipasture = true;
    this.methodmasuda = true;
    this.ditto = true;
    this.doubleditto = true;
    this.spawnEggWorld = true;
    this.shifttoopen = true;
    this.aspectEggByType = true;
    this.obtainPokeBallFromMother = true;
    this.numberIvsDestinyKnot = 5;
    this.tickstocheck = 20;
    this.multipliermasuda = 1.5f;
    this.multiplierShiny = 1.5f;
    this.permissionAutoClaim = "cobbleutils.breeding.autoclaim";
    this.cooldown = 30;
    this.cooldowns = Map.of(
      "group.vip", 15,
      "group.legendary", 10,
      "group.master", 5
    );
    this.defaultNumberPlots = 1;
    this.maxeggperplot = 3;
    this.steps = 128;
    this.checkEggToBreedInSeconds = 15;
    this.rowmenuselectplot = 3;
    this.rowmenuplot = 3;
    this.rowmenuselectpokemon = 6;
    this.raritySpawnEgg = 2048;
    this.cooldowninstaBreedInSeconds = 60;
    this.cooldowninstaHatchInSeconds = 60;
    this.defaultNumIvsToTransfer = 3;
    this.maxIvsRandom = 31;
    this.successItems = new SuccessItems();
    this.plotItem = new ItemModel(0, "minecraft:turtle_egg", "<#82d448>Plot", List.of(
      "&9male: &6%pokemon1% &f(&b%form1%&f) &f(&b%item1%&f)",
      "&dfemale: &6%pokemon2% &f(&b%form2%&f) &f(&b%item2%&f)",
      "&7Eggs: &6%eggs%",
      "&7Cooldown: &6%cooldown%"
    ), 0);
    this.infoItem = new ItemModel(4, "minecraft:book", "<#82d448>Info", List.of(
      "<#82d448>--- Info ---",
      "&7Transmit Ah: &6%ah%",
      "<#82d448>---- Items ----",
      "&7Destiny Knot: &6%destinyknot%",
      "&7Power Item: &6%poweritem%",
      "&7Ever Stone: &6%everstone%",
      "<#82d448>---- Shiny ----",
      "&7Masuda: &6%masuda% &7Multiplier: &6%multipliermasuda%",
      "&7Shiny Multiplier: &6%multipliershiny%",
      "&7ShinyRate: &6%shinyrate%",
      "<#82d448>---- Egg ----",
      "&7Egg Moves: &6%eggmoves%",
      "",
      "&7Max Ivs Random: &6%maxivs%",
      "&7Cooldown: &6%cooldown%"
    ), 0);
    this.plotSlots = List.of(
      10,
      12,
      14,
      16
    );
    this.plotThereAreEggs = new ItemModel(0, "minecraft:lime_wool", "", List.of(), 0);
    this.maleSlots = List.of();
    this.femaleSlots = List.of();
    this.eggSlots = List.of();
    this.emptySlots = new ItemModel(0, "minecraft:paper", "", List.of(""), 0);
    this.soundCreateEgg = new Sound("minecraft:entity.player.levelup");
    this.createEgg = "%prefix% <#ecca18>%pokemon1% %shiny1% &f(%form1%&f) <#64de7c>and <#ecca18>%pokemon2% %shiny2% &f(%form2%&f) <#64de7c>have created an egg <#ecca18>%egg%<#64de7c>!";
    this.notcancreateEgg = "%prefix% <#ecca18>%pokemon1% %shiny1% &f(%form1%&f) <#d65549>and <#ecca18>%pokemon2% %shiny2% &f(%form2%&f) <#d65549>can't create an egg!";
    this.notdoubleditto = "%prefix% <#d65549>you can't use two dittos!";
    this.notditto = "%prefix% <#d65549>you can't use one ditto!";
    this.blacklisted = "%prefix% <#ecca18>%pokemon% <#d65549>is blacklisted!";
    this.notbreedable = "%prefix% <#ecca18>%pokemon% <#d65549>is not breedable!";
    this.blacklist = List.of("pokestop", "egg", "manaphy");
    this.whitelist = List.of("manaphy");
    this.nameEgg = "%pokemon% Egg";
    this.nameRandomEgg = "Random Egg";
    this.nameAbandonedEgg = "Abandoned Egg";
    this.notCompatible = "%prefix% <#d65549>%pokemon1% and %pokemon2% is not compatible!";
    this.maleSelectItem = new ItemModel(10, "minecraft:light_blue_wool", "Male", List.of(""), 0);
    this.femaleSelectItem = new ItemModel(16, "minecraft:pink_wool", "Female", List.of(""), 0);
    this.incenses = Incense.defaultIncenses();
    this.blacklistForm = List.of("halloween");

    this.eggForms = List.of(
      new EggData.EggForm("galarian",
        List.of("perrserker", "sirfetchd", "mrrime", "cursola", "runerigus", "obstagoon")),
      new EggData.EggForm("paldean", List.of("clodsire")),
      new EggData.EggForm("hisuian", List.of("overqwil", "sneasler"))
    );

    this.pokemonRareMechanics = List.of(
      new EggData.PokemonRareMecanic(List.of(
        new PokemonChance("nidoranf", 50),
        new PokemonChance("nidoranm", 50)
      )),
      new EggData.PokemonRareMecanic(List.of(
        new PokemonChance("illumise", 50),
        new PokemonChance("volbeat", 50)
      ))
    );
    pokemonsForDoubleDitto = new FilterPokemons();
    pokemonsForDoubleDitto.setLegendarys(false);
    closeItem = null;
    Lang lang = CobbleUtils.language;
    if (lang != null) {
      selectMenu = new SelectMenu(
        titleselectpokemon,
        CobbleUtils.language.getItemPrevious() == null ?
          new ItemModel("minecraft:arrow", "Previous", List.of()) : CobbleUtils.language.getItemPrevious(),
        CobbleUtils.language.getItemClose() == null ?
          new ItemModel("minecraft:barrier", "Close", List.of()) : CobbleUtils.language.getItemClose(),
        CobbleUtils.language.getItemNext() == null ?
          new ItemModel("minecraft:arrow", "Next", List.of()) : CobbleUtils.language.getItemNext()

      );
    } else {
      selectMenu = new SelectMenu(
        titleselectpokemon,
        new ItemModel("minecraft:arrow", "Previous", List.of()),
        new ItemModel("minecraft:barrier", "Close", List.of()),
        new ItemModel("minecraft:arrow", "Next", List.of())
      );
    }

    blacklistLabels = List.of(
      "legendary",
      "mythical",
      "ultra_beast",
      "gmax"
    );
    blacklistFeatures = List.of(
      "netherite_coating"
    );

  }

  public void write() {
    Gson gson = Utils.newGson();
    String data = gson.toJson(this);
    CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleUtils.PATH_BREED, "config.json",
      data);
    if (!futureWrite.join()) {
      CobbleUtils.LOGGER.fatal("Could not write config.json file for " + CobbleUtils.MOD_NAME + ".");
    }
  }

  public boolean canCreateEgg(@NotNull Pokemon pokemon) {
    if (pokemon.getPersistentData().getBoolean(CobbleUtilsTags.BREEDABLE_BUILDER_TAG))
      return pokemon.getPersistentData().getBoolean(CobbleUtilsTags.BREEDABLE_TAG);

    for (String s : getWhitelist()) {
      if (pokemon.showdownId().equals(s)) return true;
    }
    if (pokemon.isLegendary() && !pokemonsForDoubleDitto.isLegendarys()) return false;
    for (String s : getBlacklist()) {
      if (pokemon.showdownId().equals(s)) return false;
    }
    for (String s : getBlacklistForm()) {
      if (pokemon.getForm().formOnlyShowdownId().equals(s)) return false;
    }
    for (String blacklistLabel : getBlacklistLabels()) {
      if (pokemon.getForm().getLabels().contains(blacklistLabel)) return false;
    }
    for (String blacklistFeature : blacklistFeatures) {
      for (SpeciesFeature feature : pokemon.getFeatures()) {
        if (feature.getName().equals(blacklistFeature)) return false;
      }
    }
    return true;
  }


  @Data
  public static class SuccessItems {
    private double percentageTransmitAH;
    private double percentageDestinyKnot;
    private double percentagePowerItem;
    private double percentageEverStone;
    private double percentageEggMoves;

    public SuccessItems() {
      this.percentageTransmitAH = 70.0;
      this.percentageDestinyKnot = 100.0;
      this.percentagePowerItem = 100.0;
      this.percentageEverStone = 100.0;
      this.percentageEggMoves = 100.0;
    }

  }

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleUtils.PATH_BREED, "config.json",
      el -> {
        Gson gson = Utils.newGson();
        CobbleUtils.breedconfig = gson.fromJson(el, BreedConfig.class);
        if (CobbleUtils.breedconfig.maleSelectItem.getSlot() == 0) CobbleUtils.breedconfig.maleSelectItem.setSlot(10);
        if (CobbleUtils.breedconfig.femaleSelectItem.getSlot() == 0)
          CobbleUtils.breedconfig.femaleSelectItem.setSlot(16);
        if (CobbleUtils.breedconfig.blacklistLabels == null) CobbleUtils.breedconfig.blacklistLabels = List.of(
          "legendary"
        );
        if (CobbleUtils.breedconfig.maxIvsRandom < 0) CobbleUtils.breedconfig.maxIvsRandom = 0;
        if (CobbleUtils.breedconfig.maxIvsRandom > 31) CobbleUtils.breedconfig.maxIvsRandom = 31;
        checker(CobbleUtils.breedconfig);
        String data = gson.toJson(CobbleUtils.breedconfig);
        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleUtils.PATH_BREED, "config.json",
          data);
        if (!futureWrite.join()) {
          CobbleUtils.LOGGER.fatal("Could not write config.json file for " + CobbleUtils.MOD_NAME + ".");
        }
      });

    if (!futureRead.join()) {
      CobbleUtils.LOGGER.info("No config.json file found for" + CobbleUtils.MOD_NAME + ". Attempting to generate one.");
      Gson gson = Utils.newGson();
      String data = gson.toJson(this);
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(CobbleUtils.PATH_BREED, "config.json",
        data);

      if (!futureWrite.join()) {
        CobbleUtils.LOGGER.fatal("Could not write config.json file for " + CobbleUtils.MOD_NAME + ".");
      }
    }

  }

  public Date getCooldown(ServerPlayerEntity player) {
    if (player == null) return new Date(System.currentTimeMillis());
    int cooldown = CobbleUtils.breedconfig.getCooldown();
    for (String permission : CobbleUtils.breedconfig.getCooldowns().keySet()) {
      if (PermissionApi.hasPermission(player, permission, 2)) {
        if (CobbleUtils.breedconfig.getCooldowns().get(permission) < cooldown) {
          cooldown = CobbleUtils.breedconfig.getCooldowns().get(permission);
        }
      }
    }
    return new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(cooldown));
  }

  private void checker(BreedConfig breedConfig) {
    if (breedConfig.getPokemonsForDoubleDitto() == null) breedConfig.setPokemonsForDoubleDitto(new FilterPokemons());
  }

  public int getNeedRows() {
    int rows = rowmenuplot;
    for (Integer plotSlot : plotSlots) {
      int currentRow = (plotSlot + 8) / 9;
      if (rows < currentRow) {
        rows = currentRow;
      }
    }
    if (rows >= 6) return 6;
    return rows;
  }
}