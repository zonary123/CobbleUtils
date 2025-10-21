package com.kingpixel.cobbleutils.config;

import com.google.gson.Gson;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.ui.ConfirmMenu;
import com.kingpixel.cobbleutils.ui.PartyPcMenu;
import com.kingpixel.cobbleutils.ui.StorageMenu;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public class Lang {
  private String prefixShop;
  private String prefixStorageRewards;
  private String titlemenushiny;
  private String titlemenushinyoperation;
  private String titlemenurewards;
  private String titlepc;
  private String titleparty;
  private String titleconfirm;
  private String HA;
  private String confirm;
  private String cancel;
  private String close;
  private String next;
  private String previous;
  private String empty;
  private String yes;
  private String no;
  private String none;
  private String unknown;
  private String nocooldown;
  private String symbolshiny;
  private String pokemonnameformat;
  private String messageReload;
  private String messagerandomitem;
  private String messagearebattle;
  private String messagefossiltime;
  private String messagefossilcomplete;
  private String messageNotHaveRewards;
  private String messageHaveRewards;
  private String messageThisPokemonIsShiny;
  private String messageNoPokemon;
  private String messageReceiveReward;
  private String messageReceiveMoney;
  private String messagePokeShout;
  private String messageCooldown;
  private String messageCooldownMenu;
  private String coloritem;
  private List<String> lorepokemon;
  private List<String> lorechance;

  // AdvancedItemChance
  private String titleLoot;
  private String messagePermissionRewards;

  // Sounds
  private String soundopen;
  private String soundclose;
  private String soundConfirm;
  private String soundCancel;

  // Time
  private String days;
  private String day;
  private String hours;
  private String hour;
  private String minutes;
  private String minute;
  private String seconds;
  private String second;

  // Pokemon Colors
  private Map<String, String> gender;
  private Map<String, String> forms;
  private Map<String, String> movecolor;
  private Map<String, String> types;

  // Economy
  private String defaultSymbol;

  // Display
  private ItemModel itemMoney;
  private ItemModel itemPc;
  private ItemModel itemNoPokemon;
  private ItemModel itemPrevious;
  private ItemModel itemNext;
  private ItemModel itemClose;
  private ItemModel itemConfirm;
  private ItemModel itemCancel;
  private ItemModel itemCommand;
  private ItemModel itemAdvancedRewardsInfo;
  private Map<String, ItemModel> itemsEconomy;
  private PartyPcMenu partyPcMenu;
  private ConfirmMenu confirmMenu;
  private StorageMenu storageMenu;

  /**
   * Constructor to generate a file if one doesn't exist.
   */
  public Lang() {
    prefixShop = "&7[<gradient:#34ff00:#ade37e><bold>Shop&7] &8» &a";
    prefixStorageRewards = "&7[<gradient:#34ff00:#ade37e><bold>Storage&7] &8» &a";
    soundopen = "cobblemon:pc.on";
    soundclose = "cobblemon:pc.off";
    confirm = "&aConfirm";
    cancel = "&cCancel";
    close = "&cClose";
    next = "&eNext";
    previous = "&ePrevious";
    empty = "&cEmpty";
    titlemenushiny = "&eShiny Menu";
    titlepc = "&bPC";
    titleparty = "&bParty";
    HA = "&f(&bHA&f)";
    messageNotHaveRewards = "&cYou don't have rewards.";
    titlemenushinyoperation = "&eShiny Operation";
    titleconfirm = "&eConfirm";
    titlemenurewards = "&eRewards Menu";
    messageReload = "&aReloaded.";
    yes = "&a✔";
    no = "&c✘";
    symbolshiny = " &e⭐";
    nocooldown = "&cNo cooldown";
    unknown = "&cUnknown";
    none = "&cNone";
    coloritem = "<gradient:#cc7435:#e3ab84>";
    pokemonnameformat = "&e%pokemon%%shiny% %gender% &f(&b%form%&f) &f(&b%level%&f) %ha%";
    messageHaveRewards = "%prefix% &aYou have rewards &6%amount%&a. Use &e/storage &ato claim the rewards.";
    titleLoot = "&eLoot";
    // Messages
    messagerandomitem = "&aYou get a &e%type% &arandomitem &f%item% &6%amount%&a!";
    messagearebattle = "&aYou need to be out of battle to do this.";
    messagefossiltime = "&aYou need to wait %cooldown% &ato get a new fossil.";
    messagefossilcomplete = "&aYou have completed the fossil.";
    messageThisPokemonIsShiny = "&aThis Pokemon is shiny!";
    messageNoPokemon = "&cNo Pokemon";
    messageCooldownMenu = "&cYou have cooldown to open this menu.";
    messageReceiveReward = "&aYou receive a reward!";
    messageReceiveMoney = "&aYou receive %amount%$!";
    messagePokeShout = "§e[PokeShout] &6%player% &ashouted &e%pokemon% %gender% &f(&b%form%&f) &f(&b%level%&f) &a!";
    lorepokemon = List.of(
      "<#83dcde><lang:cobblemon.ui.lv> &f%level%",
      "<#14b2dd><lang:cobblemon.ui.info.type>: &f%types%",
      "<#ecca18>Form: &f%form%",
      "<#4969dc>Gender: %gender%",
      "<#D0A5DE><lang:cobblemon.ui.stats.friendship>: &f%friendship%",
      "<#de896f>Tradeable: &f%tradeable%",
      "<#b0eb59>Breedable: &f%breedable%",
      "<#9be8c2><lang:cobblemon.ui.info.nature>: &f%nature% &f(&a↑%up%&f/&c↓%down%&f)",
      "<#6fa7de><lang:cobblemon.ui.info.ability>: &f%ability% %ha%",
      "<#83a7de><lang:cobblemon.ui.stats.ivs>: &e%ivs%&7/&e31",
      " <#ee8339><lang:cobblemon.ui.stats.hp>: &f%ivshp% <#e84b48><lang:cobblemon.ui.stats.atk>: &f%ivsatk% <#5d79e1><lang:cobblemon.ui.stats.def>: &f%ivsdef%",
      " <#40b5cd><lang:cobblemon.ui.stats.sp_atk>: &f%ivsspa% <#f59bc2><lang:cobblemon.ui.stats.sp_def>: &f%ivsspdef% <#69cd65><lang:cobblemon.ui.stats.speed>: &f%ivsspeed%",
      "<#de8397><lang:cobblemon.ui.stats.evs>: &e%evs%&7/&e510",
      " <#ee8339><lang:cobblemon.ui.stats.hp>: &f%evshp% <#e84b48><lang:cobblemon.ui.stats.atk>: &f%evsatk% <#5d79e1><lang:cobblemon.ui.stats.def>: &f%evsdef%",
      " <#40b5cd><lang:cobblemon.ui.stats.sp_atk>: &f%evsspa% <#f59bc2><lang:cobblemon.ui.stats.sp_def>: &f%evsspdef% <#69cd65><lang:cobblemon.ui.stats.speed>: &f%evsspeed%",
      "<#e35146>Ball: &f%ball%",
      "<#ecca18>Size: &f%size%",
      "<#ecca18><lang:cobblemon.held_item>: &f%item%",
      "<#98eb59><lang:cobblemon.ui.moves>: &f%move1% &f- %move2% &f- %move3% &f- %move4%",
      //"<#b0eb59><lang:cobblemon.ui.info.original_trainer>: &f%owner%",
      "<#b0eb59>Country: &f%country%"
      //"<#b0eb59>EggGroups: &f%egggroups%",
      //"<#b0eb59>ShowdownId: &f%showdownid%"
    );
    lorechance = List.of(
      "&7Chance: &e%chance%&f%"
    );
    // Time
    messagePermissionRewards = "&cYou don't have permission to get this reward: %permission%";

    days = "&6%s &adays ";
    day = "&6%s &aday ";
    hours = "&6%s &ahours ";
    hour = "&6%s &ahour ";
    minutes = "&6%s &aminutes ";
    minute = "&6%s &aminute ";
    seconds = "&6%s &aseconds ";
    second = "&6%s &asecond";

    gender = Map.of("M", "&b♂", "F", "&d♀", "N", "&7⚲");
    forms = Map.of("hisui", "&cHisuian");
    types = new HashMap<>();
    types.put("normal", "<gradient:#939393:#C3C3C3><lang:cobblemon.type.normal>&7");
    types.put("steel", "<gradient:#706F6F:#6F6F6F><lang:cobblemon.type.steel>&7");
    types.put("poison", "<gradient:#B363CD:#D0A5DE><lang:cobblemon.type.poison>&7");
    types.put("electric", "<gradient:#E9E13B:#EAE8BA><lang:cobblemon.type.electric>&7");
    types.put("ice", "<gradient:#87CEEB:#00FFFF><lang:cobblemon.type.ice>&7");
    types.put("fighting", "<gradient:#D77361:#F1C0B7><lang:cobblemon.type.fighting>&7");
    types.put("dragon", "<gradient:#8B72CF:#B9A8E7><lang:cobblemon.type.dragon>&7");
    types.put("water", "<gradient:#5498C5:#9BC6E3><lang:cobblemon.type.water>&7");
    types.put("rock", "<gradient:#D0953C:#E5BD80><lang:cobblemon.type.rock>&7");
    types.put("ghost", "<gradient:#4B0082:#9370DB><lang:cobblemon.type.ghost>&7");
    types.put("bug", "<gradient:#A5CB60:#CBE0A5><lang:cobblemon.type.bug>&7");
    types.put("grass", "<gradient:#A5CB60:#CBE0A5><lang:cobblemon.type.grass>&7");
    types.put("flying", "<gradient:#C4E9ED:#E3F5F7><lang:cobblemon.type.flying>&7");
    types.put("dark", "<gradient:#000000:#303030><lang:cobblemon.type.dark>&7");
    types.put("fire", "<gradient:#E24D4D:#F69F9F><lang:cobblemon.type.fire>&7");
    types.put("ground", "<gradient:#B8860B:#D2B48C><lang:cobblemon.type.ground>&7");
    types.put("psychic", "<gradient:#D74DE2:#DE77E7><lang:cobblemon.type.psychic>&7");
    types.put("fairy", "<gradient:#9C38A5:#C06EC7><lang:cobblemon.type.fairy>&7");
    movecolor = new HashMap<>();
    movecolor.put("normal", "<gradient:#939393:#C3C3C3>");
    movecolor.put("steel", "<gradient:#706F6F:#6F6F6F>");
    movecolor.put("poison", "<gradient:#B363CD:#D0A5DE>");
    movecolor.put("electric", "<gradient:#E9E13B:#EAE8BA>");
    movecolor.put("ice", "<gradient:#87CEEB:#00FFFF>");
    movecolor.put("fighting", "<gradient:#D77361:#F1C0B7>");
    movecolor.put("dragon", "<gradient:#8B72CF:#B9A8E7>");
    movecolor.put("water", "<gradient:#5498C5:#9BC6E3>");
    movecolor.put("rock", "<gradient:#D0953C:#E5BD80>");
    movecolor.put("ghost", "<gradient:#4B0082:#9370DB>");
    movecolor.put("bug", "<gradient:#A5CB60:#CBE0A5>");
    movecolor.put("grass", "<gradient:#A5CB60:#CBE0A5>");
    movecolor.put("flying", "<gradient:#C4E9ED:#E3F5F7>");
    movecolor.put("dark", "<gradient:#000000:#303030>");
    movecolor.put("fire", "<gradient:#E24D4D:#F69F9F>");
    movecolor.put("ground", "<gradient:#B8860B:#D2B48C>");
    movecolor.put("psychic", "<gradient:#D74DE2:#DE77E7>");
    movecolor.put("fairy", "<gradient:#9C38A5:#C06EC7>");
    itemMoney = new ItemModel("cobblemon:relic_coin", "%amount%", List.of(), 0);
    itemPc = new ItemModel("cobblemon:pc", "&bPC", List.of(
      "&7Right click to open"
    ), 0);
    itemNoPokemon = new ItemModel("cobblemon:poke_ball", "<gradient:#E05858:#F09E9E>No Pokemon", List.of(), 0);
    itemPrevious = new ItemModel("minecraft:arrow", "<gradient:#E0A457:#E9C79B>Previous", List.of(), 0);
    itemNext = new ItemModel("minecraft:arrow", "<gradient:#E0A457:#E9C79B>Next", List.of(), 0);
    itemClose = new ItemModel("minecraft:barrier", "<gradient:#E05858:#F09E9E>Close", List.of(), 0);
    itemCommand = new ItemModel("minecraft:command_block", "<gradient:#E0A457:#E9C79B>Command", List.of(), 0);
    itemConfirm = new ItemModel("minecraft:lime_stained_glass_pane", "<gradient:#4B9F4B:#7DC97D>Confirm", List.of(),
      0);
    itemCancel = new ItemModel("minecraft:red_stained_glass_pane", "<gradient:#E05858:#F09E9E>Cancel", List.of(), 0);
    this.messageCooldown = "%prefix% <gradient:#e33636:#f08181>You need to wait %cooldown% <gradient:#e33636:#f08181>to use this command" +
      ".</gradient>";
    this.defaultSymbol = "&e$";
    this.itemsEconomy = new HashMap<>();
    itemsEconomy.put("dollars", new ItemModel("minecraft:emerald", "%amount%", List.of(
      "§aCommon")));
    itemAdvancedRewardsInfo = new ItemModel(4, "minecraft:book", "<gradient:#00ff00:#00ff00><bold>Info", List.of(
      "&7AmountRewards: &e%amount%",
      "&7Get all: %getall%"), 0);
    partyPcMenu = new PartyPcMenu();
    confirmMenu = new ConfirmMenu();
    storageMenu = new StorageMenu();
  }

  /**
   * Method to initialize the config.
   */
  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(CobbleUtils.PATH_LANG, CobbleUtils.config.getLang() + ".json",
      el -> {
        Gson gson = Utils.newGson();
        CobbleUtils.language = gson.fromJson(el, Lang.class);
        if (CobbleUtils.language.getTitlemenurewards() == null)
          CobbleUtils.language.setTitlemenurewards("&eRewards Menu");
        String data = gson.toJson(CobbleUtils.language);
        Utils.writeFileAsync(CobbleUtils.PATH_LANG, CobbleUtils.config.getLang() + ".json", data);
      });

    if (!futureRead.join()) {
      CobbleUtils.LOGGER.info("No lang.json file found for" + CobbleUtils.MOD_NAME + ". Attempting to generate one.");
      Gson gson = Utils.newGson();
      String data = gson.toJson(this);
      Utils.writeFileAsync(CobbleUtils.PATH_LANG, CobbleUtils.config.getLang() +
          ".json",
        data);
    }
  }

}
