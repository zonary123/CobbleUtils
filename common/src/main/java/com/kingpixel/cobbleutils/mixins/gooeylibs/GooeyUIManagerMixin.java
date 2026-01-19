package com.kingpixel.cobbleutils.mixins.gooeylibs;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.container.GooeyContainer;
import ca.landonjw.gooeylibs2.api.page.Page;
import ca.landonjw.gooeylibs2.api.tasks.Task;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin para forzar que todo UIManager se ejecute en el hilo principal.
 * Evita problemas de contenedores abiertos desde threads secundarios.
 */
@Mixin(UIManager.class)
public abstract class GooeyUIManagerMixin {

  @Inject(
    method = "openUIForcefully",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void runOnMainThreadOpenUIForcefully(ServerPlayerEntity player, Page page, CallbackInfo ci) {
    ci.cancel();
    CobbleUtils.server.execute(() -> {
      try {
        Task.builder()
          .execute(() -> {
            try {
              GooeyContainer container = new GooeyContainer(player, page);
              container.open();
            } catch (Exception e) {
              e.printStackTrace();
            }
          })
          .build();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}

