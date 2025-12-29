package com.kingpixel.cobbleutils.mixins;

import ca.landonjw.gooeylibs2.api.button.ButtonAction;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Consumer;

/**
 *
 * @author Carlos Varas Alonso - 21/12/2025 23:57
 */
@Mixin(value = GooeyButton.class)
public interface GooeyButtonMixin {

  @Accessor("onClick")
  Consumer<ButtonAction> getOnClick();
}
