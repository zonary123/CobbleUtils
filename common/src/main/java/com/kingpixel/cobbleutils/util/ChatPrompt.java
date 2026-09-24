package com.kingpixel.cobbleutils.util;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Utility to intercept chat input from players for GUI editors and configuration prompts.
 */
public final class ChatPrompt {
  private static final Map<UUID, Consumer<String>> ACTIVE_PROMPTS = new ConcurrentHashMap<>();
  private static volatile boolean initialized = false;

  private ChatPrompt() {
  }

  /**
   * Initializes the chat interception listener.
   */
  public static synchronized void init() {
    if (initialized) return;
    initialized = true;

    ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
      UUID playerId = sender.getUuid();
      if (!ACTIVE_PROMPTS.containsKey(playerId)) return true;

      Consumer<String> callback = ACTIVE_PROMPTS.remove(playerId);
      if (callback != null) {
        String text = message.getContent().getString();
        sender.getServer().execute(() -> {
          if (text.equalsIgnoreCase("cancel")) {
            callback.accept(null);
          } else {
            callback.accept(text);
          }
        });
        return false;
      }
      return true;
    });
  }

  /**
   * Prompts the player in chat for input and closes their current GUI screen.
   *
   * @param player the player to prompt
   * @param promptMessage message displayed to the player
   * @param onInput callback receiving input or null if cancelled
   */
  public static void prompt(ServerPlayerEntity player, String promptMessage, Consumer<String> onInput) {
    if (player == null) return;
    init();
    player.closeHandledScreen();
    ACTIVE_PROMPTS.put(player.getUuid(), onInput);
    player.sendMessage(Text.literal("§6[Editor] §e" + promptMessage));
    player.sendMessage(Text.literal("§7Type §ccancel §7to abort."));
  }

  /**
   * Cancels any pending input prompt for the player.
   *
   * @param playerId player UUID
   */
  public static void cancel(UUID playerId) {
    ACTIVE_PROMPTS.remove(playerId);
  }

  /**
   * Checks if player has a pending prompt.
   *
   * @param playerId player UUID
   * @return true if waiting for input
   */
  public static boolean hasPendingInput(UUID playerId) {
    return ACTIVE_PROMPTS.containsKey(playerId);
  }
}
