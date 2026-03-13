package com.kingpixel.cobbleutils.model.validators;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.*;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Generic validator for any registered object (Item, Block, Entity, etc.).
 * <p>
 * Subclasses must provide:
 * <ul>
 *   <li>{@link #getId(T)} – returns the ID of the object</li>
 *   <li>{@link #getIdSet()} – the set of IDs to validate (e.g., itemIds, blockIds, entityIds)</li>
 *   <li>{@link #getTagSet()} – the set of tags to validate (e.g., itemTags, blockTags, entityTags)</li>
 *   <li>{@link #isInTag(T)} – returns true if the object belongs to any of the tags</li>
 * </ul>
 * <p>
 * This abstract class handles the common logic for blacklist checking, tag validation,
 * ID matching, and caches validation results for repeated objects.
 *
 * @param <T> the type of object to validate (ItemStack, Block, EntityType, etc.)
 */
@EqualsAndHashCode
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
public abstract class AbstractRegistryValidator<T> {

  /**
   * Common blacklist for IDs. Objects matching any entry here are considered invalid.
   */
  protected Set<String> blacklist;

  /**
   * Transient cache to store validation results per object ID.
   * Safe for serialization, initialized lazily after deserialization.
   */
  private transient Cache<String, Boolean> validationCache;

  /**
   * Lazy initialization of the cache.
   */
  private Cache<String, Boolean> getValidationCache() {
    if (validationCache == null) {
      validationCache = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build();
    }
    return validationCache;
  }

  /**
   * Returns the set of IDs that should be validated.
   * Examples: itemIds, blockIds, entityIds
   *
   * @return a Set of ID strings
   */
  protected abstract Set<String> getIdSet();

  /**
   * Returns the set of tags that should be validated.
   * Examples: itemTags, blockTags, entityTags
   *
   * @return a Set of tag strings
   */
  protected abstract Set<String> getTagSet();

  /**
   * Returns the ID of the given object.
   *
   * @param object the object to retrieve the ID from
   * @return the object's ID string
   */
  protected abstract String getId(@NonNull T object);

  /**
   * Checks if the object belongs to any of the defined tags.
   *
   * @param object the object to check
   * @return true if the object is in any tag, false otherwise
   */
  protected abstract boolean isInTag(@NonNull T object);

  /**
   * Checks if the object is valid according to the validator's criteria.
   * <p>
   * Uses cache to avoid repeated expensive validations.
   *
   * @param object the object to validate
   * @return true if the object is valid, false otherwise
   */
  public boolean isValid(@NonNull T object) {
    try {
      String id = this.getId(object);

      var validationCache = getValidationCache();
      Boolean cachedResult = validationCache.getIfPresent(id);
      if (cachedResult != null) return cachedResult;

      boolean result;
      if (this.isInTag(object)) {
        result = true;
      } else if (ValidatorUtil.match(id, this.getBlacklist())) {
        result = false;
      } else {
        result = ValidatorUtil.match(id, this.getIdSet());
      }

      validationCache.put(id, result);

      return result;

    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
}