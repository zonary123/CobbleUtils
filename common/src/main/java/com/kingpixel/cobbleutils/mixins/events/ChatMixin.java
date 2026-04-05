package com.kingpixel.cobbleutils.mixins.events;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ChatMixin {

  @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
  private void onChatMessage(ChatMessageC2SPacket packet, CallbackInfo ci) {

  }
}
