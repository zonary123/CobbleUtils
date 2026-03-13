package com.kingpixel.cobbleutils.model.validators;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Validator for generic strings.
 * <p>
 * Checks whether a string is valid based on a list of allowed IDs and optional blacklist.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StringValidator extends AbstractRegistryValidator<String> {

  /**
   * List of allowed strings. Supports wildcards and regex.
   */
  private Set<String> ids = new HashSet<>(Set.of(
    "*",
    "regex:.*",
    "example_string"
  ));

  @Override
  protected Set<String> getIdSet() {
    return ids;
  }

  @Override
  protected Set<String> getTagSet() {
    return Set.of(); // No tags for generic strings
  }

  @Override
  protected String getId(@NonNull String string) {
    return string;
  }

  @Override
  protected boolean isInTag(@NonNull String string) {
    return false; // No tags for strings
  }
}