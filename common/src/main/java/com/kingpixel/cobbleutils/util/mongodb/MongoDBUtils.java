package com.kingpixel.cobbleutils.util.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bson.BsonBinarySubType;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility methods for common MongoDB operations.
 *
 * <p>Focuses on cross-format compatibility: many Minecraft mods store UUIDs as plain
 * {@code String} while MongoDB natively uses {@code BsonBinary} (subtype 4). These utilities
 * handle both formats transparently so you don't have to worry about which format is stored.</p>
 *
 * <h2>UUID search strategy</h2>
 * <p>Use {@link #uuidFilter(String, UUID)} to generate a filter that matches a UUID field
 * regardless of how it was stored (String, Binary subtype 3, or Binary subtype 4).</p>
 *
 * <pre>{@code
 * // Find a player by UUID — works with String or Binary stored UUIDs:
 * Bson filter = MongoDBUtils.uuidFilter("uuid", player.getUuid());
 * Document doc = collection.find(filter).first();
 * }</pre>
 *
 * <h2>Convenience finders</h2>
 * <pre>{@code
 * Optional<Document> doc = MongoDBUtils.findByUuid(collection, "uuid", player.getUuid());
 * List<Document> docs    = MongoDBUtils.findAllByUuid(collection, "uuid", player.getUuid());
 * boolean exists         = MongoDBUtils.existsByUuid(collection, "uuid", player.getUuid());
 * long count             = MongoDBUtils.countByUuid(collection, "uuid", player.getUuid());
 * }</pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MongoDBUtils {

  // ─── UUID Filters ─────────────────────────────────────────────────────────

  /**
   * Creates a BSON filter that matches {@code field} against the given {@link UUID}
   * in <b>all common storage formats</b>:
   * <ul>
   *   <li>BSON Binary subtype 4 (UUID standard — native MongoDB).</li>
   *   <li>BSON Binary subtype 3 (UUID legacy — old Java driver).</li>
   *   <li>Plain {@code String} (hyphenated format used by many mods).</li>
   * </ul>
   *
   * <p>This is the recommended filter to use when searching by UUID in any collection
   * that may have mixed storage formats (e.g. after a migration or across multiple mods).</p>
   *
   * <pre>{@code
   * Bson filter = MongoDBUtils.uuidFilter("uuid", player.getUuid());
   * Document doc = collection.find(filter).first();
   * }</pre>
   *
   * @param field The document field name.
   * @param uuid  The UUID to search for.
   * @return A {@code $or} filter covering all UUID formats.
   */
  public static Bson uuidFilter(String field, UUID uuid) {
    return Filters.or(
        Filters.eq(field, uuidToBinaryStandard(uuid)),   // BinData(4, ...) — standard
        Filters.eq(field, uuidToBinaryLegacy(uuid)),     // BinData(3, ...) — legacy Java driver
        Filters.eq(field, uuid.toString())               // plain String
    );
  }

  /**
   * Creates a BSON filter that matches {@code field} only against the native
   * {@code BsonBinary subtype 4} format (most efficient — no {@code $or} needed).
   *
   * <p>Use this when you are certain all documents use the native UUID format.</p>
   *
   * @param field The document field name.
   * @param uuid  The UUID to search for.
   * @return An exact {@code $eq} filter for subtype 4.
   */
  public static Bson uuidFilterNative(String field, UUID uuid) {
    return Filters.eq(field, uuidToBinaryStandard(uuid));
  }

  /**
   * Creates a BSON filter that matches {@code field} only against a plain {@code String}.
   *
   * <p>Use this when you are certain all documents store UUIDs as strings.</p>
   *
   * @param field The document field name.
   * @param uuid  The UUID to search for.
   * @return An exact {@code $eq} filter for the string representation.
   */
  public static Bson uuidFilterString(String field, UUID uuid) {
    return Filters.eq(field, uuid.toString());
  }

  // ─── Convenience finders ──────────────────────────────────────────────────

  /**
   * Finds the first document where {@code field} matches the given UUID
   * in any of the supported formats.
   *
   * <pre>{@code
   * Optional<Document> doc = MongoDBUtils.findByUuid(collection, "uuid", player.getUuid());
   * doc.ifPresent(d -> System.out.println(d.getString("name")));
   * }</pre>
   *
   * @param collection The target collection.
   * @param field      The UUID field name.
   * @param uuid       The UUID to look for.
   * @return An {@link Optional} containing the document, or empty if not found.
   */
  public static Optional<Document> findByUuid(MongoCollection<Document> collection,
      String field, UUID uuid) {
    return Optional.ofNullable(collection.find(uuidFilter(field, uuid)).first());
  }

  /**
   * Finds all documents where {@code field} matches the given UUID
   * in any of the supported formats.
   *
   * @param collection The target collection.
   * @param field      The UUID field name.
   * @param uuid       The UUID to look for.
   * @return A list of matching documents (may be empty, never null).
   */
  public static List<Document> findAllByUuid(MongoCollection<Document> collection,
      String field, UUID uuid) {
    List<Document> result = new ArrayList<>();
    collection.find(uuidFilter(field, uuid)).into(result);
    return result;
  }

  /**
   * Returns {@code true} if at least one document matches the given UUID field.
   *
   * <pre>{@code
   * if (MongoDBUtils.existsByUuid(collection, "uuid", player.getUuid())) {
   *     // player exists in database
   * }
   * }</pre>
   *
   * @param collection The target collection.
   * @param field      The UUID field name.
   * @param uuid       The UUID to check.
   * @return {@code true} if a matching document exists.
   */
  public static boolean existsByUuid(MongoCollection<Document> collection,
      String field, UUID uuid) {
    return collection.countDocuments(uuidFilter(field, uuid)) > 0;
  }

  /**
   * Counts documents where {@code field} matches the given UUID in any format.
   *
   * @param collection The target collection.
   * @param field      The UUID field name.
   * @param uuid       The UUID to count.
   * @return Number of matching documents.
   */
  public static long countByUuid(MongoCollection<Document> collection,
      String field, UUID uuid) {
    return collection.countDocuments(uuidFilter(field, uuid));
  }

  // ─── UUID parsing ─────────────────────────────────────────────────────────

  /**
   * Reads a UUID from a {@link Document} field, supporting all storage formats:
   * <ul>
   *   <li>{@link UUID} — already deserialized (codec was applied).</li>
   *   <li>{@link Binary} subtype 4 — native UUID binary.</li>
   *   <li>{@link Binary} subtype 3 — legacy UUID binary.</li>
   *   <li>{@link String} — hyphenated UUID string.</li>
   * </ul>
   *
   * <pre>{@code
   * Optional<UUID> uuid = MongoDBUtils.getUuid(doc, "uuid");
   * uuid.ifPresent(u -> System.out.println("Player UUID: " + u));
   * }</pre>
   *
   * @param document The source document.
   * @param field    The field name.
   * @return An {@link Optional} with the parsed UUID, or empty if field is absent/null.
   */
  public static Optional<UUID> getUuid(Document document, String field) {
    if (document == null || !document.containsKey(field) || document.get(field) == null) {
      return Optional.empty();
    }

    Object raw = document.get(field);

    if (raw instanceof UUID uuid) {
      return Optional.of(uuid);
    }

    if (raw instanceof Binary binary) {
      byte subtype = binary.getType();
      if (subtype == BsonBinarySubType.UUID_STANDARD.getValue()) {
        return Optional.of(bytesToUuidBigEndian(binary.getData()));
      }
      if (subtype == BsonBinarySubType.UUID_LEGACY.getValue()) {
        return Optional.of(bytesToUuidLegacy(binary.getData()));
      }
      // Unknown subtype — attempt big-endian parse
      return Optional.of(bytesToUuidBigEndian(binary.getData()));
    }

    if (raw instanceof String str) {
      try {
        return Optional.of(UUID.fromString(str));
      } catch (IllegalArgumentException e) {
        return Optional.empty();
      }
    }

    return Optional.empty();
  }

  /**
   * Same as {@link #getUuid(Document, String)} but returns a raw {@link UUID} or {@code null}.
   *
   * @param document The source document.
   * @param field    The field name.
   * @return Parsed UUID or {@code null}.
   */
  public static UUID getUuidOrNull(Document document, String field) {
    return getUuid(document, field).orElse(null);
  }

  /**
   * Detects the BSON storage format of a UUID field in a document.
   *
   * <p>Useful for diagnostics or migration tools.</p>
   *
   * <pre>{@code
   * MongoDBUtils.UuidStorageFormat fmt = MongoDBUtils.detectUuidFormat(doc, "uuid");
   * System.out.println("Stored as: " + fmt);
   * }</pre>
   *
   * @param document The source document.
   * @param field    The field name.
   * @return The detected format, or {@link UuidStorageFormat#UNKNOWN} if unrecognized.
   */
  public static UuidStorageFormat detectUuidFormat(Document document, String field) {
    if (document == null || !document.containsKey(field)) return UuidStorageFormat.ABSENT;

    Object raw = document.get(field);
    if (raw == null) return UuidStorageFormat.NULL;
    if (raw instanceof UUID) return UuidStorageFormat.NATIVE_UUID;
    if (raw instanceof String) return UuidStorageFormat.STRING;
    if (raw instanceof Binary binary) {
      byte subtype = binary.getType();
      if (subtype == BsonBinarySubType.UUID_STANDARD.getValue()) return UuidStorageFormat.BINARY_SUBTYPE_4;
      if (subtype == BsonBinarySubType.UUID_LEGACY.getValue()) return UuidStorageFormat.BINARY_SUBTYPE_3;
      return UuidStorageFormat.BINARY_UNKNOWN;
    }
    return UuidStorageFormat.UNKNOWN;
  }

  /**
   * Describes the BSON storage format of a UUID field.
   */
  public enum UuidStorageFormat {
    /** Field does not exist in the document. */
    ABSENT,
    /** Field is present but null. */
    NULL,
    /** Already decoded as {@link UUID} (codec applied). */
    NATIVE_UUID,
    /** Stored as hyphenated String. */
    STRING,
    /** Stored as {@code BinData(4)} — UUID standard (preferred). */
    BINARY_SUBTYPE_4,
    /** Stored as {@code BinData(3)} — UUID legacy (old Java driver). */
    BINARY_SUBTYPE_3,
    /** Stored as binary with an unrecognized subtype. */
    BINARY_UNKNOWN,
    /** Stored as an unrecognized BSON type. */
    UNKNOWN
  }

  // ─── Filter helpers (internal) ────────────────────────────────────────────

  /**
   * Converts a {@link UUID} to a {@link Binary} with subtype 4 (UUID standard).
   */
  public static Binary uuidToBinaryStandard(UUID uuid) {
    return new Binary(BsonBinarySubType.UUID_STANDARD, uuidToBytesBigEndian(uuid));
  }

  /**
   * Converts a {@link UUID} to a {@link Binary} with subtype 3 (UUID legacy).
   *
   * <p>Used only in filters to match legacy-stored UUIDs — never for writing.</p>
   */
  public static Binary uuidToBinaryLegacy(UUID uuid) {
    return new Binary(BsonBinarySubType.UUID_LEGACY, uuidToBytesLegacy(uuid));
  }

  // ─── Byte conversion helpers ──────────────────────────────────────────────

  private static byte[] uuidToBytesBigEndian(UUID uuid) {
    ByteBuffer buf = ByteBuffer.wrap(new byte[16]);
    buf.putLong(uuid.getMostSignificantBits());
    buf.putLong(uuid.getLeastSignificantBits());
    return buf.array();
  }

  private static byte[] uuidToBytesLegacy(UUID uuid) {
    ByteBuffer buf = ByteBuffer.wrap(new byte[16]);
    buf.putLong(Long.reverseBytes(uuid.getMostSignificantBits()));
    buf.putLong(Long.reverseBytes(uuid.getLeastSignificantBits()));
    return buf.array();
  }

  private static UUID bytesToUuidBigEndian(byte[] bytes) {
    ByteBuffer buf = ByteBuffer.wrap(bytes);
    return new UUID(buf.getLong(), buf.getLong());
  }

  private static UUID bytesToUuidLegacy(byte[] bytes) {
    ByteBuffer buf = ByteBuffer.wrap(bytes);
    return new UUID(Long.reverseBytes(buf.getLong()), Long.reverseBytes(buf.getLong()));
  }

  // ─── General helpers ──────────────────────────────────────────────────────

  /**
   * Checks whether a BSON field type matches the expected {@link BsonType}.
   *
   * <p>Useful for safe field-type validation before reading a document.</p>
   *
   * <pre>{@code
   * if (MongoDBUtils.isFieldType(doc, "createdAt", BsonType.DATE_TIME)) {
   *     Instant ts = doc.getDate("createdAt").toInstant();
   * }
   * }</pre>
   *
   * @param document      The source document.
   * @param field         The field name.
   * @param expectedType  The expected BSON type.
   * @return {@code true} if the field exists and matches the expected type.
   */
  public static boolean isFieldType(Document document, String field, BsonType expectedType) {
    if (document == null || !document.containsKey(field)) return false;
    Object value = document.get(field);
    if (value == null) return expectedType == BsonType.NULL;
    return switch (expectedType) {
      case STRING -> value instanceof String;
      case BINARY -> value instanceof Binary;
      case BOOLEAN -> value instanceof Boolean;
      case INT32 -> value instanceof Integer;
      case INT64 -> value instanceof Long;
      case DOUBLE -> value instanceof Double;
      case DOCUMENT -> value instanceof Document;
      case ARRAY -> value instanceof List;
      case DATE_TIME -> value instanceof java.util.Date;
      case OBJECT_ID -> value instanceof org.bson.types.ObjectId;
      default -> false;
    };
  }
}

