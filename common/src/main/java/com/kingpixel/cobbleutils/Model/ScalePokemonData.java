package com.kingpixel.cobbleutils.Model;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Clase que representa los datos de escala de un Pokémon.
 * Proporciona métodos para obtener tamaños basados en configuraciones y probabilidades.
 * <p>
 * Autor: Carlos Varas Alonso - 15/07/2024
 */
@Getter
@ToString
@EqualsAndHashCode
public class ScalePokemonData {
  private String pokemon;
  private String form;
  private List<SizeChanceWithoutItem> sizes;

  /**
   * Constructor que inicializa con Pokémon y su forma.
   * Transforma las probabilidades de tamaño a partir de la configuración.
   */
  public ScalePokemonData(String pokemon, String form) {
    this.pokemon = pokemon;
    this.form = form;
    this.sizes = SizeChanceWithoutItem.transform(CobbleUtils.config.getPokemonsizes());
  }

  /**
   * Constructor que inicializa con Pokémon, su forma y tamaños específicos.
   */
  public ScalePokemonData(String pokemon, String form, List<SizeChanceWithoutItem> sizes) {
    this.pokemon = pokemon;
    this.form = form;
    this.sizes = sizes;
  }

  public static SizeChanceWithoutItem getScalePokemonData(Pokemon pokemon, Float value) {
    return getScalePokemonData(pokemon)
      .getSizes()
      .stream()
      .filter(sizeChanceWithoutItem -> sizeChanceWithoutItem.getSize() >= value)
      .findFirst()
      .orElse(new SizeChanceWithoutItem());
  }


  public String info() {
    return "Pokemon: " + pokemon + " Form: " + form;
  }

  /**
   * Compare two ScalePokemonData objects.
   * <p>
   * Compare the pokemon and form of the two objects.
   * <p>
   *
   * @param scalePokemonData1 ScalePokemonData
   * @param scalePokemonData2 ScalePokemonData
   *
   * @return boolean if the two objects are equals
   */
  public static boolean equals(ScalePokemonData scalePokemonData1, ScalePokemonData scalePokemonData2) {
    return scalePokemonData1.getPokemon().equalsIgnoreCase(scalePokemonData2.getPokemon()) &&
      (scalePokemonData1.getForm().equalsIgnoreCase(scalePokemonData2.getForm()) ||
        scalePokemonData1.getForm().equals("*") ||
        scalePokemonData2.getForm().equals("*"));
  }


  /**
   * Obtiene un tamaño aleatorio basado en las probabilidades.
   */
  public static SizeChanceWithoutItem getRandomSize(List<SizeChanceWithoutItem> SizeChance) {
    int totalWeight = SizeChance.stream().mapToInt(SizeChanceWithoutItem::getChance).sum();
    return getSizeChanceWithoutItem(SizeChance, totalWeight);
  }

  @NotNull
  private static SizeChanceWithoutItem getSizeChanceWithoutItem(List<SizeChanceWithoutItem> SizeChance, int totalWeight) {
    int randomValue = Utils.RANDOM.nextInt(totalWeight) + 1;

    int currentWeight = 0;
    for (SizeChanceWithoutItem sizeChance : SizeChance) {
      currentWeight += sizeChance.getChance();
      if (randomValue <= currentWeight) {
        return sizeChance;
      }
    }
    return new SizeChanceWithoutItem();
  }

  /**
   * check if the size exists in the list of sizes
   *
   * @return
   */
  public static ScalePokemonData getScalePokemonData(Pokemon pokemon) {
    ScalePokemonData targetData = new ScalePokemonData(
      pokemon.getSpecies().showdownId(),
      pokemon.getForm().getName()
    );
    List<ScalePokemonData> specifiedSizes = CobbleUtils.config.getSpecifiedSizes();

    if (specifiedSizes == null || specifiedSizes.isEmpty()) {
      return ScalePokemonData.transformDefaultSizes();
    }


    return specifiedSizes.stream()
      .filter(scalePokemonData -> {
        if (scalePokemonData.getSizes() == null || scalePokemonData.getSizes().isEmpty()) {
          return false;
        }
        return equals(scalePokemonData, targetData);
      })
      .findFirst()
      .orElse(null);
  }

  private static ScalePokemonData transformDefaultSizes() {
    return new ScalePokemonData("default", "default", SizeChanceWithoutItem.transform(CobbleUtils.config.getPokemonsizes()));
  }

  /**
   * Obtiene un tamaño de Pokémon basado en las probabilidades configuradas.
   */
  public SizeChanceWithoutItem getRandomPokemonSize() {
    int totalWeight = sizes.stream().mapToInt(SizeChanceWithoutItem::getChance).sum();
    return getSizeChanceWithoutItem(sizes, totalWeight);
  }

  /**
   * Obtain a size of a pokemon based on the size id
   *
   * @param pokemon Pokemon to get the size
   *
   * @return SizeChanceWithoutItem with the id and size
   */
  public static SizeChanceWithoutItem getSize(Pokemon pokemon, String sizeId) {
    if (!sizeId.equalsIgnoreCase("aleatory")) {
      return getScalePokemonData(pokemon)
        .getSizes()
        .stream()
        .filter(sizeChanceWithoutItem -> sizeChanceWithoutItem.getId().equalsIgnoreCase(sizeId))
        .findFirst()
        .orElse(new SizeChanceWithoutItem());
    } else {
      return getScalePokemonData(pokemon).getRandomPokemonSize();
    }
  }

  /**
   * Check if the size exists in the list of sizes
   *
   * @param pokemon Pokemon
   *
   * @return boolean
   */
  public boolean existSize(Pokemon pokemon) {
    String size = pokemon.getPersistentData().getString("size");
    if (size.isEmpty()) return false;
    return getScalePokemonData(pokemon)
      .getSizes()
      .stream()
      .anyMatch(sizeChanceWithoutItem -> sizeChanceWithoutItem.getId().equalsIgnoreCase(size));
  }

  public SizeChanceWithoutItem getSize(Pokemon pokemon) {
    String size = pokemon.getPersistentData().getString("size");
    return getScalePokemonData(pokemon)
      .getSizes()
      .stream()
      .filter(sizeChanceWithoutItem -> sizeChanceWithoutItem.getId().equalsIgnoreCase(size))
      .findFirst()
      .orElse(new SizeChanceWithoutItem());
  }
}
