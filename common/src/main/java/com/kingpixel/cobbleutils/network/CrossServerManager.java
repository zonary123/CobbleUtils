package com.kingpixel.cobbleutils.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class CrossServerManager {

  public static void register() {
    if (true) return;
    // Registro del codec del ProxyPacket
    PayloadTypeRegistry.playS2C().register(ProxyPacket.PACKET_ID, ProxyPacket.codec);

    // Listener para recibir respuesta de Bungee
    ServerPlayNetworking.registerGlobalReceiver(ProxyPacket.PACKET_ID,
      (payload, context) -> {
        String action = payload.args()[0];

        switch (action) {
          case "GetServers" -> {
            String[] servers = payload.args(); // aquí podrías recibir la lista de Bungee
            ServerCache.updateServers(servers);
          }
          case "Connect" -> {
            // No hace nada normalmente, solo se envía al jugador
          }
          case "GetServer" -> {
            // manejar si quieres recibir servidor actual
          }
        }
      }
    );


  }
}
