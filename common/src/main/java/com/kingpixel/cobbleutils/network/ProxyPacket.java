package com.kingpixel.cobbleutils.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public record ProxyPacket(String... args) implements CustomPayload {

  public static final Identifier BUNGEECORD_ID = Identifier.of("bungeecord", "main");
  public static final CustomPayload.Id<ProxyPacket> PACKET_ID = new CustomPayload.Id<>(BUNGEECORD_ID);

  public static final PacketCodec<RegistryByteBuf, ProxyPacket> codec = new PacketCodec<>() {
    @Override
    public ProxyPacket decode(RegistryByteBuf buf) {
      return null; // Solo envío, no recibo
    }

    @Override
    public void encode(RegistryByteBuf buf, ProxyPacket value) {
      try {
        DataOutputStream out = new DataOutputStream(new OutputStream() {
          @Override
          public void write(int b) {
            buf.writeByte(b);
          }
        });

        for (String arg : value.args()) {
          out.writeUTF(arg);
        }

      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  };

  @Override
  public Id<? extends CustomPayload> getId() {
    return PACKET_ID;
  }

  // Método de ayuda para enviar a un jugador
  public static void send(ServerPlayerEntity player, String... args) {
    PacketByteBuf buf = new PacketByteBuf(net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create());
    for (String arg : args) {
      buf.writeString(arg);
    }
  }
}
