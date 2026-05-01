package com.kingpixel.cobbleutils.network;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.pokeskies.fabricpluginmessaging.PluginMessagePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public class ProxyPacket {
  private ProxyPacket() {
    /* This utility class should not be instantiated */
  }

  public static void sendServer(ServerPlayerEntity player, String server) {
    ByteArrayDataOutput outputStream = ByteStreams.newDataOutput();
    outputStream.writeUTF("Connect");
    outputStream.writeUTF(server);
    ServerPlayNetworking.send(player, new PluginMessagePacket(outputStream.toByteArray()));
  }
}
