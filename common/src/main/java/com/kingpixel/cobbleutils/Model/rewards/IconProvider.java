package com.kingpixel.cobbleutils.Model.rewards;

import lombok.NonNull;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface IconProvider {
  @Nullable ItemStack getIcon(@NonNull String data);
}
