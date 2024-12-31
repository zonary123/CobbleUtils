package com.kingpixel.cobbleutils.action;

import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import com.cobblemon.mod.common.pokemon.Pokemon;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Carlos Varas Alonso - 29/06/2024 20:35
 */
@Getter
@Setter
public class PokemonButtonAction {
  private ButtonAction action;
  private Pokemon pokemon;

  public PokemonButtonAction(ButtonAction action, Pokemon pokemon) {
    this.action = action;
    this.pokemon = pokemon;
  }
}
