package com.kingpixel.cobbleutils.Model.Animations.core;

import com.kingpixel.cobbleutils.Model.Animations.entity.*;
import com.kingpixel.cobbleutils.Model.Animations.gui.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class AnimationRegistry {
  private static final Map<Animations, Animation> REGISTRY = new HashMap<>();

  public static void register(@NotNull Animations key, Animation animation) {
    REGISTRY.put(key, animation);
  }

  public static Animation get(@NotNull Animations key) {
    return REGISTRY.get(key);
  }

  static {
    register(Animations.CSGO, new CSGOAnimation());
    register(Animations.CIRCLE, new CircleAnimation());
    register(Animations.ALLCIRCLE, new AllRewardsCircleAnimation());
    register(Animations.HELIX, new HelixAnimation());
    register(Animations.SHOWER, new ShowerAnimation());
    register(Animations.GIFT, new GiftAnimation());
    register(Animations.DICE, new DiceAnimation());
    register(Animations.PYRAMID, new PyramidAnimation());
    register(Animations.VORTEX, new VortexAnimation());
    register(Animations.SLOT, new SlotAnimation());
    register(Animations.SCRATCH, new ScratchAnimation());
    register(Animations.SUPERNOVA, new SupernovaAnimation());
    register(Animations.CYCLONE, new CycloneAnimation());
    register(Animations.FIREWORKS, new FireworksAnimation());
    register(Animations.CONSTELLATION, new ConstellationAnimation());
    register(Animations.METEOR, new MeteorAnimation());
    register(Animations.MAGIC_SPROUT, new MagicSproutAnimation());
    register(Animations.WISHING_WELL, new WishingWellAnimation());
    register(Animations.BLACK_HOLE, new BlackHoleAnimation());
    register(Animations.ORBITAL, new OrbitalAnimation());
    register(Animations.TOTEM_AURA, new TotemAuraAnimation());
    register(Animations.PLINKO, new PlinkoAnimation());
    register(Animations.CARD_FLIP, new CardFlipAnimation());
    register(Animations.WHEEL_OF_FORTUNE, new WheelOfFortuneAnimation());
    register(Animations.SAFE_CRACKER, new SafeCrackerAnimation());
    register(Animations.LOOTBOX, new LootboxAnimation());
    register(Animations.MINESWEEPER, new MinesweeperAnimation());
    register(Animations.MYSTERY_CHEST, new MysteryChestAnimation());
    register(Animations.CONVEYOR, new ConveyorAnimation());
    register(Animations.POWER_HAMMER, new PowerHammerAnimation());
    register(Animations.ENTITY_WHEEL_OF_FORTUNE, new EWheelOfFortuneAnimation());
    register(Animations.ENTITY_PLINKO, new EPlinkoAnimation());
  }
}
