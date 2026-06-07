package com.kingpixel.cobbleutils.Model.Animations.entity;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.Animation;
import com.kingpixel.cobbleutils.Model.Animations.core.AnimationUtils;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomArmorStandEntity;
import com.kingpixel.cobbleutils.Model.Animations.core.CustomItemDisplayEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MagicSproutAnimation extends Animation {

  public static class TreeType {
    public final ItemStack sapling;
    public final net.minecraft.block.BlockState log;
    public final net.minecraft.block.BlockState leaves;
    public final net.minecraft.particle.SimpleParticleType leafParticle;

    public TreeType(ItemStack sapling, net.minecraft.block.BlockState log, net.minecraft.block.BlockState leaves, net.minecraft.particle.SimpleParticleType leafParticle) {
      this.sapling = sapling;
      this.log = log;
      this.leaves = leaves;
      this.leafParticle = leafParticle;
    }
  }

  private static final List<TreeType> TREE_TYPES = new ArrayList<>();

  static {
    TREE_TYPES.add(new TreeType(new ItemStack(Items.OAK_SAPLING), Blocks.OAK_LOG.getDefaultState(), Blocks.OAK_LEAVES.getDefaultState(), ParticleTypes.HAPPY_VILLAGER));
    TREE_TYPES.add(new TreeType(new ItemStack(Items.CHERRY_SAPLING), Blocks.CHERRY_LOG.getDefaultState(), Blocks.CHERRY_LEAVES.getDefaultState(), ParticleTypes.CHERRY_LEAVES));
    TREE_TYPES.add(new TreeType(new ItemStack(Items.BIRCH_SAPLING), Blocks.BIRCH_LOG.getDefaultState(), Blocks.BIRCH_LEAVES.getDefaultState(), ParticleTypes.HAPPY_VILLAGER));
    TREE_TYPES.add(new TreeType(new ItemStack(Items.SPRUCE_SAPLING), Blocks.SPRUCE_LOG.getDefaultState(), Blocks.SPRUCE_LEAVES.getDefaultState(), ParticleTypes.COMPOSTER));
    TREE_TYPES.add(new TreeType(new ItemStack(Items.JUNGLE_SAPLING), Blocks.JUNGLE_LOG.getDefaultState(), Blocks.JUNGLE_LEAVES.getDefaultState(), ParticleTypes.HAPPY_VILLAGER));
    TREE_TYPES.add(new TreeType(new ItemStack(Items.ACACIA_SAPLING), Blocks.ACACIA_LOG.getDefaultState(), Blocks.ACACIA_LEAVES.getDefaultState(), ParticleTypes.COMPOSTER));
    TREE_TYPES.add(new TreeType(new ItemStack(Items.DARK_OAK_SAPLING), Blocks.DARK_OAK_LOG.getDefaultState(), Blocks.DARK_OAK_LEAVES.getDefaultState(), ParticleTypes.HAPPY_VILLAGER));
  }

  @Override
  public void start(ServerPlayerEntity player, Vec3d position, List<ItemStack> obtained,
                    List<ItemStack> allRewards, Runnable onComplete) {
    int total = obtained.size();
    if (total == 0) {
      if (onComplete != null) onComplete.run();
      return;
    }

    Vec3d direction = player.getRotationVec(1.0f).normalize();
    Vec3d center = AnimationUtils.getPosition(player, position).add(direction.x * 3.0, 0.0, direction.z * 3.0);

    CobbleUtils.server.executeSync(() -> {
      TreeEntity tree = new TreeEntity(
        player.getServerWorld(),
        center.x, center.y, center.z,
        obtained, player, onComplete
      );
      player.getServerWorld().spawnEntity(tree);
    });
  }

  public static class FruitReward {
    public final CustomItemDisplayEntity display;
    public final Vec3d targetPos;
    public float scale = 0.0f;
    public int growTicks = 0;
    public final float itemYaw;

    public FruitReward(CustomItemDisplayEntity display, Vec3d targetPos, float itemYaw) {
      this.display = display;
      this.targetPos = targetPos;
      this.itemYaw = itemYaw;
    }
  }

  public static class LeafNode {
    public final DisplayEntity.BlockDisplayEntity display;
    public final float finalScale;
    public final Vector3f localOffset;

    public LeafNode(DisplayEntity.BlockDisplayEntity display, float finalScale, Vector3f localOffset) {
      this.display = display;
      this.finalScale = finalScale;
      this.localOffset = localOffset;
    }
  }

  public static class TreeEntity extends CustomArmorStandEntity {
    private final ServerPlayerEntity player;
    private final Vec3d basePos;
    private final List<ItemStack> rewards;
    private final Runnable onDestroy;
    private boolean completed = false;
    private boolean fruitsSpawned = false;

    private final TreeType chosenTree;
    private CustomItemDisplayEntity saplingDisplay;
    private DisplayEntity.BlockDisplayEntity logDisplay;
    private final List<LeafNode> leaves = new ArrayList<>();
    private final List<FruitReward> fruitEntities = new ArrayList<>();
    private float facingYaw;

    public TreeEntity(World world, double x, double y, double z, List<ItemStack> rewards, ServerPlayerEntity player, Runnable onDestroy) {
      super(world, x, y, z);
      this.player = player;
      this.basePos = new Vec3d(x, y, z);
      this.rewards = rewards;
      this.onDestroy = onDestroy;

      setNoGravity(true);
      setInvisible(true);
      setInvulnerable(true);

      ServerWorld sw = (ServerWorld) world;
      facingYaw = player.getYaw() + 180f;

      // Select random miniature tree type
      chosenTree = TREE_TYPES.get(ThreadLocalRandom.current().nextInt(TREE_TYPES.size()));

      // Spawn growing sapling
      saplingDisplay = AnimationUtils.spawnItemDisplay(
        sw, new Vec3d(x, y, z), chosenTree.sapling.copy(), new Vector3f(0.1f, 0.1f, 0.1f), facingYaw, 0
      );
    }

    private DisplayEntity.BlockDisplayEntity spawnLeafBlock(ServerWorld sw) {
      DisplayEntity.BlockDisplayEntity blockDisp = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, sw);
      blockDisp.setBlockState(chosenTree.leaves);
      blockDisp.refreshPositionAndAngles(basePos.x, basePos.y, basePos.z, 0f, 0f);
      sw.spawnEntity(blockDisp);
      return blockDisp;
    }

    @Override public void complete() {
      if (!completed) {
        completed = true;
        if (saplingDisplay != null) saplingDisplay.discard();
        if (logDisplay != null) logDisplay.discard();
        for (LeafNode node : leaves) {
          if (node.display != null) node.display.discard();
        }
        for (FruitReward ent : fruitEntities) {
          if (ent.display != null) {
            ent.display.discard();
          }
        }
        if (onDestroy != null) onDestroy.run();
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

      if (ticks < 30) {
        // Growth phase 1: Sapling sprouts and scales up
        double growProgress = ticks / 30.0;
        float currentScale = (float) (0.1 + growProgress * 1.4);

        Quaternionf rotation = new Quaternionf().rotationY((float) Math.toRadians(-facingYaw));
        AnimationUtils.updateDisplayTransformation(
          saplingDisplay, basePos, rotation, new Vector3f(currentScale, currentScale, currentScale), 2
        );

        if (ticks % 2 == 0) {
          sw.spawnParticles(
            ParticleTypes.HAPPY_VILLAGER,
            basePos.x, basePos.y + growProgress * 1.5, basePos.z,
            3, 0.15, 0.15, 0.15, 0.0
          );
        }
        if (ticks % 6 == 0) {
          player.playSoundToPlayer(SoundEvents.BLOCK_GRASS_HIT, player.getSoundCategory(), 0.5f, 1.2f);
        }
      } else if (ticks == 30) {
        // Phase transition: replace sapling with log trunk block
        if (saplingDisplay != null) {
          saplingDisplay.discard();
          saplingDisplay = null;
        }

        logDisplay = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, sw);
        logDisplay.setBlockState(chosenTree.log);
        logDisplay.refreshPositionAndAngles(basePos.x, basePos.y, basePos.z, 0f, 0f);

        // Start with a small trunk scale, mathematically rotating center and rotation
        float angleRad = (float) Math.toRadians(-facingYaw);
        Quaternionf rotation = new Quaternionf().rotationY(angleRad);
        Vector3f localCenter = rotation.transform(new Vector3f(0.25f, 0f, 0.25f));
        Vector3f translation = new Vector3f(-localCenter.x, 0f, -localCenter.z);

        logDisplay.setTransformation(new AffineTransformation(translation, rotation, new Vector3f(0.5f, 0.1f, 0.5f), null));
        sw.spawnEntity(logDisplay);

        player.playSoundToPlayer(SoundEvents.BLOCK_WOOD_PLACE, player.getSoundCategory(), 0.8f, 0.8f);
      } else if (ticks > 30 && ticks < 45) {
        // Growth phase 2: Log trunk scales upwards smoothly to 2.4 blocks!
        double trunkGrow = (ticks - 30) / 15.0;
        float heightScale = (float) (0.1 + trunkGrow * 2.3);

        float angleRad = (float) Math.toRadians(-facingYaw);
        Quaternionf rotation = new Quaternionf().rotationY(angleRad);
        Vector3f localCenter = rotation.transform(new Vector3f(0.25f, 0f, 0.25f));
        Vector3f translation = new Vector3f(-localCenter.x, 0f, -localCenter.z);

        logDisplay.setStartInterpolation(0);
        logDisplay.setInterpolationDuration(2);
        logDisplay.setTransformation(new AffineTransformation(translation, rotation, new Vector3f(0.5f, heightScale, 0.5f), null));

        if (ticks % 3 == 0) {
          sw.spawnParticles(
            ParticleTypes.COMPOSTER,
            basePos.x, basePos.y + heightScale, basePos.z,
            5, 0.3, 0.3, 0.3, 0.0
          );
        }
      } else if (ticks == 45) {
        // Phase 3: Sprout Leaf Canopy Clusters at their target offsets
        player.playSoundToPlayer(SoundEvents.BLOCK_CHERRY_SAPLING_PLACE, player.getSoundCategory(), 0.8f, 1.0f);
        player.playSoundToPlayer(SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, player.getSoundCategory(), 0.6f, 1.0f);

        double leafY = basePos.y + 2.4;
        sw.spawnParticles(
          chosenTree.leafParticle,
          basePos.x, leafY, basePos.z,
          20, 0.8, 0.5, 0.8, 0.02
        );

        // Define voxel miniature tree canopy layout (6 large canopy blocks)
        leaves.add(new LeafNode(spawnLeafBlock(sw), 1.2f, new Vector3f(0f, 3.0f, 0f)));
        leaves.add(new LeafNode(spawnLeafBlock(sw), 1.6f, new Vector3f(0f, 2.2f, 0f)));
        leaves.add(new LeafNode(spawnLeafBlock(sw), 1.2f, new Vector3f(-0.8f, 2.1f, 0f)));
        leaves.add(new LeafNode(spawnLeafBlock(sw), 1.2f, new Vector3f(0.8f, 2.1f, 0f)));
        leaves.add(new LeafNode(spawnLeafBlock(sw), 1.2f, new Vector3f(0f, 2.1f, 0.8f)));
        leaves.add(new LeafNode(spawnLeafBlock(sw), 1.2f, new Vector3f(0f, 2.1f, -0.8f)));
      } else if (ticks > 45 && ticks <= 55) {
        // Smoothly expand leaf canopy blocks from scale 0 to final scale
        double leafGrow = (ticks - 45) / 10.0;
        float angleRad = (float) Math.toRadians(-facingYaw);
        Quaternionf rotation = new Quaternionf().rotationY(angleRad);

        for (LeafNode node : leaves) {
          float currentS = (float) (leafGrow * node.finalScale);

          // Rotate target offset relative to trunk using JOML
          Vector3f rotatedOffset = rotation.transform(new Vector3f(node.localOffset));

          // Rotate local pivot center using JOML
          Vector3f localCenter = rotation.transform(new Vector3f(currentS / 2f, 0f, currentS / 2f));

          Vector3f translation = new Vector3f(
            rotatedOffset.x - localCenter.x,
            rotatedOffset.y,
            rotatedOffset.z - localCenter.z
          );

          node.display.setStartInterpolation(0);
          node.display.setInterpolationDuration(2);
          node.display.setTransformation(new AffineTransformation(translation, rotation, new Vector3f(currentS, currentS, currentS), null));
        }
      } else if (ticks > 55 && ticks <= 75 && !fruitsSpawned) {
        // Fruits (obtained rewards) begin growing and hanging under/in the leaf cluster
        int fruitTick = ticks - 56;
        int size = rewards.size();
        int fruitsPerTick = Math.max(1, size / 20);
        int startIdx = fruitTick * fruitsPerTick;
        int endIdx = Math.min(startIdx + fruitsPerTick, size);

        double[][] leafOffsets = {
          {-0.8, 2.1, 0.0}, // West
          {0.8, 2.1, 0.0},  // East
          {0.0, 2.1, 0.8},  // South
          {0.0, 2.1, -0.8}  // North
        };

        float angleRad = (float) Math.toRadians(-facingYaw);
        Quaternionf rotation = new Quaternionf().rotationY(angleRad);

        for (int i = startIdx; i < endIdx; i++) {
          ItemStack reward = rewards.get(i);
          if (reward == null) continue;

          int dirIndex = i % 4;
          double[] offset = leafOffsets[dirIndex];

          // Rotate leaf offset using JOML transform to guarantee identical rotation direction
          Vector3f rotatedLeafOffset = rotation.transform(new Vector3f((float) offset[0], (float) offset[1], (float) offset[2]));

          // Symmetrical and aligned placement
          int fruitIndexInDir = i / 4;
          int totalFruitsInDir = (size - 1 - dirIndex) / 4 + 1;

          double bx = rotatedLeafOffset.x;
          double bz = rotatedLeafOffset.z;
          double len = Math.sqrt(bx * bx + bz * bz);
          double ux = 0, uz = 0, px = 1, pz = 0;
          if (len > 0.001) {
            ux = bx / len;
            uz = bz / len;
            px = -uz;
            pz = ux;
          }

          double t = 0;
          if (totalFruitsInDir > 1) {
            t = ((double) fruitIndexInDir / (totalFruitsInDir - 1)) - 0.5;
          }

          double displacementPerpendicular = t * 0.5; // Spreads fruits up to 0.25 blocks left and right of the center
          double displacementBranch = (fruitIndexInDir % 2 == 0 ? 0.08 : -0.08); // Alternates slightly closer/further from the trunk to prevent Z-fighting or straight lines
          if (totalFruitsInDir == 1) {
            displacementBranch = 0;
          }

          double dx = px * displacementPerpendicular + ux * displacementBranch;
          double dz = pz * displacementPerpendicular + uz * displacementBranch;

          double fx = basePos.x + rotatedLeafOffset.x + dx;
          double fz = basePos.z + rotatedLeafOffset.z + dz;
          // Fruits hang closely from/slightly inside the leaf base for organic attachment
          double fy = basePos.y + rotatedLeafOffset.y + 0.1 - (fruitIndexInDir % 2) * 0.15;

          Vec3d targetPos = new Vec3d(fx, fy, fz);

          float itemYaw = player.getYaw() + 180f;
          CustomItemDisplayEntity display = AnimationUtils.spawnItemDisplay(
            sw, targetPos, reward.copy(), new Vector3f(0f, 0f, 0f), itemYaw, 0
          );

          FruitReward fruit = new FruitReward(display, targetPos, itemYaw);
          fruitEntities.add(fruit);

          sw.spawnParticles(
            chosenTree.leafParticle,
            fx, fy + 0.5, fz,
            2, 0.1, 0.1, 0.1, 0.0
          );
        }

        if (endIdx >= size) {
          fruitsSpawned = true;
          player.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, player.getSoundCategory(), 0.7f, 1.2f);
        }
      }

      // Grow and pulse all fruits centrally
      for (FruitReward fruit : fruitEntities) {
        if (fruit.growTicks < 15) {
          fruit.growTicks++;
          fruit.scale = (float) (fruit.growTicks / 15.0) * 1.2f;

          Quaternionf rotation = new Quaternionf().rotationY((float) Math.toRadians(-fruit.itemYaw));
          AnimationUtils.updateDisplayTransformation(
            fruit.display, fruit.targetPos, rotation, new Vector3f(fruit.scale, fruit.scale, fruit.scale), 2
          );
        } else {
          float pulse = 1.2f + (float) Math.sin(ticks * 0.15f) * 0.1f;
          Quaternionf rotation = new Quaternionf().rotationY((float) Math.toRadians(-fruit.itemYaw));
          AnimationUtils.updateDisplayTransformation(
            fruit.display, fruit.targetPos, rotation, new Vector3f(pulse, pulse, pulse), 2
          );
        }
      }

      if (ticks > 75) {
        if (ticks % 4 == 0) {
          sw.spawnParticles(
            chosenTree.leafParticle,
            basePos.x, basePos.y + 3.0, basePos.z,
            3, 1.0, 0.3, 1.0, 0.01
          );
        }

        if (ticks >= 135) {
          this.kill();
          complete();
        }
      }

      setTicks(ticks + 1);
    }
  }
}
