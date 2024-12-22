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

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Carlos Varas Alonso - 23/07/2024 21:49
 */
public class WalkBreeding {
  private static Map<UUID, Vec3d> lastPosition = new ConcurrentHashMap<>();
  private static Map<UUID, Integer> ticks = new ConcurrentHashMap<>() {
  };

  public static void register() {

    TickEvent.PLAYER_POST.register(player -> {
      if (!player.isInPose(EntityPose.FALL_FLYING)
        && !player.isInPose(EntityPose.SLEEPING)
        && (!player.hasVehicle() || getPermitedVehicle(player))
        && !player.getAbilities().flying) {
        ticks.compute(player.getUuid(), (uuid, integer) -> integer == null ? 1 : integer + 1);
        if (ticks.get(player.getUuid()) % CobbleUtils.breedconfig.getTickstocheck() != 0)
          return;
        AtomicInteger distanceMoved = new AtomicInteger();

        if (lastPosition.get(player.getUuid()) == null) {
          lastPosition.put(player.getUuid(), new Vec3d(player.getX(), 0, player.getZ()));
          return;
        } else {
          Vec3d currentPosition = new Vec3d(player.getX(), 0, player.getZ());
          double total = currentPosition.distanceTo(lastPosition.get(player.getUuid()));
          if (total >= 25) {
            lastPosition.put(player.getUuid(), currentPosition);
            return;
          }
          distanceMoved.set((int) Math.min(20, total));
          lastPosition.put(player.getUuid(), currentPosition);
        }
        PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(PlayerUtils.castPlayer(player));
        if (playerPartyStore.size() == 0) return;

        boolean duplicate = playerPartyStore.toGappyList().stream().filter(Objects::nonNull).anyMatch(pokemon -> {
          String name = pokemon.getAbility().getName();
          return name.equalsIgnoreCase("flamebody") ||
            name.equalsIgnoreCase("magmaarmor");
        });

        playerPartyStore.forEach(pokemon -> {
          if (!pokemon.showdownId().equalsIgnoreCase("egg"))
            return;
          pokemon.setCurrentHealth(0);
          EggData eggData = EggData.from(pokemon);
          if (eggData == null)
            return;
          int distance = distanceMoved.get();
          if (duplicate) {
            distance *= 2;
          }
          eggData.steps(PlayerUtils.castPlayer(player), pokemon, distance);
        });

      }
    });

  }

  private static boolean getPermitedVehicle(PlayerEntity player) {
    return player.getVehicle() instanceof BoatEntity
      || player.getVehicle() instanceof HorseEntity
      || player.getVehicle() instanceof DonkeyEntity;
  }
}
