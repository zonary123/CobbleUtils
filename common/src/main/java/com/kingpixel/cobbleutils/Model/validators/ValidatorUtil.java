package com.kingpixel.cobbleutils.Model.validators;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for validating strings, IDs, etc., with support for
 * exact matches, wildcard "*" and regex patterns.
 */
public class ValidatorUtil {

  // Cache compiled regex patterns to avoid recompiling
  private static final Cache<String, Pattern> regexCache = Caffeine.newBuilder()
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .maximumSize(500)
    .build();

  private ValidatorUtil() {
  }

  /**
   * Checks if a value matches a set of patterns.
   * Patterns can be exact strings, "*" wildcard, or "regex:<pattern>".
   * <p>
   * This version separates exact matches from regex for maximum performance.
   *
   * @param value  the string to validate
   * @param values the set of allowed patterns
   * @return true if the value matches any pattern
   */
  public static boolean match(String value, Set<String> values) {
    if (value == null || values == null || values.isEmpty()) return false;

    // Handle wildcard or exact match quickly
    if (values.contains("*") || values.contains(value)) return true;

    // Preprocess regex patterns (starts with "regex:") only once
    Set<String> regexPatterns = values.stream()
      .filter(s -> s.startsWith("regex:"))
      .collect(Collectors.toSet());

    for (String regex : regexPatterns) {
      try {
        Pattern pattern = regexCache.get(regex, key -> Pattern.compile(key.substring(6)));
        if (pattern == null) continue;
        if (pattern.matcher(value).matches()) return true;
      } catch (Exception e) {
        System.err.println("Invalid regex pattern: " + regex + " -> " + e.getMessage());
      }
    }
    return false;
  }
}