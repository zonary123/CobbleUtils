package com.kingpixel.cobbleutils.features.breeding.events;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.features.breeding.models.EggData;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WalkBreeding {
  private static final Map<UUID, Vec3d> lastPosition = new HashMap<>();
  private static final Map<UUID, Integer> ticks = new HashMap<>();

  public static void register() {
    if (true) return;
    TickEvent.PLAYER_POST.register(player -> {
      if (isPlayerEligible(player)) {
        UUID playerId = player.getUuid();
        ticks.compute(playerId, (uuid, integer) -> integer == null ? 1 : integer + 1);

        if (ticks.get(playerId) % CobbleUtils.breedconfig.getTickstocheck() != 0) return;

        Vec3d currentPosition = new Vec3d(player.getX(), 0, player.getZ());
        Vec3d lastPos = lastPosition.computeIfAbsent(playerId, k -> currentPosition);
        double distance = currentPosition.distanceTo(lastPos);

        if (distance >= 25) {
          lastPosition.put(playerId, currentPosition);
          return;
        }

        int distanceMoved = (int) Math.min(20, distance);
        lastPosition.put(playerId, currentPosition);

        PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(PlayerUtils.castPlayer(player));
        if (playerPartyStore.size() == 0) return;

        boolean hasFlameBodyOrMagmaArmor = false;
        for (var pokemon : playerPartyStore.toGappyList()) {
          if (pokemon != null && (pokemon.getAbility().getName().equalsIgnoreCase("flamebody") ||
            pokemon.getAbility().getName().equalsIgnoreCase("magmaarmor"))) {
            hasFlameBodyOrMagmaArmor = true;
            break;
          }
        }

        for (var pokemon : playerPartyStore) {
          if (!pokemon.showdownId().equalsIgnoreCase("egg")) continue;
          pokemon.setCurrentHealth(0);
          EggData eggData = EggData.from(pokemon);
          if (eggData == null) continue;

          int steps = hasFlameBodyOrMagmaArmor ? distanceMoved * 2 : distanceMoved;
          eggData.steps(PlayerUtils.castPlayer(player), pokemon, steps);
        }
      }
    });
  }

  private static boolean isPlayerEligible(PlayerEntity player) {
    return !player.isInPose(EntityPose.FALL_FLYING)
      && !player.isInPose(EntityPose.SLEEPING)
      && (!player.hasVehicle() || isPermittedVehicle(player))
      && !player.getAbilities().flying;
  }

  private static boolean isPermittedVehicle(PlayerEntity player) {
    return player.getVehicle() instanceof BoatEntity
      || player.getVehicle() instanceof HorseEntity
      || player.getVehicle() instanceof DonkeyEntity;
  }
}