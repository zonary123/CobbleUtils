package com.kingpixel.cobbleutils.api;

import com.kingpixel.cobbleutils.Model.conditions.Condition;
import com.kingpixel.cobbleutils.Model.conditions.util.ConditionUtils;
import com.kingpixel.cobbleutils.adapter.ConditionAdapter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;

/**
 * API for managing and checking custom conditions.
 *
 * @author Carlos Varas Alonso
 */
public class ConditionApi {

  private ConditionApi() {
  }

  /**
   * Register a custom condition type.
   *
   * @param id    The unique identifier of the condition type.
   * @param clazz The class representing the custom condition.
   */
  public static void register(String id, Class<? extends Condition> clazz) {
    ConditionUtils.register(id, clazz);
  }

  /**
   * Check if all conditions in the list are met by the player.
   * If a visualizable condition fails, it will render its visual feedback.
   *
   * @param conditions The list of conditions to check.
   * @param player     The player to check the conditions against.
   * @return True if all conditions are met, false otherwise.
   */
  public static boolean check(List<Condition> conditions, ServerPlayerEntity player) {
    return ConditionUtils.check(conditions, player);
  }

  /**
   * Check if a single condition is met by the player.
   *
   * @param condition The condition to check.
   * @param player    The player to check the condition against.
   * @return True if the condition is met, false otherwise.
   */
  public static boolean check(Condition condition, ServerPlayerEntity player) {
    if (condition == null) return true;
    return condition.check(player);
  }

  /**
   * Get all currently registered condition types and their corresponding classes.
   *
   * @return An unmodifiable map of registered condition types.
   */
  public static Map<String, Class<? extends Condition>> getRegisteredTypes() {
    return ConditionAdapter.getRegisteredTypes();
  }

  /**
   * Get a list of default instances for all registered condition types.
   *
   * @return A list of default Condition instances.
   */
  public static List<Condition> getDefaultConditions() {
    List<Condition> conditions = new java.util.ArrayList<>();
    for (Class<? extends Condition> clazz : ConditionAdapter.getRegisteredTypes().values()) {
      try {
        conditions.add(clazz.getDeclaredConstructor().newInstance());
      } catch (Exception e) {
        // Skip conditions that cannot be instantiated with a default constructor
      }
    }
    return conditions;
  }
}
