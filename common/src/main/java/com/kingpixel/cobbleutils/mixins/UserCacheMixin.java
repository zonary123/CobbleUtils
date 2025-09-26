package com.kingpixel.cobbleutils.mixins;

import net.minecraft.util.UserCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

/**
 * @author Carlos Varas Alonso - 25/09/2025 22:01
 */
@Mixin(UserCache.class)
public interface UserCacheMixin {


  @Accessor("byName")
  Map<String, ?> byName();

  @Accessor("byUuid")
  Map<UUID, ?> byUuid();


}
