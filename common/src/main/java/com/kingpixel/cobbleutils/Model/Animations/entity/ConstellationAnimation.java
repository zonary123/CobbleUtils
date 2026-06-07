package com.kingpixel.cobbleutils.Model.Animations.entity;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.Animation;
import com.kingpixel.cobbleutils.Model.Animations.core.AnimationUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomArmorStandEntity;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomItemDisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ConstellationAnimation extends Animation {

  private static final List<ConstellationData> CONSTELLATIONS = new ArrayList<>();

  static {
    CONSTELLATIONS.add(new ConstellationData("Orion",
      new double[][]{
        {0.0, 2.5}, {-0.4, 1.8}, {0.4, 1.8}, {-0.8, 1.0}, {0.0, 1.0}, {0.8, 1.0}, {-0.5, 0.0}, {0.5, 0.0}
      },
      new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 5}, {3, 4}, {4, 5}, {3, 6}, {5, 7}}
    ));

    CONSTELLATIONS.add(new ConstellationData("Cassiopeia",
      new double[][]{
        {-2.0, 0.0}, {-1.0, 1.2}, {0.0, 0.3}, {1.0, 1.2}, {2.0, 0.0}
      },
      new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}}
    ));

    CONSTELLATIONS.add(new ConstellationData("Ursa Major",
      new double[][]{
        {-1.5, 0.0}, {-0.8, 0.5}, {0.0, 0.6}, {0.8, 0.3}, {1.2, 0.8}, {1.8, 1.2}, {0.6, 1.3}
      },
      new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 3}}
    ));

    CONSTELLATIONS.add(new ConstellationData("Crux",
      new double[][]{
        {0.0, 1.8}, {-1.0, 0.9}, {1.0, 0.9}, {0.0, 0.0}, {0.5, 1.5}
      },
      new int[][]{{0, 3}, {1, 2}, {0, 4}}
    ));

    CONSTELLATIONS.add(new ConstellationData("Lyra",
      new double[][]{
        {0.0, 2.0}, {-0.6, 1.2}, {0.6, 1.2}, {-0.8, 0.3}, {0.8, 0.3}, {-0.3, 0.0}, {0.3, 0.0}
      },
      new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 4}, {3, 5}, {4, 6}, {5, 6}}
    ));

    CONSTELLATIONS.add(new ConstellationData("Leo",
      new double[][]{
        {-1.5, 1.5}, {-0.8, 2.0}, {0.0, 1.8}, {0.6, 2.0}, {1.2, 1.5}, {0.8, 0.8}, {0.0, 0.0}
      },
      new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {5, 2}}
    ));

    CONSTELLATIONS.add(new ConstellationData("Scorpius",
      new double[][]{
        {-2.0, 1.0}, {-1.2, 0.5}, {-0.5, 0.3}, {0.0, 0.0}, {0.5, 0.3}, {1.0, 0.8}, {1.4, 1.4}, {1.8, 1.8}, {2.0, 1.5}
      },
      new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 8}}
    ));

    CONSTELLATIONS.add(new ConstellationData("Gemini",
      new double[][]{
        {-0.8, 2.2}, {0.8, 2.2}, {-0.6, 1.5}, {0.6, 1.5}, {-0.4, 0.8}, {0.4, 0.8}, {-0.3, 0.0}, {0.3, 0.0}
      },
      new int[][]{{0, 2}, {1, 3}, {2, 4}, {3, 5}, {4, 6}, {5, 7}, {2, 3}}
    ));
  }

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    if (obtained.isEmpty()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    ConstellationData constellation = CONSTELLATIONS.get(ThreadLocalRandom.current().nextInt(CONSTELLATIONS.size()));

    Vec3d direction = player.getRotationVec(1.0f).normalize();
    Vec3d right = new Vec3d(-direction.z, 0, direction.x).normalize();
    Vec3d center = AnimationUtils.getPosition(player, position).add(direction.x * 3.0, 1.5, direction.z * 3.0);

    CobbleUtils.server.executeSync(() -> {
      ConstellationControllerEntity controller = new ConstellationControllerEntity(
        player.getServerWorld(), center.x, center.y, center.z,
        obtained, player, constellation, right, onComplete
      );
      player.getServerWorld().spawnEntity(controller);
    });
  }

  public static class ConstellationControllerEntity extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final ConstellationData constellation;
    private final Runnable onDestroy;
    private boolean completed = false;

    private final List<CustomItemDisplayEntity> stars = new ArrayList<>();
    private final List<Vec3d> starPositions = new ArrayList<>();

    public ConstellationControllerEntity(World world, double x, double y, double z,
                                         List<ItemStack> obtained, ServerPlayerEntity player,
                                         ConstellationData constellation, Vec3d right, Runnable onDestroy) {
      super(world, x, y, z);
      this.player = player;
      this.constellation = constellation;
      this.onDestroy = onDestroy;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);

      ServerWorld sw = (ServerWorld) world;
      int nodeCount = constellation.nodes.length;
      float itemYaw = player.getYaw() + 180f;

      player.playSoundToPlayer(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, player.getSoundCategory(), 0.8f, 1.2f);

      for (int j = 0; j < nodeCount; j++) {
        double ox = constellation.nodes[j][0] * 1.4;
        double oy = constellation.nodes[j][1] * 1.4;

        Vec3d starPos = new Vec3d(x, y, z).add(right.x * ox, oy, right.z * ox);
        starPositions.add(starPos);

        ItemStack stack = (j < obtained.size() && obtained.get(j) != null) ? obtained.get(j) : new ItemStack(Items.NETHER_STAR);

        CustomItemDisplayEntity star = AnimationUtils.spawnItemDisplay(
          sw, starPos, stack.copy(), new Vector3f(1.0f, 1.0f, 1.0f), itemYaw, 0
        );
        stars.add(star);
      }
    }

    @Override public void complete() {
      if (!completed) {
        completed = true;
        for (CustomItemDisplayEntity star : stars) {
          if (star != null) {
            star.discard();
          }
        }
        if (onDestroy != null) onDestroy.run();
      }
    }

    private void drawParticleLine(ServerWorld world, Vec3d start, Vec3d end) {
      Vec3d delta = end.subtract(start);
      double distance = delta.length();
      int points = (int) (distance * 4);
      if (points <= 0) return;
      for (int i = 0; i <= points; i++) {
        double t = (double) i / points;
        Vec3d point = start.add(delta.multiply(t));
        world.spawnParticles(
          ParticleTypes.END_ROD,
          point.x, point.y, point.z,
          1, 0.0, 0.0, 0.0, 0.0
        );
      }
    }

    @Override
    public void tick() {
      super.tick();

      if (this.player == null || this.player.isRemoved() || !this.isAlive()) {
        this.kill();
        complete();
        return;
      }

      int ticks = getTicks();
      ServerWorld sw = (ServerWorld) this.getWorld();

      if (ticks < 80) {
        // Draw the glowing constellation lines synchronously on the tick!
        for (int[] edge : constellation.edges) {
          if (edge[0] < starPositions.size() && edge[1] < starPositions.size()) {
            Vec3d a = starPositions.get(edge[0]);
            Vec3d b = starPositions.get(edge[1]);
            drawParticleLine(sw, a, b);
          }
        }

        // Make the stars twinkle (scale up and down gently)
        float twinkle = 1.0f + (float) Math.sin(ticks * 0.4f) * 0.15f;
        float itemYaw = player.getYaw() + 180f + ticks * 2.0f; // Spin stars slowly
        Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-itemYaw), 0, 0);

        for (int j = 0; j < stars.size(); j++) {
          CustomItemDisplayEntity star = stars.get(j);
          if (star != null) {
            AnimationUtils.updateDisplayTransformation(
              star, starPositions.get(j), rotation, new Vector3f(twinkle, twinkle, twinkle), 2
            );
          }
        }
      } else {
        this.kill();
        complete();
      }

      setTicks(ticks + 1);
    }
  }

  public static class ConstellationData {
    public final String name;
    public final double[][] nodes;
    public final int[][] edges;

    public ConstellationData(String name, double[][] nodes, int[][] edges) {
      this.name = name;
      this.nodes = nodes;
      this.edges = edges;
    }
  }
}
