package com.kingpixel.cobbleutils.Model.rewards;

import lombok.NonNull;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface RewardIconProvider {
  @Nullable ItemStack getIcon(@NonNull Reward reward, @NonNull String data);
}
