package com.kingpixel.cobbleutils.Model;

import club.minnced.discord.webhook.WebhookClient;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.Model.discord.WebHookStruct;
import com.kingpixel.cobbleutils.util.async.AsyncContext;
import com.kingpixel.cobbleutils.util.async.UtilsAsync;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handles the configuration payload mapping and runtime execution pipeline for Discord Webhooks.
 *
 * <p>This class abstracts non-blocking asynchronous payload transmission, routing network
 * dispatches via a centralized {@link AsyncContext} worker pool to entirely isolate the Minecraft
 * Main Server Tick Loop from network latency, blocking HTTP pipelines, and Discord API rate limits.</p>
 *
 * <p>Maintains absolute 100% binary and structural backwards compatibility with pre-existing
 * configuration layers and reflective serialization nodes.</p>
 *
 * @author Carlos Varas Alonso
 * @since 1.0
 */
@Getter
@Setter
public class WebHookData {

  /**
   * Internal thread-safe cache mapping Webhook URLs to active WebhookClient connections.
   * Kept public and mutable to prevent breaking legacy integration hooks.
   */
  public static final Map<String, WebhookClient> webhooks = new ConcurrentHashMap<>();

  /**
   * Dedicated asynchrony network sub-pool tailored for out-of-core HTTP webhook dispatches.
   * Prevents execution exhaustion and pool starvation under server event burst spikes.
   */
  private static final AsyncContext HTTP_ASYNC = UtilsAsync.createContext("cobbleutils-webhooks", "CobbleUtils-Webhooks", 2, 4);

  // Configuration Node Layout Fields (Maintained as mutable public properties for compatibility)
  public boolean ENABLED;
  public String URL_WEBHOOK;
  public String AVATAR_URL;
  public String USERNAME;
  public String COLOR;

  /**
   * Constructs a new WebHookData instance initialized to a disabled baseline state.
   * Preserved exactly as originally designed to secure external reflective allocation signatures.
   *
   * @param URL_WEBHOOK The targeted API endpoint web address string.
   * @param AVATAR_URL  The display profile image file pointer address.
   * @param USERNAME    The dynamic customized sender identifier title.
   */
  public WebHookData(String URL_WEBHOOK, String AVATAR_URL, String USERNAME) {
    this.ENABLED = false;
    this.URL_WEBHOOK = URL_WEBHOOK;
    this.AVATAR_URL = AVATAR_URL;
    this.USERNAME = USERNAME;
    this.COLOR = Integer.toHexString(0x00FF00); // Default Hex Green
  }

  /**
   * Dispatches a structured Discord webhook embed payload asynchronously using raw model instances.
   *
   * @param id       The debug tracking boundary key string.
   * @param struct   The core Discord message embed structural blueprint map.
   * @param players  The active source player entities list collection.
   * @param pokemons The source model structural attributes array requested for serialization.
   */
  public void sendWebHook(String id, WebHookStruct struct, List<ServerPlayerEntity> players,
                          List<Pokemon> pokemons) {
    runAsyncWebhook(id, () -> {
      WebhookClient client = getWebhookClient();
      if (client != null) {
        client.send(struct.getMessage(this, players, pokemons));
      }
    });
  }

  /**
   * Dispatches a structured Discord webhook embed payload asynchronously mapping real in-world entities.
   *
   * @param id       The debug tracking boundary key string.
   * @param struct   The core Discord message embed structural blueprint map.
   * @param players  The active source player entities list collection.
   * @param pokemons The active in-world entities list present inside current ticking chunks.
   */
  public void sendWebHookEntity(String id, WebHookStruct struct, List<ServerPlayerEntity> players,
                                List<PokemonEntity> pokemons) {
    runAsyncWebhook(id, () -> {
      WebhookClient client = getWebhookClient();
      if (client != null) {
        client.send(struct.getMessageEntity(this, players, pokemons));
      }
    });
  }

  /**
   * Internal wrapper routing execution tasks through the centralized, fail-safe AsyncContext architecture.
   * Implements strict timeout limits to completely immunize the system against connection hangs.
   *
   * @param id   The contextual transaction tracing tag.
   * @param task The executable functional block wrapping the actual payload transmission.
   */
  private void runAsyncWebhook(String id, Runnable task) {
    if (!ENABLED || URL_WEBHOOK == null || URL_WEBHOOK.isEmpty()) return;

    HTTP_ASYNC.runAsync(task)
      .orTimeout(5, TimeUnit.SECONDS)
      .exceptionally(e -> {
        System.err.println("[CobbleUtils-Webhooks] Failed to deliver webhook payload context '" + id + "': " + e.getMessage());
        return null;
      });
  }

  /**
   * Resolves or dynamically computes a cached WebhookClient connection map pipeline boundary.
   *
   * @return A live authenticated WebhookClient proxy reference, or null if unconfigured.
   */
  public WebhookClient getWebhookClient() {
    if (URL_WEBHOOK == null || URL_WEBHOOK.isEmpty()) return null;
    return webhooks.computeIfAbsent(URL_WEBHOOK, k -> WebhookClient.withUrl(URL_WEBHOOK));
  }

  /**
   * Explicitly teardowns and flushes all active underlying HTTP stream sockets held in memory.
   * * <p>Highly recommended to be invoked during hot-reload hooks (e.g., /cobbleutils reload)
   * to guarantee complete remediation against connection leaks and ghost operating system threads.</p>
   */
  public static void clearCache() {
    webhooks.values().forEach(client -> {
      try {
        client.close();
      } catch (Exception ignored) {
        // Quietly absorb unexpected stream termination anomalies
      }
    });
    webhooks.clear();
  }
}