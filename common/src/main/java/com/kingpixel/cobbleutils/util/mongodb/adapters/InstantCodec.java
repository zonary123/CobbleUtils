package com.kingpixel.cobbleutils.util.mongodb.adapters;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.Instant;

/**
 * BSON codec for {@link Instant}.
 *
 * <h3>Encoding</h3>
 * <p>Stores as BSON {@code DateTime} (64-bit epoch milliseconds), which is
 * the native MongoDB date type and compatible with all MongoDB tooling.</p>
 *
 * <h3>Decoding — multi-format compatibility</h3>
 * <ul>
 *   <li><b>DateTime</b> — native, preferred.</li>
 *   <li><b>String</b> — ISO-8601 format ({@code "2025-04-23T10:00:00Z"}) for legacy support.</li>
 *   <li><b>Int64</b> — raw epoch millis stored as long.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>
 * Instant.now() ↔ ISODate("2025-04-23T10:00:00.000Z")
 * </pre>
 */
public class InstantCodec implements Codec<Instant> {

  public static final InstantCodec INSTANCE = new InstantCodec();

  private InstantCodec() {
  }

  @Override
  public void encode(BsonWriter writer, Instant value, EncoderContext ctx) {
    writer.writeDateTime(value.toEpochMilli());
  }

  @Override
  public Instant decode(BsonReader reader, DecoderContext ctx) {
    BsonType type = reader.getCurrentBsonType();
    return switch (type) {
      case DATE_TIME -> Instant.ofEpochMilli(reader.readDateTime());
      case INT64 -> Instant.ofEpochMilli(reader.readInt64());
      case STRING -> Instant.parse(reader.readString());
      default -> throw new IllegalStateException(
          "Cannot decode BSON type " + type + " into Instant"
      );
    };
  }

  @Override
  public Class<Instant> getEncoderClass() {
    return Instant.class;
  }
}

