package com.kingpixel.cobbleutils.Model;

import club.minnced.discord.webhook.WebhookClient;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.Model.discord.WebHookStruct;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;

/**
 * @author Carlos Varas Alonso - 19/11/2024 2:16
 */
@Getter
@Setter
public class WebHookData {
  public static Map<String, WebhookClient> webhooks;

  private boolean ENABLED;
  private String URL_WEBHOOK;
  private String AVATAR_URL;
  private String USERNAME;
  private String COLOR;

  public WebHookData(String URL_WEBHOOK, String AVATAR_URL, String USERNAME) {
    this.ENABLED = false;
    this.URL_WEBHOOK = URL_WEBHOOK;
    this.AVATAR_URL = AVATAR_URL;
    this.USERNAME = USERNAME;
    this.COLOR = Integer.toHexString(0x00FF00);
  }

  /**
   * Send a message to the webhook
   *
   * @param id       The id of the webhook
   * @param struct   The struct of the message
   * @param players  The players to send the message
   * @param pokemons The pokemons to send the message
   */
  public void sendWebHook(String id, WebHookStruct struct, List<ServerPlayerEntity> players,
                          List<Pokemon> pokemons) {
    if (!ENABLED || URL_WEBHOOK.isEmpty()) return;
    WebhookClient client = webhooks.get(id);
    if (client == null) {
      client = WebhookClient.withUrl(URL_WEBHOOK);
      webhooks.put(id, client);
    }
    client.send(struct.getMessage(this, players, pokemons));
  }

  public void sendWebHookEntity(String id, WebHookStruct struct, List<ServerPlayerEntity> players,
                                List<PokemonEntity> pokemons) {
    if (!ENABLED || URL_WEBHOOK.isEmpty()) return;
    WebhookClient client = webhooks.get(id);
    if (client == null) {
      client = WebhookClient.withUrl(URL_WEBHOOK);
      webhooks.put(id, client);
    }
    client.send(struct.getMessageEntity(this, players, pokemons));
  }

}
