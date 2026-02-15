package com.kingpixel.cobbleutils.Model.validators;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Carlos Varas Alonso - 20/01/2026 11:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StringValidator {
  private Set<String> ids = new HashSet<>(
    Set.of("*")
  );

  /**
   * Check if the given string is valid according to the validator rules.
   *
   * @param string The string to validate.
   * @return True if the string is valid, false otherwise.
   */
  public boolean isValid(@NonNull String string) {
    return ValidatorUtil.match(string, ids);
  }
}
