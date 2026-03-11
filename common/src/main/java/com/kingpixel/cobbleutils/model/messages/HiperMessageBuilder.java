package com.kingpixel.cobbleutils.model.messages;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.sound.SoundEvent;

/**
 * @author Carlos Varas Alonso - 25/09/2025 23:18
 */
public class HiperMessageBuilder {
  private MessageType type;
  // Message content
  private String rawMessage;
  // Title and subtitle for title messages
  private String title;
  private String subtitle;
  // Sound settings
  private SoundEvent sound;
  private double pitch = 1.0;
  private double volume = 1.0;
  // Effect settings can be added here in the future
  private StatusEffect potionEffect;
  private int duration = 60;
  private int amplifier = 1;
  // Particle settings can be added here in the future
  private ParticleEffect particleEffect;
  private int count = 10;

  public static HiperMessageBuilder builder() {
    return new HiperMessageBuilder();
  }

  public HiperMessageBuilder setRawMessage(String rawMessage) {
    this.rawMessage = rawMessage;
    return this;
  }

  public HiperMessageBuilder setType(MessageType type) {
    this.type = type;
    return this;
  }

  public HiperMessageBuilder setTitle(String title) {
    this.title = title;
    return this;
  }

  public HiperMessageBuilder setSubtitle(String subtitle) {
    this.subtitle = subtitle;
    return this;
  }

  //==== Sound settings ====

  public HiperMessageBuilder setSound(SoundEvent sound) {
    this.sound = sound;
    return this;
  }

  public HiperMessageBuilder setPitch(double pitch) {
    this.pitch = pitch;
    return this;
  }

  public HiperMessageBuilder setVolume(double volume) {
    this.volume = volume;
    return this;
  }

  //==== Effect settings ====
  public HiperMessageBuilder setPotionEffect(StatusEffect potionEffect) {
    this.potionEffect = potionEffect;
    return this;
  }

  public HiperMessageBuilder setDuration(int duration) {
    this.duration = duration;
    return this;
  }

  public HiperMessageBuilder setAmplifier(int amplifier) {
    this.amplifier = amplifier;
    return this;
  }

  //==== Particle settings ====
  public HiperMessageBuilder setParticleEffect(ParticleEffect particleEffect) {
    this.particleEffect = particleEffect;
    return this;
  }

  public HiperMessageBuilder setCount(int count) {
    this.count = count;
    return this;
  }


  public HiperMessage build() {
    HiperMessage hiperMessage = new HiperMessage("", null);
    StringBuilder builder = new StringBuilder();
    switch (type) {
      case CHAT -> builder.append("c:").append(rawMessage);
      case CHAT_BROADCAST -> builder.append("cb:").append(rawMessage);
      case ACTIONBAR -> builder.append("ab:").append(rawMessage);
      case ACTIONBAR_BROADCAST -> builder.append("abb:").append(rawMessage);
      case TITLE_SUBTITLE -> {
        builder.append("ts:");
        if (title == null) {
          throw new NullPointerException("title is null");
        }
        builder.append("title:").append(title);
        if (subtitle == null) {
          throw new NullPointerException("subtitle is null");
        }
        builder.append("subtitle:").append(subtitle);
      }
      case TITLE_SUBTITLE_BROADCAST -> {
        builder.append("tsb:");
        if (title == null) {
          throw new NullPointerException("title is null");
        }
        builder.append("title:").append(title);
        if (subtitle == null) {
          throw new NullPointerException("subtitle is null");
        }
        builder.append("subtitle:").append(subtitle);
      }
      case BROADCAST -> builder.append("b:").append(rawMessage);
      case BOSSBAR_BROADCAST -> builder.append("bsb:").append(rawMessage);
      case BOSSBAR -> builder.append("bs:").append(rawMessage);
    }

    if (sound != null) {
      builder.append("sound:").append(sound.getId().toString());
      builder.append("volume:").append(volume);
      builder.append("pitch:").append(pitch);
    }

    if (potionEffect != null) {
      builder.append("effect:").append(potionEffect.getName().getString());
      builder.append("duration:").append(duration);
      builder.append("amplifier:").append(amplifier);
    }

    if (particleEffect != null) {
      builder.append("particle:").append("minecraft:firework");
      builder.append("count:").append(count);
    }

    hiperMessage.setRawMessage(builder.toString());
    return hiperMessage;
  }
}
