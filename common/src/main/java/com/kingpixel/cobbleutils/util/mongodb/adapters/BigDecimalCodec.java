package com.kingpixel.cobbleutils.util.mongodb.adapters;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Decimal128;

import java.math.BigDecimal;

/**
 * BSON codec for {@link BigDecimal}.
 *
 * <p>Stores values as BSON {@code Decimal128}, preserving full decimal precision.
 * This avoids any IEEE-754 double-precision rounding that would occur with {@code Double}.</p>
 *
 * <h3>BSON representation</h3>
 * <pre>
 * BigDecimal("123.456789012345") ↔ NumberDecimal("123.456789012345")
 * </pre>
 *
 * <h3>Decode compatibility</h3>
 * <ul>
 *   <li>{@code Decimal128} → parsed directly.</li>
 *   <li>{@code String} → parsed via {@link BigDecimal#BigDecimal(String)} (legacy support).</li>
 *   <li>{@code Double} / {@code Int32} / {@code Int64} → converted (legacy support).</li>
 * </ul>
 */
public class BigDecimalCodec implements Codec<BigDecimal> {

  public static final BigDecimalCodec INSTANCE = new BigDecimalCodec();

  private BigDecimalCodec() {
  }

  @Override
  public void encode(BsonWriter writer, BigDecimal value, EncoderContext ctx) {
    writer.writeDecimal128(new Decimal128(value));
  }

  @Override
  public BigDecimal decode(BsonReader reader, DecoderContext ctx) {
    BsonType type = reader.getCurrentBsonType();
    return switch (type) {
      case DECIMAL128 -> reader.readDecimal128().bigDecimalValue();
      case STRING -> new BigDecimal(reader.readString());
      case DOUBLE -> BigDecimal.valueOf(reader.readDouble());
      case INT32 -> BigDecimal.valueOf(reader.readInt32());
      case INT64 -> BigDecimal.valueOf(reader.readInt64());
      default -> throw new IllegalStateException(
          "Cannot decode BSON type " + type + " into BigDecimal"
      );
    };
  }

  @Override
  public Class<BigDecimal> getEncoderClass() {
    return BigDecimal.class;
  }
}

