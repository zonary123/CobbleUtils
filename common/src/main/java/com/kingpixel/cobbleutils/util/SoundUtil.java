package com.kingpixel.cobbleutils.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * @author Carlos Varas Alonso - 19/08/2024 20:13
 */
public class SoundUtil {

  public static SoundEvent getSound(String sound) {
    if (sound == null || sound.isEmpty()) return null;
    try {
      String namespace = sound.split(":")[0];
      String path = sound.split(":")[1];
      return SoundEvent.of(Identifier.of(namespace, path));
    } catch (Exception e) {
      return null;
    }
  }

  public static void playSound(SoundEvent sound, ServerPlayerEntity player) {
    if (sound == null) return;
    player.playSoundToPlayer(sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
  }

  public static void playSound(String soundopen, ServerPlayerEntity player) {
    if (soundopen == null || soundopen.isEmpty()) return;
    var sound = getSound(soundopen);
    if (sound == null) return;
    player.playSoundToPlayer(sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
  }
}
