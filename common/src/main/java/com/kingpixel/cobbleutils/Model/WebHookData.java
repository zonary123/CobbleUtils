package com.kingpixel.cobbleutils.Model;

import club.minnced.discord.webhook.WebhookClient;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kingpixel.cobbleutils.Model.discord.WebHookStruct;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@Setter
public class WebHookData {
  public static final Map<String, WebhookClient> webhooks = new ConcurrentHashMap<>();


  private static final ExecutorService WEBHOOK_THREAD_FACTORY = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
    .setDaemon(true)
    .setNameFormat("CobbleUtils Webhook - %d")
    .build());

  // Webhook configuration
  private final boolean ENABLED;
  private final String URL_WEBHOOK;
  private final String AVATAR_URL;
  private final String USERNAME;
  private final String COLOR;

  public WebHookData(String URL_WEBHOOK, String AVATAR_URL, String USERNAME) {
    this.ENABLED = false;
    this.URL_WEBHOOK = URL_WEBHOOK;
    this.AVATAR_URL = AVATAR_URL;
    this.USERNAME = USERNAME;
    this.COLOR = Integer.toHexString(0x00FF00);
  }

  /**
   * Send a webhook with Pokemon (not entity).
   */
  public void sendWebHook(String id, WebHookStruct struct, List<ServerPlayerEntity> players,
                          List<Pokemon> pokemons) {
    runAsyncWebhook(id, () -> {
      WebhookClient client = webhooks.computeIfAbsent(URL_WEBHOOK, k -> WebhookClient.withUrl(URL_WEBHOOK));
      client.send(struct.getMessage(this, players, pokemons));
    });
  }


  /**
   * Send a webhook with PokemonEntity (in-world).
   */
  public void sendWebHookEntity(String id, WebHookStruct struct, List<ServerPlayerEntity> players,
                                List<PokemonEntity> pokemons) {
    runAsyncWebhook(id, () -> {
      WebhookClient client = webhooks.computeIfAbsent(URL_WEBHOOK, k -> WebhookClient.withUrl(URL_WEBHOOK));
      client.send(struct.getMessageEntity(this, players, pokemons));
    });
  }

  /**
   * Runs a webhook task asynchronously with timeout and exception handling.
   */
  private void runAsyncWebhook(String id, Runnable task) {
    if (!ENABLED || URL_WEBHOOK == null || URL_WEBHOOK.isEmpty()) return;
    CompletableFuture.runAsync(task, WEBHOOK_THREAD_FACTORY)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
  }
}
