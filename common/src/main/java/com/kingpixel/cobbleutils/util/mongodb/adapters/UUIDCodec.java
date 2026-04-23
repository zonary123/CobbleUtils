package com.kingpixel.cobbleutils.util.mongodb.adapters;

import org.bson.BsonBinary;
import org.bson.BsonBinarySubType;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Smart BSON codec for {@link UUID}.
 *
 * <h3>Encoding</h3>
 * <p>Always writes as BSON {@code Binary} with subtype {@code 4} (UUID standard),
 * which is the native MongoDB UUID representation and the most efficient storage.</p>
 *
 * <h3>Decoding — multi-format compatibility</h3>
 * <p>Handles all common formats found in existing databases:</p>
 * <ul>
 *   <li><b>Binary subtype 4</b> (UUID standard) — native, most efficient.</li>
 *   <li><b>Binary subtype 3</b> (UUID legacy) — older MongoDB Java driver format.</li>
 *   <li><b>String</b> — {@code "550e8400-e29b-41d4-a716-446655440000"} — common in mods.</li>
 * </ul>
 *
 * <p>This means you can migrate from string-stored UUIDs to native Binary UUIDs
 * transparently: on first read the string is decoded as UUID, on next write it
 * is stored as native Binary.</p>
 *
 * <h3>Example — Document insertion</h3>
 * <pre>{@code
 * // Instead of:
 * doc.put("uuid", player.getUuid().toString()); // stored as String
 *
 * // Just put the UUID directly — the codec handles encoding:
 * doc.put("uuid", player.getUuid()); // stored as BinData(4, ...)
 * }</pre>
 */
public class UUIDCodec implements Codec<UUID> {

  public static final UUIDCodec INSTANCE = new UUIDCodec();

  private UUIDCodec() {
  }

  @Override
  public void encode(BsonWriter writer, UUID value, EncoderContext ctx) {
    byte[] bytes = uuidToBytes(value);
    writer.writeBinaryData(new BsonBinary(BsonBinarySubType.UUID_STANDARD, bytes));
  }

  @Override
  public UUID decode(BsonReader reader, DecoderContext ctx) {
    BsonType type = reader.getCurrentBsonType();

    return switch (type) {
      case BINARY -> {
        BsonBinary binary = reader.readBinaryData();
        byte subtype = binary.getType();
        if (subtype == BsonBinarySubType.UUID_STANDARD.getValue()) {
          yield bytesToUuid(binary.getData());
        } else if (subtype == BsonBinarySubType.UUID_LEGACY.getValue()) {
          yield bytesToUuidLegacy(binary.getData());
        }
        yield bytesToUuid(binary.getData());
      }
      case STRING -> UUID.fromString(reader.readString());
      default -> throw new IllegalStateException(
          "Cannot decode BSON type " + type + " into UUID"
      );
    };
  }

  @Override
  public Class<UUID> getEncoderClass() {
    return UUID.class;
  }

  // ─── Byte helpers ─────────────────────────────────────────────────────────

  /**
   * Converts a {@link UUID} to 16 bytes in big-endian order (UUID standard subtype 4).
   */
  private static byte[] uuidToBytes(UUID uuid) {
    ByteBuffer buf = ByteBuffer.wrap(new byte[16]);
    buf.putLong(uuid.getMostSignificantBits());
    buf.putLong(uuid.getLeastSignificantBits());
    return buf.array();
  }

  /**
   * Converts 16 big-endian bytes (subtype 4) back to a {@link UUID}.
   */
  private static UUID bytesToUuid(byte[] bytes) {
    ByteBuffer buf = ByteBuffer.wrap(bytes);
    long msb = buf.getLong();
    long lsb = buf.getLong();
    return new UUID(msb, lsb);
  }

  /**
   * Converts 16 bytes in the legacy Java driver byte order (subtype 3) back to a {@link UUID}.
   *
   * <p>The legacy format stores the bytes in a mixed little-endian / big-endian layout
   * used by the old MongoDB Java driver. This codec converts transparently so existing
   * legacy data can be read without modification.</p>
   */
  private static UUID bytesToUuidLegacy(byte[] bytes) {
    // Legacy subtype 3: first 8 bytes reversed for MSB, last 8 bytes reversed for LSB
    ByteBuffer buf = ByteBuffer.wrap(bytes);
    long msb = Long.reverseBytes(buf.getLong());
    long lsb = Long.reverseBytes(buf.getLong());
    return new UUID(msb, lsb);
  }
}



