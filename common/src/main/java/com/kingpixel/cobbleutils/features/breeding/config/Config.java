package com.kingpixel.cobbleutils.features.breeding.config;

import com.google.gson.Gson;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * @author Carlos Varas Alonso - 29/04/2024 0:14
 */
@Getter
@ToString
@Data
public class Config {
  private String prefix;
  private List<String> eggcommand;
  private boolean active;
  private boolean autoclaim;
  private boolean changeuipasture;
  private boolean obtainAspect;
  private float multiplierShiny;
  private int cooldown;
  private int maxeggperplot;
  private int maxplots;
  private int rowmenuselectplot;
  private int rowmenuplot;
  private int rowmenuselectpokemon;
  private String createEgg;
  private String notcancreateEgg;
  private ItemModel plotItem;
  private List<Integer> plotSlots;


  public Config() {
    this.prefix = "&7[<##82d448>Breeding&7] &8»";
    this.eggcommand = List.of("breed", "egg");
    this.active = true;
    this.autoclaim = false;
    this.obtainAspect = false;
    this.changeuipasture = false;
    this.multiplierShiny = 1.5f;
    this.cooldown = 30;
    this.maxeggperplot = 3;
    this.maxplots = 3;
    this.rowmenuselectplot = 6;
    this.rowmenuplot = 6;
    this.rowmenuselectpokemon = 6;
    this.plotItem = new ItemModel(0, "minecraft:turtle_egg", "Plot", List.of(
      "pokemon1: %pokemon1%",
      "pokemon2: %pokemon2%"
    ), 0);
    this.plotSlots = List.of(10,
      12,
      14,
      16,
      18,
      20,
      22,
      24,
      26);
    this.createEgg = "%prefix% <#ecca18>%pokemon1% %shiny% &f(%form%&f) <#64de7c>and <#ecca18>%pokemon2% %shiny% &f(%form%&f) " +
      "<#64de7c>have created an egg <#ecca18>%egg%<#64de7c>!";
    this.notcancreateEgg = "%prefix% <#ecca18>%pokemon1% %shiny% <#d65549>and <#ecca18>%pokemon2% %shiny% &f(%form%&f) <#d65549>can't " +
      "create an egg!";
  }

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleUtils.PATH_BREED, "config.json",
      el -> {
        Gson gson = Utils.newGson();
        Config config = gson.fromJson(el, Config.class);
        prefix = config.getPrefix();
        active = config.isActive();
        changeuipasture = config.isChangeuipasture();
        createEgg = config.getCreateEgg();
        cooldown = config.getCooldown();
        maxeggperplot = config.getMaxeggperplot();
        maxplots = config.getMaxplots();
        notcancreateEgg = config.getNotcancreateEgg();
        autoclaim = config.isAutoclaim();
        plotItem = config.getPlotItem();
        multiplierShiny = config.getMultiplierShiny();
        eggcommand = config.getEggcommand();
        obtainAspect = config.isObtainAspect();
        plotSlots = config.getPlotSlots();
        rowmenuplot = config.getRowmenuplot();
        rowmenuselectplot = config.getRowmenuselectplot();
        rowmenuselectpokemon = config.getRowmenuselectpokemon();

        String data = gson.toJson(this);
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
}