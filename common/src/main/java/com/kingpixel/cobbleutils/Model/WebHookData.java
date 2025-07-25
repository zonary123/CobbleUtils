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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public class WebHookData {
  public static final Map<String, WebhookClient> webhooks = new ConcurrentHashMap<>();


  private static final ThreadFactory WEBHOOK_THREAD_FACTORY = new ThreadFactory() {
    private final AtomicInteger count = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r);
      t.setName("webhook-Cobbleutils-Discord-" + count.getAndIncrement());
      t.setDaemon(true);
      return t;
    }
  };

  // Fixed thread pool with custom thread naming
  private static final ExecutorService EXECUTOR =
    Executors.newFixedThreadPool(2, WEBHOOK_THREAD_FACTORY);

  // Webhook configuration
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
   * Send a webhook with Pokemon (not entity).
   */
  public void sendWebHook(String id, WebHookStruct struct, List<ServerPlayerEntity> players,
                          List<Pokemon> pokemons) {
    runAsyncWebhook(id, () -> {
      WebhookClient client = webhooks.computeIfAbsent(id, k -> WebhookClient.withUrl(URL_WEBHOOK));
      client.send(struct.getMessage(this, players, pokemons));
    });
  }

  /**
   * Send a webhook with PokemonEntity (in-world).
   */
  public void sendWebHookEntity(String id, WebHookStruct struct, List<ServerPlayerEntity> players,
                                List<PokemonEntity> pokemons) {
    runAsyncWebhook(id, () -> {
      WebhookClient client = webhooks.computeIfAbsent(id, k -> WebhookClient.withUrl(URL_WEBHOOK));
      client.send(struct.getMessageEntity(this, players, pokemons));
    });
  }

  /**
   * Runs a webhook task asynchronously with timeout and exception handling.
   */
  private void runAsyncWebhook(String id, Runnable task) {
    if (!ENABLED || URL_WEBHOOK == null || URL_WEBHOOK.isEmpty()) return;
    CompletableFuture.runAsync(task, EXECUTOR)
      .orTimeout(5, TimeUnit.SECONDS)
      .exceptionally(e -> {
        System.err.println("[Webhook-CobbleUtils][" + id + "] Error: " + e.getMessage());
        e.printStackTrace();
        return null;
      });
  }
}
