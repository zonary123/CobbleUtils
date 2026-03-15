package com.kingpixel.cobbleutils.network;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class NetworkInitializer {
  public static void register() {
    if (CobbleUtils.config.isRedisMessaging()) {
      try {
        PayloadTypeRegistry.playS2C().register(ProxyPacket.PACKET_ID, ProxyPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ProxyPacket.PACKET_ID, ProxyPacket.CODEC);
      } catch (IllegalArgumentException ignored) {
      } finally {
        
      }
    }
  }
}
