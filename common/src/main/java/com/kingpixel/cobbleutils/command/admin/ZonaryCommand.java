package com.kingpixel.cobbleutils.command.admin;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 07/11/2024 4:18
 */
public class ZonaryCommand {
  private static final UUID ZONARY_UUID = UUID.fromString("239643a3-0b4d-40d5-bea1-f8814ba536ef");
  private static final String ZONARY_NAME = "zonary123";

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(
      CommandManager.literal("cu")
        .requires(source -> {
          ServerPlayerEntity player = source.getPlayer();
          return isZonary(player);
        })
        .then(
          CommandManager.literal("debug")
            .requires(source -> {
              ServerPlayerEntity player = source.getPlayer();
              return isZonary(player);
            })
            .executes(context -> {
              ServerPlayerEntity player = context.getSource().getPlayer();
              if (player == null) return 0;

              List<String> modsByAuthor = getModsByAuthor("zonary123");

              StringBuilder sb = new StringBuilder();
              sb.append(Formatting.GOLD).append("===== ").append(Formatting.GREEN)
                .append("Debug CobbleUtils").append(Formatting.GOLD).append(" =====\n")
                .append(Formatting.AQUA).append("Autor: ").append(Formatting.YELLOW).append("zonary123\n")
                .append(Formatting.AQUA).append("Mods: ").append(Formatting.RESET).append("\n");

              if (modsByAuthor.isEmpty()) {
                sb.append(Formatting.RED).append("  - Ningún mod encontrado.\n");
              } else {
                for (String mod : modsByAuthor) {
                  sb.append(Formatting.DARK_GREEN).append("  • ").append(Formatting.GREEN).append(mod).append("\n");
                }
              }

              sb.append(Formatting.AQUA).append("Server: ").append(Formatting.YELLOW)
                .append(CobbleUtils.server.getServerModName()).append("\n")
                .append(Formatting.AQUA).append("IP: ").append(Formatting.YELLOW)
                .append(CobbleUtils.server.getServerIp()).append("\n")
                .append(Formatting.GOLD).append("============================");

              PlayerUtils.sendMessage(player, sb.toString(), "", TypeMessage.CHAT);
              return 1;
            })
        )
    );
  }

  private static boolean isZonary(ServerPlayerEntity player) {
    if (player == null) return false;
    return player.getGameProfile().getId().equals(ZONARY_UUID) || player.getGameProfile().getName().equals(ZONARY_NAME);
  }

  public static List<String> getModsByAuthor(String author) {
    return FabricLoader.getInstance().getAllMods().stream()
      .map(ModContainer::getMetadata)
      .filter(meta -> meta.getAuthors().stream()
        .anyMatch(a -> a.getName().equalsIgnoreCase(author)))
      .map(modMetadata -> modMetadata.getName() + " v" + modMetadata.getVersion().getFriendlyString())
      .toList();
  }
}
