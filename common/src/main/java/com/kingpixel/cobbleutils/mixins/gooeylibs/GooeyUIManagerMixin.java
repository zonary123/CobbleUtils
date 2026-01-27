package com.kingpixel.cobbleutils.mixins.gooeylibs;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.container.GooeyContainer;
import ca.landonjw.gooeylibs2.api.page.Page;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mixin para forzar que todo UIManager se ejecute en el hilo principal.
 * Evita problemas de contenedores abiertos desde threads secundarios.
 */
@Mixin(UIManager.class)
public abstract class GooeyUIManagerMixin {

    @Unique
    private static final long COOLDOWN_MS = 500L;
    @Unique
    private static final int MAX_ATTEMPTS = 5;

    @Unique
    private static final Map<UUID, Long> LAST_ATTEMPT = new ConcurrentHashMap<>();
    @Unique
    private static final Map<UUID, Integer> ATTEMPTS = new ConcurrentHashMap<>();

    @Inject(
            method = "openUIForcefully",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void runOnMainThreadOpenUIForcefully(
            ServerPlayerEntity player,
            Page page,
            CallbackInfo ci
    ) {
        if (CobbleUtils.server.isOnThread()) return;

        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();
        long last = LAST_ATTEMPT.getOrDefault(uuid, 0L);

        if (now - last > COOLDOWN_MS) {
            ATTEMPTS.remove(uuid);
        }

        int attempts = ATTEMPTS.getOrDefault(uuid, 0);
        if (attempts >= MAX_ATTEMPTS) return;

        LAST_ATTEMPT.put(uuid, now);
        ATTEMPTS.put(uuid, attempts + 1);

        ci.cancel();

        CobbleUtils.server.execute(() -> {
            try {
                if (player.isRemoved()) return;
                GooeyContainer container = new GooeyContainer(player, page);
                container.open();
                ATTEMPTS.remove(uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}



