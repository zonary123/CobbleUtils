package com.kingpixel.cobbleutils.features.breeding;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.database.DatabaseClientFactory;
import com.kingpixel.cobbleutils.features.breeding.events.*;
import com.kingpixel.cobbleutils.features.breeding.manager.ManagerPlotEggs;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.RewardsUtils;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import kotlin.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * @author Carlos Varas Alonso - 23/07/2024 9:24
 */
public class Breeding {
  public static ManagerPlotEggs managerPlotEggs = new ManagerPlotEggs();
  public static Map<UUID, UserInfo> playerCountry = new ConcurrentHashMap<>();
  private static final String API_URL_IP = "http://ip-api.com/json/";
  private static boolean active = false;
  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private static final List<ScheduledFuture<?>> scheduledTasks = new CopyOnWriteArrayList<>();

  public static void register() {
    if (!active) {
      events();
      active = true;
    }

    for (ScheduledFuture<?> task : scheduledTasks) {
      task.cancel(true);
    }
    scheduledTasks.clear();

    // Crear una nueva tarea
    ScheduledFuture<?> checkegg = scheduler.scheduleAtFixedRate(() -> {
      try {
        // Checking eggs
        CobbleUtils.server.getPlayerManager().getPlayerList().forEach(player -> {
          DatabaseClientFactory.databaseClient.checkDaycarePlots(player);
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }, 0, CobbleUtils.breedconfig.getCheckEggToBreedInSeconds(), TimeUnit.SECONDS);

    scheduledTasks.add(checkegg);
  }

  private static void events() {
    PlayerEvent.PLAYER_JOIN.register(player -> {
      DatabaseClientFactory.databaseClient.getPlots(player);
      countryPlayer(player);
    });

    CobblemonEvents.POKEMON_HEALED.subscribe(Priority.HIGHEST, pokemon -> {
      if (pokemon.getPokemon().getSpecies().showdownId().equalsIgnoreCase("egg")) {
        pokemon.cancel();
      }
      return Unit.INSTANCE;
    });

    PlayerEvent.PLAYER_QUIT.register(player -> {
      // Remove country data
      playerCountry.remove(player.getUuid());
      // Remove data
      DatabaseClientFactory.databaseClient.removeDataIfNecessary(player);

    });


    LifecycleEvent.SERVER_STOPPING.register(instance -> {
      for (ScheduledFuture<?> task : scheduledTasks) {
        task.cancel(true);
      }
      scheduledTasks.clear();
      HatchEggEvent.HATCH_EGG_EVENT.clear();
      scheduler.shutdown();
      try {
        if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
        }
      } catch (InterruptedException ex) {
        scheduler.shutdownNow(); // Interrupción forzada
      }
    });

    PlayerEvent.ATTACK_ENTITY.register((player, level, target, hand, result) -> egg(target, PlayerUtils.castPlayer(player)));

    InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> egg(entity, PlayerUtils.castPlayer(player)));

    WalkBreeding.register();
    EggThrow.register();
    PastureUI.register();

    NationalityPokemon.register();
  }

  private static EventResult egg(Entity entity, ServerPlayerEntity player) {
    try {
      if (entity == null)
        return EventResult.pass();
      if (entity instanceof PokemonEntity pokemonEntity) {
        Pokemon pokemon = pokemonEntity.getPokemon();
        if (pokemon.getSpecies().showdownId().equalsIgnoreCase("egg")) {
          if (pokemon.getPersistentData().getBoolean("EggSpawned")) {
            pokemon.getPersistentData().remove("EggSpawned");
            CobbleUtils.LOGGER.info("persistentdata: " + pokemon.getPersistentData());
            try {
              RewardsUtils.saveRewardPokemon(player, pokemon);
            } catch (NoPokemonStoreException e) {
              throw new RuntimeException(e);
            }
            pokemonEntity.remove(Entity.RemovalReason.KILLED);
          }
        }
        return EventResult.pass();
      }
      return EventResult.pass();
    } catch (Exception e) {
      e.printStackTrace();
      return EventResult.pass();
    }
  }

  public record UserInfo(String country, String countryCode, String language) {
  }

  private static void countryPlayer(ServerPlayerEntity player) {
    if (playerCountry.get(player.getUuid()) != null) return; // Verifica si ya se obtuvo la información del jugador

    CompletableFuture.runAsync(() -> {
      try {
        URL url = new URL(API_URL_IP + player.getIp());
        // Establece la conexión HTTP con la API
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // Usar try-with-resources para asegurar el cierre de BufferedReader
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
          // Parsear la respuesta en un JsonObject
          JsonObject json = JsonParser.parseReader(in).getAsJsonObject();

          // Verifica si el JSON tiene la información del país
          if (json.has("country")) {
            String country = json.get("country").getAsString();
            String countryCode = json.get("countryCode").getAsString();

            // Determina el idioma según el código del país
            String language = switch (countryCode) {
              case "AR", "ES" -> "es";
              case "US", "GB", "AU" -> "en";
              default -> "en"; // Idioma por defecto
            };

            // Crea y almacena la información del usuario
            UserInfo userInfo = new UserInfo(country, countryCode, language);
            playerCountry.put(player.getUuid(), userInfo);
          }
        } finally {
          conn.disconnect(); // Desconectar la conexión HTTP
        }

      } catch (Exception e) {
        // Maneja cualquier excepción que ocurra durante la solicitud
        e.printStackTrace();
      }
    });
  }
}
