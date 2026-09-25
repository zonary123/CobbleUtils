package com.kingpixel.cobbleutils.mixins.events;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.CobbleUtilsEvents;
import com.kingpixel.cobbleutils.events.models.EventItemStack;
import com.kingpixel.cobbleutils.events.models.EventShearEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PokemonEntity.class)
public abstract class PokemonShearMixin {

  @Unique
  private ServerPlayerEntity cobbleutils$lastShearer;
  @Unique
  private ItemStack cobbleutils$lastTool;

  @Inject(method = "interactMob", at = @At("HEAD"))
  private void cobbleutils$capturePlayer(
    PlayerEntity player,
    Hand hand,
    CallbackInfoReturnable<ActionResult> cir
  ) {
    try {
      if (player instanceof ServerPlayerEntity sp) {
        this.cobbleutils$lastShearer = sp;
        this.cobbleutils$lastTool = player.getStackInHand(hand).copy();
      }
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in PokemonShearMixin#cobbleutils$capturePlayer", e);
    }
  }

  @Inject(method = "sheared", at = @At("RETURN"))
  private void cobbleutils$onSheared(SoundCategory shearedSoundCategory, CallbackInfo ci) {
    try {
      if (CobbleUtilsEvents.SHEAR_ENTITY_EVENT.isEmpty() && CobbleUtilsEvents.SHEEP_SHEAR_EVENT.isEmpty()) return;
      if (this.cobbleutils$lastShearer != null) {
        PokemonEntity pokemonEntity = (PokemonEntity) (Object) this;
        List<ItemStack> drops = cobbleutils$resolvePokemonDrops(pokemonEntity);

        if (!CobbleUtilsEvents.SHEAR_ENTITY_EVENT.isEmpty()) {
          CobbleUtilsEvents.SHEAR_ENTITY_EVENT.emit(EventShearEntity.builder()
            .player(this.cobbleutils$lastShearer)
            .entity(pokemonEntity)
            .tool(this.cobbleutils$lastTool != null ? this.cobbleutils$lastTool : ItemStack.EMPTY)
            .drops(drops)
            .build());
        }

        if (!CobbleUtilsEvents.SHEEP_SHEAR_EVENT.isEmpty()) {
          ItemStack primary = drops.isEmpty() ? ItemStack.EMPTY : drops.get(0);
          CobbleUtilsEvents.SHEEP_SHEAR_EVENT.emit(EventItemStack.builder()
            .player(this.cobbleutils$lastShearer)
            .itemStack(primary)
            .itemStacks(drops)
            .build());
        }

        this.cobbleutils$lastShearer = null;
        this.cobbleutils$lastTool = null;
      }
    } catch (Throwable e) {
      CobbleUtils.LOGGER_RAW.error("Error in PokemonShearMixin#cobbleutils$onSheared", e);
    }
  }

  @Unique
  private List<ItemStack> cobbleutils$resolvePokemonDrops(PokemonEntity entity) {
    try {
      Pokemon pokemon = entity.getPokemon();
      if (pokemon == null) return List.of(new ItemStack(Items.WHITE_WOOL));

      String species = pokemon.getSpecies().getName().toLowerCase();
      if (species.equals("slowpoke")) {
        Item tail = Registries.ITEM.get(Identifier.of("cobblemon", "tasty_tail"));
        if (tail != Items.AIR) {
          return List.of(new ItemStack(tail));
        }
      }

      String color = "white";
      for (String aspect : pokemon.getAspects()) {
        String clean = aspect.replace("_wool", "").toLowerCase();
        if (clean.equals("black") || clean.equals("blue") || clean.equals("brown") || clean.equals("cyan")
          || clean.equals("gray") || clean.equals("green") || clean.equals("light_blue") || clean.equals("light_gray")
          || clean.equals("lime") || clean.equals("magenta") || clean.equals("orange") || clean.equals("pink")
          || clean.equals("purple") || clean.equals("red") || clean.equals("white") || clean.equals("yellow")) {
          color = clean;
          break;
        }
      }

      Item wool = Registries.ITEM.get(Identifier.of("minecraft", color + "_wool"));
      if (wool == Items.AIR) {
        wool = Items.WHITE_WOOL;
      }
      return List.of(new ItemStack(wool));
    } catch (Throwable e) {
      return List.of(new ItemStack(Items.WHITE_WOOL));
    }
  }
}
