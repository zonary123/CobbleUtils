package com.kingpixel.cobbleutils.util.mongodb.adapters;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BSON codecs for Java {@code java.util.concurrent.atomic} types.
 *
 * <p>Atomic wrappers are transparent to MongoDB — they are stored as their
 * primitive BSON equivalent and reconstructed on decode.</p>
 *
 * <h3>Type mapping</h3>
 * <table border="1">
 *   <tr><th>Java type</th><th>BSON type</th></tr>
 *   <tr><td>{@link AtomicInteger}</td><td>Int32</td></tr>
 *   <tr><td>{@link AtomicLong}</td><td>Int64</td></tr>
 *   <tr><td>{@link AtomicBoolean}</td><td>Boolean</td></tr>
 *   <tr><td>{@link AtomicReference}{@code <T>}</td><td>Same as {@code T}</td></tr>
 * </table>
 *
 * <h3>Registration</h3>
 * <pre>{@code
 * MongoCodecProvider.INSTANCE.register(AtomicInteger.class, AtomicCodecs.INTEGER);
 * MongoCodecProvider.INSTANCE.register(AtomicLong.class,    AtomicCodecs.LONG);
 * MongoCodecProvider.INSTANCE.register(AtomicBoolean.class, AtomicCodecs.BOOLEAN);
 * MongoCodecProvider.INSTANCE.registerFactory(AtomicReference.class, AtomicCodecs::reference);
 * }</pre>
 */
public final class AtomicCodecs {

  private AtomicCodecs() {
  }

  // ─── AtomicInteger ────────────────────────────────────────────────────────

  /**
   * Codec for {@link AtomicInteger} → BSON {@code Int32}.
   *
   * <p>Decode compatibility: accepts {@code Int32}, {@code Int64}, {@code Double}.</p>
   *
   * <pre>
   * new AtomicInteger(42) ↔ 42
   * </pre>
   */
  public static final Codec<AtomicInteger> INTEGER = new Codec<>() {

    @Override
    public void encode(BsonWriter writer, AtomicInteger value, EncoderContext ctx) {
      writer.writeInt32(value.get());
    }

    @Override
    public AtomicInteger decode(BsonReader reader, DecoderContext ctx) {
      return new AtomicInteger(readAsInt(reader));
    }

    @Override
    public Class<AtomicInteger> getEncoderClass() {
      return AtomicInteger.class;
    }
  };

  // ─── AtomicLong ───────────────────────────────────────────────────────────

  /**
   * Codec for {@link AtomicLong} → BSON {@code Int64}.
   *
   * <p>Decode compatibility: accepts {@code Int64}, {@code Int32}, {@code Double}.</p>
   *
   * <pre>
   * new AtomicLong(1776516441883L) ↔ NumberLong("1776516441883")
   * </pre>
   */
  public static final Codec<AtomicLong> LONG = new Codec<>() {

    @Override
    public void encode(BsonWriter writer, AtomicLong value, EncoderContext ctx) {
      writer.writeInt64(value.get());
    }

    @Override
    public AtomicLong decode(BsonReader reader, DecoderContext ctx) {
      return new AtomicLong(readAsLong(reader));
    }

    @Override
    public Class<AtomicLong> getEncoderClass() {
      return AtomicLong.class;
    }
  };

  // ─── AtomicBoolean ────────────────────────────────────────────────────────

  /**
   * Codec for {@link AtomicBoolean} → BSON {@code Boolean}.
   *
   * <p>Decode compatibility: accepts {@code Boolean}; Int32/Int64 0=false, non-zero=true.</p>
   *
   * <pre>
   * new AtomicBoolean(true) ↔ true
   * </pre>
   */
  public static final Codec<AtomicBoolean> BOOLEAN = new Codec<>() {

    @Override
    public void encode(BsonWriter writer, AtomicBoolean value, EncoderContext ctx) {
      writer.writeBoolean(value.get());
    }

    @Override
    public AtomicBoolean decode(BsonReader reader, DecoderContext ctx) {
      BsonType type = reader.getCurrentBsonType();
      boolean val = switch (type) {
        case BOOLEAN -> reader.readBoolean();
        case INT32 -> reader.readInt32() != 0;
        case INT64 -> reader.readInt64() != 0;
        default -> throw new IllegalStateException("Cannot decode BSON type " + type + " into AtomicBoolean");
      };
      return new AtomicBoolean(val);
    }

    @Override
    public Class<AtomicBoolean> getEncoderClass() {
      return AtomicBoolean.class;
    }
  };

  // ─── AtomicReference ──────────────────────────────────────────────────────

  /**
   * Creates a codec for {@link AtomicReference}{@code <T>} using the provided registry
   * to resolve the inner type codec.
   *
   * <p>The inner value is encoded/decoded as its native BSON type.
   * A {@code null} reference is stored as BSON {@code null}.</p>
   *
   * <pre>
   * new AtomicReference<>("hello") ↔ "hello"
   * new AtomicReference<>(null)    ↔ null
   * </pre>
   *
   * <p><b>Registration:</b></p>
   * <pre>{@code
   * MongoCodecProvider.INSTANCE.registerFactory(AtomicReference.class, AtomicCodecs::reference);
   * }</pre>
   *
   * @param registry The live codec registry used to find the inner type codec.
   * @return A codec for {@code AtomicReference}.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static Codec<AtomicReference> reference(CodecRegistry registry) {
    return new Codec<>() {

      @Override
      public void encode(BsonWriter writer, AtomicReference value, EncoderContext ctx) {
        Object inner = value.get();
        if (inner == null) {
          writer.writeNull();
          return;
        }
        Codec codec = registry.get(inner.getClass());
        ctx.encodeWithChildContext(codec, writer, inner);
      }

      @Override
      public AtomicReference decode(BsonReader reader, DecoderContext ctx) {
        if (reader.getCurrentBsonType() == BsonType.NULL) {
          reader.readNull();
          return new AtomicReference<>(null);
        }
        // Fallback: decode as Document for complex objects
        Codec<org.bson.Document> codec = registry.get(org.bson.Document.class);
        return new AtomicReference<>(codec.decode(reader, ctx));
      }

      @Override
      public Class<AtomicReference> getEncoderClass() {
        return AtomicReference.class;
      }
    };
  }

  // ─── Internal helpers ─────────────────────────────────────────────────────

  private static int readAsInt(BsonReader reader) {
    return switch (reader.getCurrentBsonType()) {
      case INT32 -> reader.readInt32();
      case INT64 -> (int) reader.readInt64();
      case DOUBLE -> (int) reader.readDouble();
      default -> throw new IllegalStateException(
          "Cannot decode BSON type " + reader.getCurrentBsonType() + " into AtomicInteger");
    };
  }

  private static long readAsLong(BsonReader reader) {
    return switch (reader.getCurrentBsonType()) {
      case INT64 -> reader.readInt64();
      case INT32 -> reader.readInt32();
      case DOUBLE -> (long) reader.readDouble();
      default -> throw new IllegalStateException(
          "Cannot decode BSON type " + reader.getCurrentBsonType() + " into AtomicLong");
    };
  }
}

