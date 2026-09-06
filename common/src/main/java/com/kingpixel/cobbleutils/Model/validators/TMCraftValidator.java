package com.kingpixel.cobbleutils.Model.validators;

import com.cobblemon.mod.common.CobblemonItemComponents;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.tms.TechnicalMachine;
import com.cobblemon.mod.common.api.tms.TechnicalMachines;
import com.cobblemon.mod.common.item.components.TMMoveComponent;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.events.models.EventTMCraft;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import net.minecraft.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Validator for Technical Machine (TM) crafting events, moves, and TM items.
 * <p>
 * Validates TM crafting based on move names/IDs, elemental types, damage categories,
 * blacklist, and optional tags.
 *
 * @author Carlos Varas Alonso - 06/09/2026
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
public class TMCraftValidator extends AbstractRegistryValidator<EventTMCraft> {

  private static final String REGEX_ALL = "regex:.*";

  /**
   * List of allowed move or TM IDs (e.g., "*", "tackle", "fire_blast", "cobblemon:tm01", "regex:.*").
   */
  @Builder.Default
  private Set<String> moves = new HashSet<>(Set.of(
    "*",
    REGEX_ALL,
    "tackle"
  ));

  /**
   * List of allowed elemental types (e.g., "*", "fire", "water", "dragon").
   */
  @Builder.Default
  private Set<String> types = new HashSet<>(Set.of(
    "*",
    REGEX_ALL
  ));

  /**
   * List of allowed damage categories (e.g., "*", "physical", "special", "status").
   */
  @Builder.Default
  private Set<String> damageCategories = new HashSet<>(Set.of(
    "*",
    REGEX_ALL
  ));

  /**
   * Optional tags for TM / move validation.
   */
  @Builder.Default
  private Set<String> tmTags = new HashSet<>();

  @Override
  protected Set<String> getIdSet() {
    return moves;
  }

  @Override
  protected Set<String> getTagSet() {
    return tmTags;
  }

  @Override
  protected String getId(@NonNull EventTMCraft event) {
    if (event.getMove() != null) {
      return event.getMove().getName();
    }
    TechnicalMachine tm = event.getTm();
    if (tm != null && tm.getMoveName() != null) {
      return tm.getMoveName().getName();
    }
    return "";
  }

  @Override
  protected boolean isInTag(@NonNull EventTMCraft event) {
    return false;
  }

  @Override
  public boolean isValid(@NonNull EventTMCraft event) {
    try {
      MoveTemplate move = resolveMove(event);
      if (move == null) return false;

      String moveName = move.getName();
      Boolean cachedResult = getValidationCache().getIfPresent(moveName);
      if (cachedResult != null) return cachedResult;

      boolean valid = checkValidation(move, event.getTm());
      getValidationCache().put(moveName.intern(), valid);
      return valid;
    } catch (Exception e) {
      CobbleUtils.LOGGER_RAW.error("Error validating EventTMCraft: {}", event, e);
      return false;
    }
  }

  private MoveTemplate resolveMove(EventTMCraft event) {
    MoveTemplate move = event.getMove();
    if (move == null && event.getTm() != null) {
      move = event.getTm().getMoveName();
    }
    return move;
  }

  private boolean checkValidation(MoveTemplate move, TechnicalMachine tm) {
    String moveName = move.getName();
    String type = move.getElementalType().getName().toLowerCase();
    String category = move.getDamageCategory().getName().toLowerCase();

    if (isBlacklisted(moveName, type, category, tm)) {
      return false;
    }

    if (!matchesMoves(moveName, tm)) {
      return false;
    }

    if (!ValidatorUtil.match(type, types)) {
      return false;
    }

    return ValidatorUtil.match(category, damageCategories);
  }

  private boolean isBlacklisted(String moveName, String type, String category, TechnicalMachine tm) {
    Set<String> bl = getBlacklist();
    if (ValidatorUtil.match(moveName, bl)
      || ValidatorUtil.match(type, bl)
      || ValidatorUtil.match(category, bl)) {
      return true;
    }

    if (tm != null && tm.getId() != null) {
      return ValidatorUtil.match(tm.getId().toString(), bl);
    }
    return false;
  }

  private boolean matchesMoves(String moveName, TechnicalMachine tm) {
    if (ValidatorUtil.match(moveName, moves)) {
      return true;
    }
    if (tm != null && tm.getId() != null) {
      return ValidatorUtil.match(tm.getId().toString(), moves);
    }
    return false;
  }

  /**
   * Validates a MoveTemplate directly.
   *
   * @param move the move template to validate
   * @return true if the move matches the validator criteria
   */
  public boolean isValid(MoveTemplate move) {
    if (move == null) return false;
    TechnicalMachine tm = TechnicalMachines.INSTANCE.getMoveToTM().get(move);
    return isValid(EventTMCraft.builder()
      .move(move)
      .tm(tm)
      .build());
  }

  /**
   * Validates a TechnicalMachine directly.
   *
   * @param tm the technical machine to validate
   * @return true if the TM matches the validator criteria
   */
  public boolean isValid(TechnicalMachine tm) {
    if (tm == null) return false;
    return isValid(EventTMCraft.builder()
      .move(tm.getMoveName())
      .tm(tm)
      .build());
  }

  /**
   * Validates an ItemStack containing a TM.
   *
   * @param itemStack the item stack to validate
   * @return true if the TM item stack matches the validator criteria
   */
  public boolean isValid(ItemStack itemStack) {
    if (itemStack == null || itemStack.isEmpty()) return false;
    TMMoveComponent comp = itemStack.get(CobblemonItemComponents.TM_MOVE);
    if (comp == null) return false;
    MoveTemplate move = comp.getMove();
    if (move == null) {
      String moveName = comp.getMoveName();
      if (moveName != null && !moveName.isEmpty()) {
        move = Moves.getByName(moveName);
      }
    }
    if (move == null) return false;
    TechnicalMachine tm = TechnicalMachines.INSTANCE.getMoveToTM().get(move);
    return isValid(EventTMCraft.builder()
      .itemStack(itemStack)
      .move(move)
      .tm(tm)
      .build());
  }

  /**
   * Validates by move name.
   *
   * @param moveName the name of the move to validate
   * @return true if the move matches the validator criteria
   */
  public boolean isValid(String moveName) {
    if (moveName == null || moveName.isEmpty()) return false;
    MoveTemplate move = Moves.getByName(moveName);
    if (move == null) return false;
    return isValid(move);
  }
}
