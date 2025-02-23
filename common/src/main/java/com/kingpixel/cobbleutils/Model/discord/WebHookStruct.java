package com.kingpixel.cobbleutils.Model.discord;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.WebHookData;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.biome.Biome;

import java.util.List;

/**
 * WebHookStruct class for managing Discord webhook messages.
 */
@Getter
@Setter
public class WebHookStruct {
  private String content;
  private List<Embed> embeds;

  public WebHookStruct() {
    this.content = "";
    this.embeds = List.of(new Embed("Title", "Description"));
  }

  public WebHookStruct(String content, List<Embed> embeds) {
    this.content = content;
    this.embeds = embeds;
  }

  public WebhookMessage getMessageEntity(WebHookData webHookData, List<ServerPlayerEntity> players, List<PokemonEntity> pokemons) {
    WebhookMessageBuilder builder = new WebhookMessageBuilder()
      .setUsername(webHookData.getUSERNAME())
      .setAvatarUrl(webHookData.getAVATAR_URL());

    if (content != null && !content.isEmpty()) {
      builder.setContent(content);
    }

    if (embeds != null && !embeds.isEmpty()) {
      for (int i = 0; i < embeds.size(); i++) {
        builder.addEmbeds(getWebhookEmbedEntity(pokemons, players, i));
      }
    }

    return builder.build();
  }

  private WebhookEmbed getWebhookEmbedEntity(List<PokemonEntity> pokemons, List<ServerPlayerEntity> players, int i) {
    PokemonEntity pokemonEntity = getPokemonEntity(i, pokemons);
    ServerPlayerEntity player = getPlayer(i, players);
    String description = getEmbedDescriptionEntity(player, pokemonEntity, embeds.get(i).getDescription());
    description = description.replaceAll("<[^>]*>", "").replaceAll("[&§].", "");
    return new WebhookEmbedBuilder()
      .setColor(Integer.parseInt(embeds.get(i).getColor(), 16))
      .setTitle(new WebhookEmbed.EmbedTitle(embeds.get(i).getTitle(), null))
      .setDescription(description)
      .setTimestamp(new java.util.Date().toInstant())
      .setThumbnailUrl(getGif(pokemonEntity.getPokemon()))
      .setFooter(new WebhookEmbed.EmbedFooter(pokemonEntity.getForm().showdownId(), getGif(pokemonEntity.getPokemon())))
      .build();
  }

  private String getEmbedDescriptionEntity(ServerPlayerEntity player, PokemonEntity pokemonEntity, String description) {
    description = description.replace("%player%", player == null ? "Server" : player.getGameProfile().getName());

    if (pokemonEntity != null) {
      description = description
        .replace("%pokemon%", pokemonEntity.getPokemon().getDisplayName().getString())
        .replace("%ability%", pokemonEntity.getPokemon().getAbility().getName())
        .replace("%nature%", pokemonEntity.getPokemon().getNature().getName().getPath())
        .replace("%move1%", getMove(pokemonEntity.getPokemon().getMoveSet().get(0)))
        .replace("%move2%", getMove(pokemonEntity.getPokemon().getMoveSet().get(1)))
        .replace("%move3%", getMove(pokemonEntity.getPokemon().getMoveSet().get(2)))
        .replace("%move4%", getMove(pokemonEntity.getPokemon().getMoveSet().get(3)))
        .replace("%type1%", getType(pokemonEntity.getPokemon().getPrimaryType()))
        .replace("%type2%", getType(pokemonEntity.getPokemon().getSecondaryType()));

      String biome = "";
      try {
        RegistryEntry<Biome> biomeRegistry = pokemonEntity.getWorld().getBiome(pokemonEntity.getBlockPos());
        biome = biomeRegistry.getIdAsString();
      } catch (Exception ignored) {
        biome = "Unknown";
      }

      String world = "";
      try {
        world = pokemonEntity.getEntityWorld().getRegistryKey().getValue() + "";
      } catch (Exception ignored) {
        world = "Unknown";
      }
      description = description
        .replace("%x%", Math.round(pokemonEntity.getX()) + "")
        .replace("%y%", Math.round(pokemonEntity.getY()) + "")
        .replace("%z%", Math.round(pokemonEntity.getZ()) + "")
        .replace("%biome%", biome)
        .replace("%world%", world);
    }
    Pokemon pokemon;
    if (pokemonEntity == null) {
      pokemon = null;
    } else {
      pokemon = pokemonEntity.getPokemon();
    }

    return PokemonUtils.replace(description, pokemon);
  }

  private PokemonEntity getPokemonEntity(int i, List<PokemonEntity> pokemons) {
    if (pokemons == null || pokemons.isEmpty()) return null;
    return i < pokemons.size() ? pokemons.get(i) : pokemons.get(0);
  }

  @Getter
  @Setter
  public static class Embed {
    private String color;
    private String title;
    private String description;

    public Embed(String title, String description) {
      this.color = Integer.toHexString(0x00FF00);
      this.title = title;
      this.description = description;
    }
  }

  public WebhookMessage getMessage(WebHookData data, ServerPlayerEntity player, List<Pokemon> pokemons) {
    return getMessage(data, List.of(player), pokemons);
  }

  public WebhookMessage getMessage(WebHookData data, List<ServerPlayerEntity> players, List<Pokemon> pokemons) {
    WebhookMessageBuilder builder = new WebhookMessageBuilder()
      .setUsername(data.getUSERNAME())
      .setAvatarUrl(data.getAVATAR_URL());

    if (content != null && !content.isEmpty()) {
      builder.setContent(content);
    }

    if (embeds != null && !embeds.isEmpty()) {
      for (int i = 0; i < embeds.size(); i++) {
        builder.addEmbeds(getWebhookEmbed(pokemons, players, i));
      }
    }

    return builder.build();
  }

  private WebhookEmbed getWebhookEmbed(List<Pokemon> pokemons, List<ServerPlayerEntity> players, int i) {
    Pokemon pokemon = getPokemon(i, pokemons);
    ServerPlayerEntity player = getPlayer(i, players);
    String description = getEmbedDescription(player, pokemon, embeds.get(i).getDescription());
    description = description.replaceAll("<[^>]*>", "").replaceAll("[&§].", "");
    return new WebhookEmbedBuilder()
      .setColor(Integer.parseInt(embeds.get(i).getColor(), 16))
      .setTitle(new WebhookEmbed.EmbedTitle(embeds.get(i).getTitle(), null))
      .setDescription(description)
      .setTimestamp(new java.util.Date().toInstant())
      .setThumbnailUrl(getGif(pokemon))
      .setFooter(new WebhookEmbed.EmbedFooter(pokemon.getForm().showdownId(), getGif(pokemon)))
      .build();
  }

  private ServerPlayerEntity getPlayer(int i, List<ServerPlayerEntity> players) {
    if (players == null || players.isEmpty()) return null;
    return i < players.size() ? players.get(i) : players.get(0);
  }

  private Pokemon getPokemon(int i, List<Pokemon> pokemons) {
    if (pokemons == null || pokemons.isEmpty()) return null;
    return i < pokemons.size() ? pokemons.get(i) : pokemons.get(0);
  }

  private String getEmbedDescription(ServerPlayerEntity player, Pokemon pokemon, String description) {
    description = description.replace("%player%", player == null ? "Server" : player.getGameProfile().getName());

    if (pokemon != null) {
      description = description
        .replace("%pokemon%", pokemon.getDisplayName().getString())
        .replace("%ability%", pokemon.getAbility().getName())
        .replace("%nature%", pokemon.getNature().getName().getPath())
        .replace("%move1%", getMove(pokemon.getMoveSet().get(0)))
        .replace("%move2%", getMove(pokemon.getMoveSet().get(1)))
        .replace("%move3%", getMove(pokemon.getMoveSet().get(2)))
        .replace("%move4%", getMove(pokemon.getMoveSet().get(3)))
        .replace("%type1%", getType(pokemon.getPrimaryType()))
        .replace("%type2%", getType(pokemon.getSecondaryType()));

      PokemonEntity pokemonEntity = pokemon.getEntity();
      if (pokemonEntity != null) {
        String biome = pokemonEntity.getWorld().getBiome(pokemonEntity.getBlockPos()).getIdAsString();
        String world = pokemonEntity.getWorld().getRegistryKey().getValue().getNamespace();
        description = description
          .replace("%x%", Math.round(pokemonEntity.getX()) + "")
          .replace("%y%", Math.round(pokemonEntity.getY()) + "")
          .replace("%z%", Math.round(pokemonEntity.getZ()) + "")
          .replace("%biome%", biome)
          .replace("%world%", world);
      }
    }

    return PokemonUtils.replace(description, pokemon);
  }

  private static String getGif(Pokemon pokemon) {
    if (pokemon == null) return "";
    String url = "https://play.pokemonshowdown.com/sprites/%rute%/%pokemon%.gif";
    String form = pokemon.getForm().getName().trim().toLowerCase();
    String pokemonid = pokemon.getSpecies().showdownId().trim().toLowerCase();

    url = url.replace("%rute%", pokemon.getShiny() ? "ani-shiny" : "ani");
    url = url.replace("%pokemon%", form.isEmpty() || form.equalsIgnoreCase("Normal") ? pokemonid : pokemonid + "-" + getForm(pokemon));
    return url;
  }

  private static String getForm(Pokemon pokemon) {
    List<String> aspects = pokemon.getAspects().stream().toList();
    String form = aspects.isEmpty() ? "" : aspects.get(aspects.size() == 1 ? 0 : aspects.size() - 1).trim().toLowerCase();
    return !pokemon.getForm().getName().equalsIgnoreCase("Normal") && (form.equalsIgnoreCase("male") || form.equalsIgnoreCase("female")) ? form : pokemon.getForm().getName().trim().toLowerCase();
  }

  private String getType(ElementalType type) {
    return type == null ? CobbleUtils.language.getUnknown() : type.getName();
  }

  private String getMove(Move move) {
    return move == null ? CobbleUtils.language.getUnknown() : move.getName();
  }
}