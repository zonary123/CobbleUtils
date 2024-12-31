package com.kingpixel.cobbleutils.Model.discord;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.Model.WebHookData;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 19/11/2024 4:13
 */
@Getter
@Setter
public class WebHookStruct {
  private String content;
  private List<Embed> embeds;

  public WebHookStruct() {
    this.content = "";
    this.embeds = List.of(
      new Embed("Title", "Description")
    );
  }

  public WebHookStruct(String content, List<Embed> embeds) {
    this.content = content;
    this.embeds = embeds;
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
    WebhookMessageBuilder builder = new WebhookMessageBuilder()
      .setUsername(data.getUSERNAME())
      .setAvatarUrl(data.getAVATAR_URL());

    if (content != null && !content.isEmpty()) {
      builder.setContent(content);
    }

    if (embeds != null && !embeds.isEmpty()) {
      for (int i = 0; i < embeds.size(); i++) {
        String description = getEmbedDescription(player, getPokemon(i, pokemons), embeds.get(i).getDescription());
        description = description.replaceAll("<[^>]*>", "")
          .replaceAll("[&§].", "");
        WebhookEmbed webhookEmbed = new WebhookEmbedBuilder()
          .setColor(Integer.parseInt(embeds.get(i).getColor(), 16))
          .setTitle(new WebhookEmbed.EmbedTitle(embeds.get(i).getTitle(), null))
          .setDescription(description)
          .build();
        builder.addEmbeds(webhookEmbed);
      }
    }


    return builder.build();
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
        String description = getEmbedDescription(getPlayer(i, players), getPokemon(i, pokemons),
          embeds.get(i).getDescription());
        description = description.replaceAll("<[^>]*>", "")
          .replaceAll("[&§].", "");
        WebhookEmbed webhookEmbed = new WebhookEmbedBuilder()
          .setColor(Integer.parseInt(embeds.get(i).getColor(), 16))
          .setTitle(new WebhookEmbed.EmbedTitle(embeds.get(i).getTitle(), null))
          .setDescription(description)
          .build();
        builder.addEmbeds(webhookEmbed);
      }
    }


    return builder.build();
  }

  private ServerPlayerEntity getPlayer(int i, List<ServerPlayerEntity> players) {
    if (players == null || players.isEmpty()) return null;
    if (i < players.size()) return players.get(i);
    return players.get(0);
  }

  private Pokemon getPokemon(int i, List<Pokemon> pokemons) {
    if (pokemons == null || pokemons.isEmpty()) return null;
    if (i < pokemons.size()) return pokemons.get(i);
    return pokemons.get(0);
  }

  private String getEmbedDescription(ServerPlayerEntity player, Pokemon pokemon, String description) {
    description = description
      .replace("%player%", player == null ? "Server" : player.getGameProfile().getName());

    if (pokemon != null) {
      description = description.replace("%pokemon%", pokemon.getDisplayName().getString())
        .replace("%ability%", pokemon.getAbility().getName())
        .replace("%nature%", pokemon.getNature().getName().getNamespace());
    }

    return PokemonUtils.replace(description, pokemon);
  }
}
