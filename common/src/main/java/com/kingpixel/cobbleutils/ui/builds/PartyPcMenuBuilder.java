package com.kingpixel.cobbleutils.ui.builds;

import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.template.Template;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.model.PokemonBlackList;
import com.kingpixel.cobbleutils.action.PokemonButtonAction;
import com.kingpixel.cobbleutils.ui.ConfirmMenu;
import com.kingpixel.cobbleutils.ui.PartyPcMenu;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Data
public class PartyPcMenuBuilder {
  private ServerPlayerEntity player;
  private PartyPcMenu partyPcMenu;
  private ConfirmMenu confirmMenu;
  private List<String> lorePokemon;
  private PokemonBlackList blackList;
  private Consumer<Template> templateConsumer;
  private Consumer<ButtonAction> closeAction;
  private Consumer<PokemonButtonAction> pokemonAction;
  private Function<Pokemon, ItemStack> itemStackProvider;
  private BiConsumer<Pokemon, List<String>> loreModifier;
  private Predicate<Pokemon> customFilter;

  public PartyPcMenuBuilder setCustomFilter(Predicate<Pokemon> customFilter) {
    this.customFilter = customFilter;
    return this;
  }

  public PartyPcMenuBuilder setPlayer(ServerPlayerEntity player) {
    this.player = player;
    return this;
  }

  public PartyPcMenuBuilder setItemStackProvider(Function<Pokemon, ItemStack> itemStackProvider) {
    this.itemStackProvider = itemStackProvider;
    return this;
  }

  public PartyPcMenuBuilder setPokemonAction(Consumer<PokemonButtonAction> pokemonAction) {
    this.pokemonAction = pokemonAction;
    return this;
  }

  public PartyPcMenuBuilder setCloseAction(Consumer<ButtonAction> closeAction) {
    this.closeAction = closeAction;
    return this;
  }

  public PartyPcMenuBuilder setBlackList(PokemonBlackList blackList) {
    this.blackList = blackList;
    return this;
  }

  public PartyPcMenuBuilder setLorePokemon(List<String> lorePokemon) {
    this.lorePokemon = lorePokemon;
    return this;
  }

  public PartyPcMenuBuilder setLoreModifier(BiConsumer<Pokemon, List<String>> loreModifier) {
    this.loreModifier = loreModifier;
    return this;
  }

  public PartyPcMenuBuilder setConfirmMenu(ConfirmMenu confirmMenu) {
    this.confirmMenu = confirmMenu;
    return this;
  }

  public PartyPcMenuBuilder setTemplateConsumer(Consumer<Template> templateConsumer) {
    this.templateConsumer = templateConsumer;
    return this;
  }

  public PartyPcMenuBuilder setPartyPcMenu(PartyPcMenu partyPcMenu) {
    this.partyPcMenu = partyPcMenu;
    return this;
  }

  public PartyPcMenuBuilder build() {
    if (partyPcMenu == null) partyPcMenu = new PartyPcMenu();
    if (player == null) {
      throw new IllegalArgumentException("Player cannot be null");
    }
    if (pokemonAction == null) {
      throw new IllegalArgumentException("Pokemon action cannot be null");
    }
    if (closeAction == null) {
      throw new IllegalArgumentException("Close action cannot be null");
    }
    return this;
  }
}