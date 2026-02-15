package com.kingpixel.cobbleutils.mixins.events;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Carlos Varas Alonso - 06/08/2025 21:39
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class CommandMixin {
  @Shadow
  public ServerPlayerEntity player;

  @Inject(method = "executeCommand", at = @At("HEAD"))
  private void CobbleQuests$onKilledOther(String command, CallbackInfo ci) {
    if (player == null) return;


  }
}
