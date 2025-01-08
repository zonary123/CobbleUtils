package com.kingpixel.cobbleutils.command.base.shops;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.shops.ShopConfigMenu;
import com.kingpixel.cobbleutils.features.shops.ShopTransactions;
import com.kingpixel.cobbleutils.util.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Carlos Varas Alonso - 28/08/2024 5:04
 */
public class ShopTransactionCommand implements Command<ServerCommandSource> {

  static GooeyPage getTransactionsPlayers(ServerPlayerEntity viewer, ShopConfigMenu shop) {
    viewer.sendMessage(AdventureTranslator.toNative("&7This command is still in development."));
    ChestTemplate template = ChestTemplate.builder(6).build();

    List<Button> buttons = generatePlayerButtons(viewer);

    template.rectangle(0, 0, 5, 9, new PlaceholderButton());
    addNavigationButtons(template);

    LinkedPage.Builder linkedPageBuilder = LinkedPage
      .builder()
      .title(AdventureTranslator.toNative("Shop Transactions Players"));

    return PaginationHelper.createPagesFromPlaceholders(template, buttons, linkedPageBuilder);
  }

  private static List<Button> generatePlayerButtons(ServerPlayerEntity viewer) {
    return ShopTransactions.transactions.entrySet().stream()
      .map(entry -> createPlayerButton(viewer, entry.getKey(), entry.getValue()))
      .collect(Collectors.toList());
  }

  private static GooeyButton createPlayerButton(ServerPlayerEntity viewer, UUID uuid,
                                                Map<String, ShopTransactions.TransactionSummary> shopActions) {
    ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(uuid);

    String title = (player != null) ? player.getName().getString() : uuid.toString();

    ItemStack itemStack = PlayerUtils.getHeadItem(player);

    List<String> lore = new ArrayList<>();
    lore.add("&7Total Transactions: &b" + shopActions.size());

    // Total Bought Amount
    BigDecimal totalBoughtAmount = shopActions.values().stream()
      .map(ShopTransactions.TransactionSummary::getTotalBoughtAmount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    lore.add("&7Total Bought Amount: &b" + totalBoughtAmount.toPlainString());

    // Total Sold Amount
    BigDecimal totalSoldAmount = shopActions.values().stream()
      .map(ShopTransactions.TransactionSummary::getTotalSoldAmount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    lore.add("&7Total Sold Amount: &b" + totalSoldAmount.toPlainString());

    // Total Money Spent
    BigDecimal totalMoneySpent = shopActions.values().stream()
      .map(ShopTransactions.TransactionSummary::getTotalSpent)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    lore.add("&7Total Money Spent: &b" + totalMoneySpent.toPlainString());

    // Total Bought Quantity
    BigDecimal totalBoughtQuantity = shopActions.values().stream()
      .map(ShopTransactions.TransactionSummary::getTotalBoughtQuantity)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    lore.add("&7Total Bought Quantity: &b" + totalBoughtQuantity.toPlainString());

    // Total Sold Quantity
    BigDecimal totalSoldQuantity = shopActions.values().stream()
      .map(ShopTransactions.TransactionSummary::getTotalSoldAmount)
      // vendida
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    lore.add("&7Total Sold Quantity: &b" + totalSoldQuantity.toPlainString());

    // Net
    BigDecimal net = shopActions.values().stream()
      .map(ShopTransactions.TransactionSummary::getNet)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    String currency = shopActions.values().stream()
      .findFirst()
      .map(ShopTransactions.TransactionSummary::getCurrency)
      .orElse("");

    String formattedNet = EconomyUtil.formatCurrency(net, currency);
    lore.add("&7Net: &b" + formattedNet);

    return GooeyButton.builder()
      .display(itemStack)
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&b" + title))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)))  // Utiliza la lista `lore`
      // completa
      .onClick(action -> UIManager.openUIForcefully(action.getPlayer(), getTransactionPlayer(viewer, uuid, CobbleUtils.shopConfig.getShopConfigMenu())))
      .build();
  }

  static GooeyPage getTransactionPlayer(ServerPlayerEntity player, UUID uuid, ShopConfigMenu shop) {
    ChestTemplate template = ChestTemplate.builder(6).build();
    List<Button> buttons = generateTransactionButtons(player, uuid, shop);

    template.rectangle(0, 0, 5, 9, new PlaceholderButton());
    addNavigationButtons(template);

    String playerName = getPlayerName(uuid);
    LinkedPage.Builder linkedPageBuilder = LinkedPage
      .builder()
      .title(AdventureTranslator.toNative("Shop Transactions for " + playerName));

    return PaginationHelper.createPagesFromPlaceholders(template, buttons, linkedPageBuilder);
  }

  private static List<Button> generateTransactionButtons(ServerPlayerEntity player, UUID uuid, ShopConfigMenu shop) {
    Map<String, ShopTransactions.TransactionSummary> transactions = ShopTransactions.transactions.getOrDefault(uuid,
      Collections.emptyMap());

    return transactions.entrySet().stream()
      .map(entry -> createTransactionButton(player, entry.getKey(), entry.getValue(), shop))
      .collect(Collectors.toList());
  }

  private static GooeyButton createTransactionButton(ServerPlayerEntity player, String product,
                                                     ShopTransactions.TransactionSummary transaction, ShopConfigMenu shop) {
    List<String> lore = List.of(
      "&7Bought: &b" + EconomyUtil.formatCurrency(transaction.getTotalBoughtPrice(), transaction.getCurrency()),
      "&7Amount: &b" + transaction.getTotalBoughtAmount(),
      "",
      "&7Sold: &b" + EconomyUtil.formatCurrency(transaction.getTotalSoldPrice(), transaction.getCurrency()),
      "&7Amount: &b" + transaction.getTotalSoldAmount(),
      "",
      "&7Net: &b" + EconomyUtil.formatCurrency(transaction.getNet(), transaction.getCurrency())
    );


    ItemStack itemStack = shop.getProductById(product).getItemStack();
    return GooeyButton.builder()
      .display(itemStack)
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(itemStack.getName().getString()))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)))
      .build();
  }

  private static void addNavigationButtons(ChestTemplate template) {
    LinkedPageButton next = UIUtils.getNextButton(action -> SoundUtil.playSound(CobbleUtils.shopLang.getSoundOpen(), action.getPlayer()));
    LinkedPageButton previous = UIUtils.getPreviousButton(action -> SoundUtil.playSound(CobbleUtils.shopLang.getSoundOpen(), action.getPlayer()));
    GooeyButton close = UIUtils.getCloseButton(action -> UIManager.closeUI(action.getPlayer()));

    template.set((6 * 9) - 1, next);
    template.set((6 * 9) - 5, close);
    template.set((6 * 9) - 9, previous);
  }

  private static String getPlayerName(UUID uuid) {
    ServerPlayerEntity player = CobbleUtils.server.getPlayerManager().getPlayer(uuid);
    return (player != null) ? player.getName().getString() : uuid.toString();
  }

  @Override
  public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return 0;
  }
}
