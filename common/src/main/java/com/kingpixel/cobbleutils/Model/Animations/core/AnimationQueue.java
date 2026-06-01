package com.kingpixel.cobbleutils.Model.Animations.core;

import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationQueue {
  private static final Map<UUID, Queue<AnimationTask>> PLAYER_QUEUES = new ConcurrentHashMap<>();

  public static void enqueue(ServerPlayerEntity player, Animations animationType,
                             List<ItemStack> allRewards, List<ItemStack> obtained) {
    if (player == null || animationType == Animations.NONE) return;

    if (animationType == Animations.RANDOM) {
      List<Animations> options = new ArrayList<>();
      options.add(Animations.CSGO);
      options.add(Animations.CIRCLE);
      options.add(Animations.ALLCIRCLE);
      options.add(Animations.HELIX);
      options.add(Animations.SHOWER);
      options.add(Animations.GIFT);
      options.add(Animations.DICE);
      options.add(Animations.PYRAMID);
      options.add(Animations.VORTEX);
      options.add(Animations.SLOT);
      options.add(Animations.SCRATCH);
      options.add(Animations.SUPERNOVA);
      options.add(Animations.CYCLONE);
      options.add(Animations.FIREWORKS);
      options.add(Animations.CONSTELLATION);
      options.add(Animations.METEOR);
      options.add(Animations.MAGIC_SPROUT);
      options.add(Animations.WISHING_WELL);
      options.add(Animations.BLACK_HOLE);
      options.add(Animations.ORBITAL);
      options.add(Animations.TOTEM_AURA);
      options.add(Animations.PLINKO);
      options.add(Animations.CARD_FLIP);
      options.add(Animations.WHEEL_OF_FORTUNE);
      options.add(Animations.SAFE_CRACKER);
      options.add(Animations.LOOTBOX);
      options.add(Animations.MINESWEEPER);
      options.add(Animations.MYSTERY_CHEST);
      options.add(Animations.CONVEYOR);
      options.add(Animations.POWER_HAMMER);
      animationType = options.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(options.size()));
    }

    Animation anim = AnimationRegistry.get(animationType);
    if (anim == null) return;

    UUID uuid = player.getUuid();
    AnimationTask task = new AnimationTask(player, anim, allRewards, obtained);

    PLAYER_QUEUES.compute(uuid, (k, queue) -> {
      if (queue == null) {
        queue = new LinkedList<>();
        queue.add(task);
        task.start(() -> dequeueNext(uuid));
      } else {
        queue.add(task);
      }
      return queue;
    });
  }

  public static void clearQueue(UUID uuid) {
    PLAYER_QUEUES.remove(uuid);
  }

  private static void dequeueNext(UUID uuid) {
    CobbleUtils.server.execute(() -> {
      Queue<AnimationTask> queue = PLAYER_QUEUES.get(uuid);
      if (queue == null) return;

      queue.poll();

      AnimationTask nextTask = queue.peek();
      if (nextTask != null) {
        if (nextTask.isValid()) {
          nextTask.start(() -> dequeueNext(uuid));
        } else {
          dequeueNext(uuid);
        }
      } else {
        PLAYER_QUEUES.remove(uuid);
      }
    });
  }

  private static class AnimationTask {
    private final ServerPlayerEntity player;
    private final Animation animation;
    private final List<ItemStack> allRewards;
    private final List<ItemStack> obtained;

    public AnimationTask(ServerPlayerEntity player, Animation animation, List<ItemStack> allRewards, List<ItemStack> obtained) {
      this.player = player;
      this.animation = animation;
      this.allRewards = allRewards;
      this.obtained = obtained;
    }

    public boolean isValid() {
      return player != null && !player.isRemoved();
    }

    public void start(Runnable onComplete) {
      Vec3d centerPosition = AnimationUtils.getPosition(player, null);

      final boolean[] completed = {false};
      Runnable safeOnComplete = () -> {
        synchronized (completed) {
          if (!completed[0]) {
            completed[0] = true;
            onComplete.run();
          }
        }
      };

      com.kingpixel.cobbleutils.CobbleUtils.ASYNC.schedule(() -> {
        synchronized (completed) {
          if (!completed[0]) {
            safeOnComplete.run();
          }
        }
      }, 12, java.util.concurrent.TimeUnit.SECONDS);

      animation.start(player, centerPosition, obtained, allRewards, safeOnComplete);
    }
  }
}
