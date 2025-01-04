package com.kingpixel.cobbleutils.features.shops;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.ButtonClick;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.TemplateType;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.config.ShopConfig;
import com.kingpixel.cobbleutils.features.shops.models.Product;
import com.kingpixel.cobbleutils.features.shops.models.types.*;
import com.kingpixel.cobbleutils.util.*;
import lombok.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 02/08/2024 9:25
 */
@Getter
@Setter
@EqualsAndHashCode
@Data
@ToString
public class Shop {
  private boolean active;
  private String id;
  private String title;
  private short rows;
  private String currency;
  private TemplateType templateType;
  private ShopType shopType;
  private Rectangle rectangle;
  private ItemModel display;
  private ItemModel itemInfoShop;
  private short slotbalance;
  private int globalDiscount;
  private String soundopen;
  private String soundclose;
  private String colorItem;
  private ItemModel previous;
  private String closeCommand;
  private ItemModel close;
  private ItemModel next;
  private List<Product> products;
  private List<Integer> slotsPrevious;
  private List<Integer> slotsClose;
  private List<Integer> slotsNext;
  //private ItemModel money;
  private ItemModel fill;
  private List<FillItems> fillItems;

  public Shop() {
    this.active = true;
    this.id = "";
    this.title = "";
    this.rows = 3;
    this.currency = "dollars";
    this.templateType = TemplateType.CHEST;
    this.rectangle = new Rectangle();
    this.display = new ItemModel("cobblemon:poke_ball");
    this.itemInfoShop = display;
    this.slotbalance = 47;
    this.globalDiscount = 0;
    this.soundopen = "cobblemon:pc.on";
    this.soundclose = "cobblemon:pc.off";
    this.colorItem = "<#6bd68f>";
    this.closeCommand = "";
    this.close = CobbleUtils.language.getItemClose();
    this.next = CobbleUtils.language.getItemNext();
    this.previous = CobbleUtils.language.getItemPrevious();
    this.products = getDefaultProducts();
    this.fill = new ItemModel("");
    this.fillItems = new ArrayList<>();
    this.fillItems.add(new FillItems());
    this.shopType = new ShopTypePermanent();
  }

  public Shop(String id, String title, ShopType shopType, short rows, List<String> lore) {
    this.active = true;
    this.id = id;
    this.title = title;
    this.rows = rows;
    this.slotbalance = 47;
    this.slotsNext = new ArrayList<>();
    this.slotsPrevious = new ArrayList<>();
    this.slotsClose = new ArrayList<>();
    this.soundopen = "cobblemon:pc.on";
    this.soundclose = "cobblemon:pc.off";
    this.currency = "dollars";
    this.templateType = TemplateType.CHEST;
    this.rectangle = new Rectangle();
    this.shopType = shopType;
    this.colorItem = "<#6bd68f>";
    this.closeCommand = "";
    this.close = CobbleUtils.language.getItemClose();
    this.next = CobbleUtils.language.getItemNext();
    this.previous = CobbleUtils.language.getItemPrevious();
    this.globalDiscount = 0;
    this.display = new ItemModel("cobblemon:poke_ball");
    display.setDisplayname(title);
    display.setLore(lore);
    this.itemInfoShop = display;
    this.products = getDefaultProducts();
    this.fill = new ItemModel("");
    this.fillItems = new ArrayList<>();
    this.fillItems.add(new FillItems());
  }

  @Getter
  @Setter
  @EqualsAndHashCode
  @Data
  @ToString
  public static class Rectangle {
    private int startRow;
    private int startColumn;
    private int length;
    private int width;

    public Rectangle() {
      this.startRow = 0;
      this.startColumn = 0;
      this.length = 5;
      this.width = 9;
    }

    public Rectangle(int rows) {
      this.startRow = 0;
      this.startColumn = 0;
      this.length = rows - 1;
      this.width = 9;
    }

    public Rectangle(int startRow, int startColumn, int length, int width) {
      this.startRow = startRow;
      this.startColumn = startColumn;
      this.length = length;
      this.width = width;
    }

    public void apply(ChestTemplate template) {
      template.rectangle(startRow, startColumn, length, width, new PlaceholderButton());
    }

    public int getSlotsFree(int rows) {
      int totalSlots = rows * 9;
      int occupiedSlots = length * width;

      int startSlot = (startRow * 9) + startColumn; // Calcula el índice inicial
      int endSlot = startSlot + occupiedSlots - 1;  // Calcula el índice final ocupado

      if (endSlot >= totalSlots) {
        endSlot = totalSlots - 1;
      }

      int actualOccupiedSlots = endSlot - startSlot + 1; // Slots realmente ocupados

      return totalSlots - actualOccupiedSlots;
    }

  }


  public static List<Product> getDefaultProducts() {
    List<Product> products = new ArrayList<>();
    ItemChance.defaultItemChances().forEach(itemChance -> {
      Product product = new Product();
      product.setProduct(itemChance.getItem());
      product.setBuy(BigDecimal.valueOf(100));
      product.setSell(BigDecimal.valueOf(25));
      products.add(product);
    });
    products.add(new Product(true));
    return products;
  }


  @EqualsAndHashCode(callSuper = true)
  @Getter
  @Setter
  @Data
  @ToString
  public static class FillItems extends ItemModel {
    private List<Integer> slots;

    public FillItems() {
      super("minecraft:gray_stained_glass_pane");
      slots = new ArrayList<>();
    }
  }

  public void open(ServerPlayerEntity player, ShopConfig shopConfig, String mod_id, boolean byCommand, Shop shop) {
    if (!PermissionApi.hasPermission(player.getCommandSource(), List.of(mod_id + ".shop." + this.getId()), 4)) {
      player.sendMessage(
        AdventureTranslator.toNative(
          CobbleUtils.shopLang.getMessageNotHavePermission()
            .replace("%prefix%", CobbleUtils.shopLang.getPrefix())
        )
      );
      return;
    }

    try {
      ChestTemplate template = ChestTemplate
        .builder(rows)
        .build();

      List<Button> buttons = new ArrayList<>();

      String symbol = EconomyUtil.getSymbol(getCurrency());
      List<Product> products = shopType.getProducts(this);

      if (products == null) return;


      products.forEach(product -> {
        if (!shopConfig.getShopConfigMenu().isViewItemsWithOptionPermission()) {
          if (product.getPermission() != null) {
            if (LuckPermsUtil.checkPermission(player, product.getPermission()) && product.getNotCanBuyWithPermission() != null && product.getNotCanBuyWithPermission())
              return;
          }
        }
        BigDecimal buy = product.getBuy();
        BigDecimal sell = product.getSell();


        TypeError typeError = getTypeError(product, player);


        ItemStack itemStack;
        if (typeError == TypeError.NONE) {
          itemStack = product.getItemStack(1, false);
        } else {
          if (CobbleUtils.shopLang.isChangeItemError()) {
            itemStack = product.getItemStack(1, false);
          } else {
            itemStack = Utils.parseItemId("minecraft:barrier");
          }
        }
        int defaultamount;
        if (product.getItemchance().getType() == ItemChance.ItemChanceType.MONEY || product.getItemchance().getType() == ItemChance.ItemChanceType.COMMAND) {
          defaultamount = 1;
        } else {
          defaultamount = 0;
        }


        GooeyButton button = GooeyButton.builder()
          .display(itemStack)
          .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(colorItem == null ? getTitleItem(product) :
            colorItem + getTitleItem(product)))
          .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(getLoreProduct(
            buy, sell, product, player, symbol, typeError, BigDecimal.ONE
          ))))
          .onClick(action -> {
            if (typeError == TypeError.NONE) {
              if (shopConfig.getShopConfigMenu().isViewItemsWithOptionPermission()) {
                if (product.getPermission() != null) {
                  if (LuckPermsUtil.checkPermission(player, product.getPermission()) && product.getNotCanBuyWithPermission() != null && product.getNotCanBuyWithPermission()) {
                    PlayerUtils.sendMessage(
                      player,
                      CobbleUtils.shopLang.getMessageYouCantBuyThisProduct()
                        .replace("%product%", ItemUtils.getTranslatedName(product.getItemchance().getItemStack()))
                        .replace("%permission%", product.getPermission()),
                      CobbleUtils.shopLang.getPrefix()
                    );
                    return;
                  }
                }
              }
              switch (getShopAction(product)) {
                case BUY:
                  if (buy.compareTo(BigDecimal.ZERO) > 0) {
                    SoundUtil.playSound(getSoundopen(), player);
                    openBuySellMenu(player, shopConfig, product, TypeMenu.BUY, defaultamount, mod_id, byCommand, shop);
                  }
                  break;
                case SELL:
                  if (sell.compareTo(BigDecimal.ZERO) > 0) {
                    SoundUtil.playSound(getSoundopen(), player);
                    openBuySellMenu(player, shopConfig, product, TypeMenu.SELL, defaultamount, mod_id, byCommand, shop);
                  }
                  break;
                case BUY_SELL:
                  if (action.getClickType() == ButtonClick.LEFT_CLICK || action.getClickType() == ButtonClick.SHIFT_LEFT_CLICK) {
                    if (buy.compareTo(BigDecimal.ZERO) > 0) {
                      SoundUtil.playSound(getSoundopen(), player);
                      openBuySellMenu(player, shopConfig, product, TypeMenu.BUY, defaultamount, mod_id, byCommand, shop);
                    }
                  } else if (action.getClickType() == ButtonClick.RIGHT_CLICK || action.getClickType() == ButtonClick.SHIFT_RIGHT_CLICK) {
                    if (sell.compareTo(BigDecimal.ZERO) > 0) {
                      SoundUtil.playSound(getSoundopen(), player);
                      openBuySellMenu(player, shopConfig, product, TypeMenu.SELL, defaultamount, mod_id, byCommand, shop);
                    }
                  }
                  break;
                default:
                  sendError(player, typeError);
                  break;
              }
            } else {
              sendError(player, typeError);
            }
          })
          .build();

        buttons.add(button);
      });

      template.fill(fill.getButton(action -> {
      }));

      if (!getFillItems().isEmpty()) {
        getFillItems().forEach(fillItem -> {
          ItemStack itemStack = fillItem.getItemStack();
          fillItem.getSlots().forEach(fillItemSlot -> {
            template.set(fillItemSlot, GooeyButton.of(itemStack));
          });
        });
      }


      // Item Show Cooldown and Amount Products
      ItemModel itemInfoShop = shop.getItemInfoShop();

      if (UIUtils.isInside(itemInfoShop, rows)) {
        List<String> lore = new ArrayList<>(itemInfoShop.getLore());
        switch (shop.getShopType().getTypeShop()) {
          case DYNAMIC:
            ShopTypeDynamic shopTypeDynamic = ((ShopTypeDynamic) shop.getShopType()).updateShop(shop);
            lore.replaceAll(
              s -> s
                .replace("%cooldown%", PlayerUtils.getCooldown(shopTypeDynamic.getCooldown(shop)))
                .replace("%amountProducts%", String.valueOf(shopTypeDynamic.getAmountProducts()))
            );
            break;
          case WEEKLY:
            ShopTypeWeekly shopTypeWeekly = (ShopTypeWeekly) shop.getShopType();
            lore.replaceAll(
              s -> s
                .replace("%days%", shopTypeWeekly.getDayOfWeek().toString())
            );
            break;
          case DYNAMIC_WEEKLY:
            ShopTypeDynamicWeekly shopTypeDynamicWeekly = ((ShopTypeDynamicWeekly) shop.getShopType()).updateShop(shop);
            lore.replaceAll(
              s -> s
                .replace("%cooldown%", PlayerUtils.getCooldown(shopTypeDynamicWeekly.getCooldown(shop)))
                .replace("%amountProducts%", String.valueOf(shopTypeDynamicWeekly.getAmountProducts()))
                .replace("%days%", shopTypeDynamicWeekly.getDayOfWeek().toString())
            );
            break;
          default:
            break;
        }

        template.set(itemInfoShop.getSlot(), GooeyButton.builder()
          .display(itemInfoShop.getItemStack())
          .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(itemInfoShop.getDisplayname()))
          .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)))
          .build());
      }

      // Balance
      if (UIUtils.isInside(getSlotbalance(), rows)) {
        ItemModel balance = CobbleUtils.shopLang.getBalance();
        List<String> lorebalance = new ArrayList<>(balance.getLore());

        lorebalance.replaceAll(s -> s
          .replace("%balance%", EconomyUtil.getBalance(player, getCurrency(), EconomyUtil.getDecimals(getCurrency())))
          .replace("%currency%", getCurrency())
          .replace("%symbol%", symbol));
        template.set(getSlotbalance(), GooeyButton.builder()
          .display(balance.getItemStack())
          .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(balance.getDisplayname()))
          .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lorebalance)))
          .build());
      }

      // Display
      int slotfree = rectangle.getSlotsFree(rows);

      if (!byCommand) {
        if (UIUtils.isInside(getClose(), rows)) {
          GooeyButton close = getClose().getButton(action -> {
            if (closeCommand == null || closeCommand.isEmpty()) {
              ShopConfigMenu.open(player, shopConfig, mod_id, byCommand);
            } else {
              UIManager.closeUI(player);
              PlayerUtils.executeCommand(closeCommand, player);
            }
          });
          template.set(getClose().getSlot(), close);
          if (slotsClose != null && !slotsClose.isEmpty()) {
            for (Integer i : slotsClose) {
              if (UIUtils.isInside(i, rows)) {
                template.set(i, close);
              }
            }
          }
        }
      }

      if (slotfree - products.size() < 0) {
        if (UIUtils.isInside(getNext(), rows)) {
          LinkedPageButton next = LinkedPageButton.builder()
            .display(getNext().getItemStack())
            .onClick(action -> SoundUtil.playSound(getSoundopen(), action.getPlayer()))
            .linkType(LinkType.Next)
            .build();
          template.set(getNext().getSlot(), next);
          if (slotsNext != null && !slotsNext.isEmpty()) {
            for (Integer i : slotsNext) {
              if (UIUtils.isInside(i, rows)) {
                template.set(i, next);
              }
            }
            slotsNext.forEach(slot -> template.set(slot, next));
          }
        }


        if (UIUtils.isInside(getPrevious(), rows)) {
          LinkedPageButton previous = LinkedPageButton.builder()
            .display(getPrevious().getItemStack())
            .onClick(action -> {
              SoundUtil.playSound(getSoundopen(), action.getPlayer());
            })
            .linkType(LinkType.Previous)
            .build();
          template.set(getPrevious().getSlot(), previous);
          if (slotsPrevious != null && !slotsPrevious.isEmpty()) {
            for (Integer i : slotsPrevious) {
              if (UIUtils.isInside(i, rows)) {
                template.set(i, previous);
              }
            }
          }
        }
      }
      LinkedPage.Builder linkedPageBuilder = LinkedPage.builder();

      rectangle.apply(template);


      linkedPageBuilder
        .template(template)
        .title(AdventureTranslator.toNative(title))
        .build();


      LinkedPage page = PaginationHelper.createPagesFromPlaceholders(template, buttons, linkedPageBuilder);

      UIManager.openUIForcefully(player, page);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public enum ShopAction {
    BUY, SELL, BUY_SELL
  }

  public ShopAction getShopAction(Product product) {
    if (product.getBuy().compareTo(BigDecimal.ZERO) > 0 && product.getSell().compareTo(BigDecimal.ZERO) > 0) {
      return ShopAction.BUY_SELL;
    } else if (product.getBuy().compareTo(BigDecimal.ZERO) > 0) {
      return ShopAction.BUY;
    } else if (product.getSell().compareTo(BigDecimal.ZERO) > 0) {
      return ShopAction.SELL;
    }
    return null;
  }

  private List<String> getLoreProduct(BigDecimal buy, BigDecimal sell, Product product, ServerPlayerEntity player,
                                      String symbol, TypeError typeError, BigDecimal amount) {
    String symbolValue = (symbol != null) ? symbol : "";
    String currencyValue = (currency != null) ? currency : "";
    BigDecimal amountValue = (amount != null) ? amount : BigDecimal.ZERO;

    List<String> lore = new ArrayList<>(CobbleUtils.shopLang.getLoreProduct());

    if (product.getLore() != null && !product.getLore().isEmpty()) {
      lore.addAll(product.getLore());
    }

    if (buy.compareTo(BigDecimal.ZERO) <= 0) {
      lore.removeIf(s -> s.contains("%buy%"));
      lore.removeIf(s -> s.contains("%removebuy%"));
    }

    if (sell.compareTo(BigDecimal.ZERO) <= 0) {
      lore.removeIf(s -> s.contains("%sell%"));
      lore.removeIf(s -> s.contains("%removesell%"));
    }

    int discount = getDiscount(product);

    String priceWithoutDiscount = EconomyUtil.formatCurrency(calculatePrice(product, TypeMenu.BUY, amount, false),
      currency,
      player.getUuid());

    String priceDiscount = EconomyUtil.formatCurrency(calculatePrice(product, TypeMenu.BUY, amount, true), currency, player.getUuid());

    haveDiscount(product);

    lore.replaceAll(s -> s
      .replace("%buy%", haveDiscount(product) ? "&m" + (priceWithoutDiscount != null ? priceWithoutDiscount : "") + "&r &e" + (priceDiscount != null ? priceDiscount : "") :
        (priceWithoutDiscount != null ? priceWithoutDiscount : ""))
      .replace("%sell%", EconomyUtil.formatCurrency(calculatePrice(product, TypeMenu.SELL, amountValue, false), currencyValue,
        player.getUuid()))
      .replace("%currency%", getCurrency() != null ? getCurrency() : "")
      .replace("%symbol%", symbolValue)
      .replace("%amount%", amountValue.toString())
      .replace("%amountproduct%", String.valueOf(product.getItemchance().getItemStack().getCount()))
      .replace("%total%", String.valueOf(amountValue.compareTo(BigDecimal.ZERO) == 0 ? 1 : amountValue.multiply(BigDecimal.valueOf(product.getItemchance().getItemStack().getCount()))))
      .replace("%balance%", EconomyUtil.getBalance(player, getCurrency(), EconomyUtil.getDecimals(getCurrency())))
      .replace("%removebuy%", "")
      .replace("%removesell%", "")
      .replace("%discount%", (discount > 0) ? discount + "%" : "")
    );


    if (typeError == TypeError.PERMISSION) {
      lore = new ArrayList<>(lore);
      lore.add(CobbleUtils.shopLang.getNotPermission()
        .replace("%product%", ItemUtils.getTranslatedName(product.getItemchance().getItemStack())));
      lore.replaceAll(s ->
        s.replace("%description%", product.getLore() != null ? String.valueOf(product.getLore()) : "")
      );
    } else if (typeError != TypeError.NONE) {
      lore = new ArrayList<>();
      lore.add("&cError in this product");
      lore.add("&cContact the server administrator");
      lore.add(product.getItemchance().toString());
      lore.add("&cError: " + typeError.name());
    }

    return lore;
  }

  private boolean haveDiscount(Product product) {
    return ((product.getDiscount() != null && product.getDiscount() > 0) || globalDiscount > 0);
  }

  private int getDiscount(Product product) {
    int discount = product.getDiscount() != null && product.getDiscount() > 0 ? product.getDiscount() : this.globalDiscount;
    return discount;
  }

  private enum TypeMenu {
    BUY, SELL, STACK
  }

  private enum TypeError {
    INVALID_PRICE, ZERO, PERMISSION, BAD_DISCOUNT, INVALID_PRICE_WITH_DISCOUNT, NONE
  }

  private TypeError getTypeError(Product product, ServerPlayerEntity player) {
    BigDecimal globalDiscount = BigDecimal.valueOf(this.globalDiscount);
    if (product.getBuy().compareTo(BigDecimal.ZERO) <= 0 && product.getSell().compareTo(BigDecimal.ZERO) <= 0) {
      return TypeError.ZERO;
    } else if (!PermissionApi.hasPermission(player.getCommandSource(), product.getPermission(), 4)) {
      return TypeError.PERMISSION;
    } else if (product.getBuy().compareTo(BigDecimal.ZERO) > 0 && product.getSell().compareTo(product.getBuy()) > 0) {
      return TypeError.INVALID_PRICE;
    } else if (product.getDiscount() != null && (product.getDiscount() > 100) || globalDiscount.compareTo(BigDecimal.valueOf(100)) > 0) {
      return TypeError.BAD_DISCOUNT;
    } else {
      if (product.getBuy().compareTo(BigDecimal.ZERO) == 0) return TypeError.NONE;


      BigDecimal applicableDiscount = product.getDiscount() != null && product.getDiscount() > 0 ?
        BigDecimal.valueOf(product.getDiscount()) :
        globalDiscount;


      BigDecimal discountedBuyPrice = product.getBuy().multiply(BigDecimal.ONE.subtract(applicableDiscount.divide(BigDecimal.valueOf(100))));

      if (product.getSell().compareTo(discountedBuyPrice) > 0) {
        return TypeError.INVALID_PRICE_WITH_DISCOUNT;
      }
    }

    return TypeError.NONE;
  }


  private void sendError(ServerPlayerEntity player, TypeError typeError) {
    switch (typeError) {
      case INVALID_PRICE:
        player.sendMessage(Text.literal("Buy price is higher than sell price contact the server administrator"));
        break;
      case ZERO:
        player.sendMessage(Text.literal("Buy and sell price is zero contact the server administrator"));
        break;
      case PERMISSION:
        player.sendMessage(Text.literal("You don't have permission to buy or sell this product"));
        break;
      case BAD_DISCOUNT:
        player.sendMessage(Text.literal("Discount is not valid contact the server administrator"));
        break;
      case INVALID_PRICE_WITH_DISCOUNT:
        player.sendMessage(Text.literal("Sell price is higher than buy price with discount contact the server administrator"));
        break;
      default:
        player.sendMessage(Text.literal("Error in this product contact the server administrator"));
        break;
    }
  }

  private void openBuySellMenu(ServerPlayerEntity player, ShopConfig shopConfig,
                               Product product, TypeMenu typeMenu,
                               int amount, String mod_id, boolean byCommand, Shop shop) {
    ChestTemplate template = ChestTemplate
      .builder(shopConfig.getShopConfigMenu().getRowsBuySellMenu())
      .build();

    int maxStack = product.getItemchance().getItemStack().getMaxCount();
    BigDecimal price = calculatePrice(product, typeMenu, BigDecimal.valueOf(amount), true);
    String title = generateTitle(product, typeMenu);
    String symbol = EconomyUtil.getSymbol(getCurrency());

    // Botones de cantidad
    if (maxStack != 1) {
      createAmountButton(shopConfig, template, player, product, typeMenu, CobbleUtils.shopLang.getAdd1(),
        CobbleUtils.shopLang.getRemove1(), 1, amount, mod_id, byCommand, shop);

      if (maxStack == 16) {
        createAmountButton(shopConfig, template, player, product, typeMenu, CobbleUtils.shopLang.getAdd8(),
          CobbleUtils.shopLang.getRemove8(), 8, amount, mod_id, byCommand, shop);
        createAmountButton(shopConfig, template, player, product, typeMenu, CobbleUtils.shopLang.getAdd16(),
          CobbleUtils.shopLang.getRemove16(), 16, amount, mod_id, byCommand, shop);
      }
      if (maxStack == 64) {
        createAmountButton(shopConfig, template, player, product, typeMenu, CobbleUtils.shopLang.getAdd10(),
          CobbleUtils.shopLang.getRemove10(), 10, amount, mod_id, byCommand, shop);
        createAmountButton(shopConfig, template, player, product, typeMenu, CobbleUtils.shopLang.getAdd64(),
          CobbleUtils.shopLang.getRemove64(), 64, amount, mod_id, byCommand, shop);
      }
    }

    // Botón de ver producto
    createViewProductButton(shopConfig, mod_id, player, template, product, typeMenu, amount, price, symbol, byCommand
      , shop);

    // Botón de confirmar
    ItemModel confirm = CobbleUtils.shopLang.getConfirm();
    template.set(confirm.getSlot(), confirm.getButton(action -> {

      int finalamount = (amount == 0) ? 1 : amount;
      if (typeMenu == TypeMenu.BUY) {
        if (buyProduct(player, product, finalamount, price)) {
          //ShopTransactions.addTransaction(player.getUuid(), this, ShopTransactions.ShopAction.BUY, product,
          //  BigDecimal.valueOf(finalamount), price);
          open(player, shopConfig, mod_id, false, shop);
          //ShopTransactions.updateTransaction(player.getUuid(), shopConfig.getShopConfigMenu());
        }
      }
      if (typeMenu == TypeMenu.SELL) {
        if (sellProduct(player, product, finalamount)) {
          //ShopTransactions.addTransaction(player.getUuid(), this, ShopTransactions.ShopAction.SELL, product,
          // BigDecimal.valueOf(finalamount), price);
          open(player, shopConfig, mod_id, false, shop);
          //ShopTransactions.updateTransaction(player.getUuid(), shopConfig.getShopConfigMenu());
        }
      }

    }));

    // Botón de comprar pilas completas
    //createMaxStackButton(template, player, product, typeMenu, maxStack);

    // Botón de cancelar
    createCancelButton(template, shopConfig, mod_id, player, byCommand, shop);

    // Relleno y botón de cerrar
    template.fill(GooeyButton.of(Utils.parseItemId(CobbleUtils.config.getFill())));

    createCloseButton(template, shopConfig, mod_id, player, byCommand, shop);

    GooeyPage page = GooeyPage.builder()
      .title(AdventureTranslator.toNative(title))
      .template(template)
      .build();

    /*page.subscribe(amount, () -> {
      createViewProductButton(shopConfig, mod_id, player, template, product, typeMenu, amount, price, symbol, byCommand, shop);
    });*/

    UIManager.openUIForcefully(player, page);
  }

  private boolean buyProduct(ServerPlayerEntity player, Product product, int amount, BigDecimal price) {
    if (price.compareTo(BigDecimal.ZERO) <= 0) return false;
    if (EconomyUtil.hasEnough(player, currency, price)) {
      SoundUtil.playSound(CobbleUtils.shopLang.getSoundBuy(), player);

      ItemChance itemChance = product.getItemchance();
      ItemStack productStack = itemChance.getItemStack();


      int itemsPerPackage = productStack.getCount(); // Número de ítems en cada paquete
      int maxStackSize = productStack.getMaxCount(); // Capacidad máxima de un ItemStack

      if (itemChance.getType() == ItemChance.ItemChanceType.ITEM) {
        if (itemChance.getItem().startsWith("item:")) {
          itemsPerPackage = Integer.parseInt(itemChance.getItem().split(":")[1]);
        }
      }

      // Total de ítems a entregar
      int totalItems = amount * itemsPerPackage;

      // Calcula cuántos paquetes completos se necesitan
      int fullStacks = totalItems / maxStackSize;
      int remainder = totalItems % maxStackSize;

      // Entregar los paquetes completos
      for (int i = 0; i < fullStacks; i++) {
        ItemChance.giveReward(player, itemChance, maxStackSize / itemsPerPackage);
      }

      // Entregar los paquetes parciales si es necesario
      if (remainder > 0) {
        int remainderPackages = (int) Math.ceil((double) remainder / itemsPerPackage);
        ItemChance.giveReward(player, itemChance, remainderPackages);
      }

      // Si llegamos aquí, la compra se realizó con éxito
      return true;
    }
    return false;
  }


  private boolean sellProduct(ServerPlayerEntity player, Product product, int amount) {
    int packageSize = product.getItemchance().getItemStack().getCount();
    int digits = EconomyUtil.getDecimals(getCurrency());

    BigDecimal unitPrice =
      product.getSell().divide(BigDecimal.valueOf(packageSize), digits, RoundingMode.HALF_EVEN);

    BigDecimal totalPrice =
      unitPrice.multiply(BigDecimal.valueOf(amount));

    // Verifica si el jugador tiene la cantidad requerida del producto en su inventario
    int amountItemInv = player.getInventory().main.stream()
      .filter(itemInv -> !itemInv.isEmpty() && ItemStack.areItemsEqual(itemInv, product.getItemchance().getItemStack(amount)))
      .mapToInt(ItemStack::getCount)
      .sum();

    if (amountItemInv >= amount) {
      int remaining = amount;

      // Remueve los ítems del inventario del jugador
      for (ItemStack itemStack : player.getInventory().main) {
        if (!itemStack.isEmpty() && ItemStack.areItemsEqual(itemStack, product.getItemchance().getItemStack(amount))) {
          int count = itemStack.getCount();

          if (count >= remaining) {
            itemStack.decrement(remaining);
            break;
          } else {
            remaining -= count;
            itemStack.setCount(0);
          }
        }
      }

      // Añade el precio calculado a la cuenta del jugador
      EconomyUtil.addMoney(player, getCurrency(), totalPrice);
      SoundUtil.playSound(CobbleUtils.shopLang.getSoundSell(), player);
      // Envía un mensaje de éxito al jugador
      player.sendMessage(
        AdventureTranslator.toNative(
          CobbleUtils.shopLang.getMessageSellSuccess()
            .replace("%prefix%", CobbleUtils.shopLang.getPrefix())
            .replace("%amount%", String.valueOf(amount))
            .replace("%amountproduct%", String.valueOf(product.getItemchance().getItemStack().getCount()))
            .replace("%total%", String.valueOf(amount * product.getItemchance().getItemStack().getCount()))
            .replace("%product%", ItemUtils.getTranslatedName(product.getItemchance().getItemStack()))
            .replace("%price%", EconomyUtil.formatCurrency(totalPrice, currency, player.getUuid()))
            .replace("%unitprice%", EconomyUtil.formatCurrency(unitPrice, currency, player.getUuid()))
            .replace("%sell%", EconomyUtil.formatCurrency(totalPrice, currency, player.getUuid()))
            .replace("%currency%", getCurrency())
            .replace("%symbol%", EconomyUtil.getSymbol(getCurrency()))
            .replace("%balance%", EconomyUtil.getBalance(player, getCurrency(), EconomyUtil.getDecimals(getCurrency())))
        )
      );
      return true;
    } else {
      // Envía un mensaje de error si el jugador no tiene la cantidad suficiente
      SoundUtil.playSound(CobbleUtils.shopLang.getSoundError(), player);
      player.sendMessage(
        AdventureTranslator.toNative(
          CobbleUtils.shopLang.getMessageSellError()
            .replace("%prefix%", CobbleUtils.shopLang.getPrefix())
            .replace("%amount%", String.valueOf(amount))
            .replace("%amountproduct%", String.valueOf(product.getItemchance().getItemStack().getCount()))
            .replace("%total%", String.valueOf(amount * product.getItemchance().getItemStack().getCount()))
            .replace("%product%", ItemUtils.getTranslatedName(product.getItemchance().getItemStack()))
            .replace("%price%", EconomyUtil.formatCurrency(totalPrice, currency, player.getUuid()))
            .replace("%unitprice%", EconomyUtil.formatCurrency(unitPrice, currency, player.getUuid()))
            .replace("%sell%", EconomyUtil.formatCurrency(totalPrice, currency, player.getUuid()))
            .replace("%currency%", getCurrency())
            .replace("%symbol%", EconomyUtil.getSymbol(getCurrency()))
            .replace("%balance%", EconomyUtil.getBalance(player, getCurrency(), EconomyUtil.getDecimals(getCurrency())))
        )
      );
    }
    return false;
  }


  private BigDecimal calculatePrice(Product product, TypeMenu typeMenu, BigDecimal amount, boolean showDiscount) {

    // Verificamos si hay un descuento y si es un tipo de menú de compra (BUY)
    BigDecimal finalAmount = (amount.compareTo(BigDecimal.ZERO) <= 0) ? BigDecimal.ONE : amount;
    if (showDiscount) {
      int discount = getDiscount(product);
      if (discount > 0 && discount <= 100 && typeMenu == TypeMenu.BUY) {
        // Calcular el precio por artículo (multiplicando por la cantidad)
        BigDecimal pricePerItem = product.getBuy();
        // Calcular el monto del descuento
        BigDecimal discountAmount = pricePerItem.multiply(BigDecimal.valueOf(discount)).divide(BigDecimal.valueOf(100));
        // Calcular el precio final con el descuento aplicado y la cantidad
        return pricePerItem.subtract(discountAmount).multiply(finalAmount);
      }
    }

    // Si no hay descuento o no es una compra, usamos el precio según el tipo de menú
    BigDecimal pricePerItem = (typeMenu == TypeMenu.BUY) ? product.getBuy() : product.getSell();
    return pricePerItem.multiply(finalAmount);
  }


  private String generateTitle(Product product, TypeMenu typeMenu) {
    return (typeMenu == TypeMenu.BUY) ?
      CobbleUtils.shopLang.getTitleBuy().replace("%product%", getTitleItem(product)) :
      CobbleUtils.shopLang.getTitleSell().replace("%product%", getTitleItem(product));
  }

  private void createAmountButton(ShopConfig shopConfig, ChestTemplate template, ServerPlayerEntity player,
                                  Product product, TypeMenu typeMenu,
                                  ItemModel addModel, ItemModel removeModel,
                                  int increment, int amount,
                                  String mod_id, boolean byCommand, Shop shop) {
    if (product.getItemchance().getType() == ItemChance.ItemChanceType.MONEY || product.getItemchance().getType() == ItemChance.ItemChanceType.COMMAND)
      return;
    template.set(addModel.getSlot(), GooeyButton.builder()
      .display(addModel.getItemStack(increment))
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(addModel.getDisplayname(), null, player))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(addModel.getLore(), player)))
      .onClick(action -> {
        SoundUtil.playSound(CobbleUtils.shopLang.getSoundAdd(), player);
        openBuySellMenu(player, shopConfig, product, typeMenu, amount + increment, mod_id, byCommand, shop);
      })
      .build());


    template.set(removeModel.getSlot(), GooeyButton.builder()
      .display(removeModel.getItemStack(increment))
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(removeModel.getDisplayname(), null, player))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(removeModel.getLore(), player)))
      .onClick(action -> {
        SoundUtil.playSound(CobbleUtils.shopLang.getSoundRemove(), player);
        openBuySellMenu(player, shopConfig, product, typeMenu, Math.max(amount - increment, 1), mod_id, byCommand, shop);
      })
      .build());


  }

  private void createViewProductButton(ShopConfig shopConfig, String mod_id,
                                       ServerPlayerEntity player, ChestTemplate template,
                                       Product product, TypeMenu typeMenu,
                                       int amount, BigDecimal price,
                                       String symbol, boolean byCommand, Shop shop) {


    ItemStack viewProduct = getViewItemStack(product, (amount == 0 ? 1 : amount));

    BigDecimal buy;
    BigDecimal sell;

    if (typeMenu == TypeMenu.BUY) {
      buy = price;
      sell = BigDecimal.ZERO;
    } else if (typeMenu == TypeMenu.SELL) {
      buy = BigDecimal.ZERO;
      sell = price;
    } else {
      buy = product.getBuy();
      sell = product.getSell();
    }

    viewProduct.setCount((amount == 0 ? 1 : amount));
    template.set(shopConfig.getShopConfigMenu().getSlotViewProduct(), GooeyButton.builder()
      .display(viewProduct)
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(getTitleItem(product)))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(getLoreProduct(
        buy, sell,
        product, player,
        symbol, TypeError.NONE,
        BigDecimal.valueOf(amount)
      ))))
      .onClick(action -> {
        TypeError typeError = getTypeError(product, player);
        if (typeError == TypeError.NONE) {
          SoundUtil.playSound(CobbleUtils.shopLang.getSoundOpen(), player);
          if (action.getClickType() == ButtonClick.LEFT_CLICK || action.getClickType() == ButtonClick.SHIFT_LEFT_CLICK) {
            if (product.getBuy().compareTo(BigDecimal.ZERO) > 0) {
              SoundUtil.playSound(getSoundopen(), player);
              openBuySellMenu(player, shopConfig, product, TypeMenu.BUY, 0, mod_id, byCommand, shop);
            }
          } else if (action.getClickType() == ButtonClick.RIGHT_CLICK || action.getClickType() == ButtonClick.SHIFT_RIGHT_CLICK) {
            if (product.getSell().compareTo(BigDecimal.ZERO) > 0) {
              SoundUtil.playSound(getSoundopen(), player);
              openBuySellMenu(player, shopConfig, product, TypeMenu.SELL, 0, mod_id, byCommand, shop);
            }
          }
        } else {
          sendError(player, typeError);
        }
      })
      .build());
  }

  private String getTitleItem(Product product) {
    String titleItem;

    if (product.getProduct().contains("pokemon:")) {
      return PokemonUtils.replace(product.getColor() == null ? CobbleUtils.language.getPokemonnameformat() :
          product.getColor() + CobbleUtils.language.getPokemonnameformat(),
        PokemonProperties.Companion.parse(product.getProduct().replace(
          "pokemon:",
          "")).create());
    }

    if (product.getDisplayname() != null && !product.getDisplayname().isEmpty()) {
      titleItem = product.getDisplayname();
    } else {
      if (product.getColor() == null || product.getColor().isEmpty()) {
        titleItem =
          (this.colorItem == null ? "" : this.colorItem) + ItemUtils.getTranslatedName(product.getItemchance().getItemStack());
      } else {
        titleItem = product.getColor() + ItemUtils.getTranslatedName(product.getItemchance().getItemStack());
      }
    }
    return titleItem;
  }

  private void createCancelButton(ChestTemplate template, ShopConfig shopConfig,
                                  String mod_id, ServerPlayerEntity player, boolean byCommand, Shop shop) {
    ItemModel cancel = CobbleUtils.shopLang.getCancel();
    template.set(cancel.getSlot(), cancel.getButton(action -> open(player, shopConfig, mod_id, byCommand, shop)));
  }

  private ItemStack getViewItemStack(Product product, int amount) {
    return product.getItemStack(amount);
  }

  private void createCloseButton(ChestTemplate template, ShopConfig shopConfig,
                                 String mod_id, ServerPlayerEntity player, boolean byCommand, Shop shop) {
    template.set((shopConfig.getShopConfigMenu().getRowsBuySellMenu() * 9) - 5, UIUtils.getCloseButton(action -> open(player,
      shopConfig, mod_id, byCommand, shop)));
  }

}
