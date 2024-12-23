package com.kingpixel.cobbleutils.command.base.shops;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.FilterPokemons;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.config.ShopConfig;
import com.kingpixel.cobbleutils.features.shops.models.Product;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.EconomyUtil;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import org.blanketeconomy.Blanketconfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 23/11/2024 4:39
 */
public class ShopAddFuntionality {

  // shopConfig.getShop().addProduct(product, buy, sell, mod_id, shop);

  public static void open(ServerPlayerEntity player, ShopConfig shopConfig, String modId, String shop, Product product) {
    ChestTemplate template = ChestTemplate
      .builder(6)
      .build();

    // Product
    GooeyButton productButton = GooeyButton.builder()
      .display(product.getItemStack())
      .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative("§aProduct"))
      .with(DataComponentTypes.LORE,
        new LoreComponent(AdventureTranslator.toNativeL(
          List.of("§7Current: §f" + product.getProduct() + "",
            "§7Click to change product")
        ))
      ).onClick(action -> {
        openChangeProduct(player, shopConfig, modId, shop, product);
      })
      .build();

    // Change Prices
    GooeyButton changePrices = GooeyButton.builder()
      .display(Items.EMERALD.getDefaultStack())
      .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNativeComponent("§aChange Prices"))
      .with(DataComponentTypes.LORE,
        new LoreComponent(AdventureTranslator.toNativeL(
          List.of("§7Current Buy: §f" + product.getBuy() + "",
            "§7Current Sell: §f" + product.getSell() + "",
            "§7Click to change prices")
        ))
      ).onClick(action -> {
        openChangePrices(player, shopConfig, modId, shop, product);
      })
      .build();

    //
    GooeyButton confirm = UIUtils.getConfirmButton(action -> {
      if (product != null && product.getBuy() != null && product.getSell() != null && product.getProduct() != null) {
        shopConfig.getShopConfigMenu().addProduct(product, modId, shop);
        UIManager.closeUI(player);
      } else {
        PlayerUtils.sendMessage(
          action.getPlayer(),
          "§cError: §7You need to fill in the fields: Product, Buy, and Sell.",
          CobbleUtils.language.getPrefixShop()
        );
      }
    });

    GooeyButton close = UIUtils.getCloseButton(action -> {
      UIManager.closeUI(player);
    });

    GooeyButton cancel = UIUtils.getCancelButton(action -> {
      UIManager.closeUI(player);
    });

    template.set(13, productButton);
    template.set(21, changePrices);

    template.set(48, confirm);
    template.set(49, close);
    template.set(50, cancel);

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title("Adding product to shop")
      .build();

    UIManager.openUIForcefully(player, page);
  }

  private static void openChangeProduct(ServerPlayerEntity player, ShopConfig shopConfig, String modId, String shop, Product product) {
    ChestTemplate template = ChestTemplate
      .builder(3)
      .build();

    GooeyButton pokemon = GooeyButton.builder()
      .display(PokemonItem.from(PokemonProperties.Companion.parse("pikachu")))
      .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative("§aPokemon"))
      .onClick(action -> {
        defaultChangeProduct(player, shopConfig, modId, shop, product, getPokemonButtons(player, shopConfig, modId, shop, product)
          , "Pokemons");
      })
      .build();

    GooeyButton item = GooeyButton.builder()
      .display(Items.DIAMOND.getDefaultStack())
      .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative("§aItem"))
      .onClick(action -> {
        defaultChangeProduct(player, shopConfig, modId, shop, product, getItemButtons(player, shopConfig, modId, shop, product)
          , "Items");
      })
      .build();

    GooeyButton money = GooeyButton.builder()
      .display(Items.EMERALD.getDefaultStack())
      .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative("§aMoney"))
      .onClick(action -> {
        defaultChangeProduct(player, shopConfig, modId, shop, product, getMoneyButtons(player, shopConfig, modId, shop, product)
          , "Money");
      })
      .build();


    GooeyButton moditem = GooeyButton.builder()
      .display(Items.DIAMOND.getDefaultStack())
      .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative("§aMod Item"))
      .onClick(action -> {
        defaultChangeProduct(player, shopConfig, modId, shop, product, getModItemButtons(player, shopConfig, modId,
          shop, product), "Mod Items");
      })
      .build();

    GooeyButton confirm = UIUtils.getConfirmButton(action -> {
      open(player, shopConfig, modId, shop, product);
    });


    template.set(10, pokemon);
    template.set(12, item);
    template.set(14, money);
    template.set(16, moditem);

    template.set(22, confirm);

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title("Adding product to shop")
      .build();

    UIManager.openUIForcefully(player, page);
  }

  private static List<Button> getModItemButtons(ServerPlayerEntity player, ShopConfig shopConfig, String modId, String shop, Product product) {
    List<Button> buttons = new ArrayList<>();
    ItemChance.modItems.forEach((s, itemModel) -> {
      itemModel.forEach(itemMod -> {
        GooeyButton button = GooeyButton.builder()
          .display(itemMod.getItemStack())
          .onClick(action -> {
            product.setProduct("mod:" + s + ":" + itemMod.getItemId());
            open(player, shopConfig, modId, shop, product);
          })
          .build();
        buttons.add(button);
      });
    });
    return buttons;
  }

  private static List<Button> getMoneyButtons(ServerPlayerEntity player, ShopConfig shopConfig, String modId, String shop, Product product) {
    List<Button> buttons = new ArrayList<>();
    switch (EconomyUtil.economyType) {
      case IMPACTOR -> {
        EconomyUtil.impactorService.currencies().registered().forEach(currency -> {
          GooeyButton button = GooeyButton.builder()
            .display(Items.EMERALD.getDefaultStack())
            .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative(currency.key().asString()))
            .onClick(action -> {
              product.setProduct("money:" + currency.key().asString()
                .replace("impactor:", "") + ":1");
              open(player, shopConfig, modId, shop, product);
            })
            .build();
          buttons.add(button);
        });
      }
      case BLANKECONOMY -> {
        Blanketconfig.INSTANCE.getConfig().getEconomy().forEach(economyConfig -> {
          GooeyButton button = GooeyButton.builder()
            .display(Items.EMERALD.getDefaultStack())
            .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative(economyConfig.getCurrencyType()))
            .onClick(action -> {
              product.setProduct("money:" + economyConfig.getCurrencyType() + ":1");
              open(player, shopConfig, modId, shop, product);
            })
            .build();
          buttons.add(button);
        });
      }
    }
    return buttons;
  }

  private static List<Button> getItemButtons(ServerPlayerEntity player, ShopConfig shopConfig, String modId, String shop, Product product) {
    List<Button> buttons = new ArrayList<>();
    Registries.ITEM.forEach(item -> {
      if (item == Items.AIR) return;
      GooeyButton button = GooeyButton.builder()
        .display(item.getDefaultStack())
        .onClick(action -> {
          product.setProduct("item:1:" + item.getTranslationKey()
            .replace(".", ":")
            .replace("item:", "")
            .replace("block:", ""));
          open(player, shopConfig, modId, shop, product);
        })
        .build();
      buttons.add(button);
    });
    return buttons;
  }

  private static List<Button> getPokemonButtons(ServerPlayerEntity player, ShopConfig shopConfig, String modId, String shop, Product product) {
    List<Button> buttons = new ArrayList<>();
    new FilterPokemons().getAllowedPokemons().forEach(pokemon -> {
      List<String> aspects = new ArrayList<>();
      if (pokemon.getAspects() != null) {
        aspects = pokemon.getAspects().stream().toList();
      }

      String form;
      if (!aspects.isEmpty()) {
        form = aspects.get(aspects.size() - 1);
      } else {
        form = "";
      }
      GooeyButton button = GooeyButton.builder()
        .display(PokemonItem.from(pokemon))
        .onClick(action -> {
          product.setProduct("pokemon:" + pokemon.getSpecies().showdownId() + " " + form);
          open(player, shopConfig, modId, shop, product);
        })
        .build();
      buttons.add(button);
    });
    return buttons;
  }

  private static void defaultChangeProduct(ServerPlayerEntity player, ShopConfig shopConfig, String modId,
                                           String shop, Product product, List<Button> buttons, String title) {
    ChestTemplate template = ChestTemplate
      .builder(6)
      .build();


    template.rectangle(0, 0, 5, 9, new PlaceholderButton());

    template.set(45, UIUtils.getPreviousButton(action -> {
    }));
    template.set(49, UIUtils.getCloseButton(action -> {
      openChangeProduct(player, shopConfig, modId, shop, product);
    }));
    template.set(53, UIUtils.getNextButton(action -> {
    }));

    LinkedPage.Builder linkedPage = LinkedPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative(title));

    UIManager.openUIForcefully(player, PaginationHelper.createPagesFromPlaceholders(template, buttons,
      linkedPage));
  }

  private static void openChangePrices(ServerPlayerEntity player, ShopConfig shopConfig, String modId, String shop, Product product) {
    ChestTemplate template = ChestTemplate
      .builder(3)
      .build();


    GooeyButton buy = GooeyButton.builder()
      .display(Items.EMERALD.getDefaultStack())
      .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative("§aBuy"))
      .with(DataComponentTypes.LORE,
        new LoreComponent(AdventureTranslator.toNativeL(
          List.of("§7Current: §f" + product.getBuy() + "",
            "§7Left Click to increase by 1",
            "&7Right Click to increase by 10",
            "&7Shift + Left Click to increase by 100",
            "&7Shift + Right Click to increase by 1000",
            "&7Middle Click to set to 0")
        ))
      ).onClick(action -> {
        switch (action.getClickType()) {
          case LEFT_CLICK:
            product.setBuy(product.getBuy().add(BigDecimal.ONE));
            break;
          case RIGHT_CLICK:
            product.setBuy(product.getBuy().add(BigDecimal.TEN));
            break;
          case SHIFT_LEFT_CLICK:
            product.setBuy(product.getBuy().add(BigDecimal.valueOf(100)));
            break;
          case SHIFT_RIGHT_CLICK:
            product.setBuy(product.getBuy().add(BigDecimal.valueOf(1000)));
            break;
          case MIDDLE_CLICK:
            product.setBuy(BigDecimal.ZERO);
            break;
        }
        openChangePrices(player, shopConfig, modId, shop, product);
      })
      .build();

    GooeyButton confirm = UIUtils.getConfirmButton(action -> {
      open(player, shopConfig, modId, shop, product);
    });

    GooeyButton sell = GooeyButton.builder()
      .display(Items.EMERALD.getDefaultStack())
      .with(DataComponentTypes.ITEM_NAME, AdventureTranslator.toNative("§aSell"))
      .with(DataComponentTypes.LORE, new LoreComponent(
          AdventureTranslator.toNativeL(
            List.of("§7Current: §f" + product.getSell() + "",
              "§7Left Click to increase by 1",
              "&7Right Click to increase by 10",
              "&7Shift + Left Click to increase by 100",
              "&7Shift + Right Click to increase by 1000",
              "&7Middle Click to set to 0")
          )
        )
      ).onClick(action -> {
        switch (action.getClickType()) {
          case LEFT_CLICK:
            product.setSell(product.getSell().add(BigDecimal.ONE));
            break;
          case RIGHT_CLICK:
            product.setSell(product.getSell().add(BigDecimal.TEN));
            break;
          case SHIFT_LEFT_CLICK:
            product.setSell(product.getSell().add(BigDecimal.valueOf(100)));
            break;
          case SHIFT_RIGHT_CLICK:
            product.setSell(product.getSell().add(BigDecimal.valueOf(1000)));
            break;
          case MIDDLE_CLICK:
            product.setSell(BigDecimal.ZERO);
            break;
        }
        openChangePrices(player, shopConfig, modId, shop, product);
      })
      .build();

    template.set(10, buy);
    template.set(16, sell);

    template.set(22, confirm);

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title("Adding product to shop")
      .build();

    UIManager.openUIForcefully(player, page);
  }

}
