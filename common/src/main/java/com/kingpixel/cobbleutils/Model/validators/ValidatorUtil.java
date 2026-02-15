package com.kingpixel.cobbleutils.Model.validators;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ValidatorUtil {

  private static final Cache<String, Pattern> regexCache = Caffeine.newBuilder()
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .maximumSize(500)
    .build();

  private ValidatorUtil() {
  }

  public static boolean match(String value, Set<String> values) {
    if (value == null || values == null) return false;
    if (values.isEmpty() || values.contains("*")) return true;
    if (values.contains(value)) return true;


    for (String s : values) {
      if (!s.startsWith("regex:")) continue;

      String patternStr = s.substring(6);
      try {
        Pattern pattern = regexCache.get(patternStr, Pattern::compile);
        if (pattern == null) continue;
        if (pattern.matcher(value).find()) return true;

      } catch (Exception ignored) {
      }
    }

    return false;
  }

}
