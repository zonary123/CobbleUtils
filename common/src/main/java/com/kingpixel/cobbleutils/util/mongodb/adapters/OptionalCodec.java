package com.kingpixel.cobbleutils.util.mongodb.adapters;

import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.Optional;

/**
 * BSON codec for {@link Optional}.
 *
 * <h3>Encoding</h3>
 * <ul>
 *   <li>{@code Optional.empty()} → BSON {@code null}.</li>
 *   <li>{@code Optional.of(value)} → encoded inner value using the registry codec for that type.</li>
 * </ul>
 *
 * <h3>Decoding</h3>
 * <ul>
 *   <li>BSON {@code null} → {@code Optional.empty()}.</li>
 *   <li>Any other BSON type → {@code Optional.of(decoded value)} (decoded as {@code Document}).</li>
 * </ul>
 *
 * <p><b>Note:</b> Due to Java type erasure, decoding without the inner type information
 * falls back to {@code Document} for complex types. For simple scalar fields prefer
 * using the raw type directly in your model and wrapping in Optional at the application layer.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class PlayerData {
 *     private Optional<String> nickname = Optional.empty();
 * }
 * }</pre>
 */
public class OptionalCodec<T> implements Codec<Optional<T>> {

  private final CodecRegistry registry;

  public OptionalCodec(CodecRegistry registry) {
    this.registry = registry;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void encode(BsonWriter writer, Optional<T> value, EncoderContext ctx) {
    if (value.isEmpty()) {
      writer.writeNull();
      return;
    }
    T inner = value.get();
    Codec codec = registry.get(inner.getClass());
    ctx.encodeWithChildContext(codec, writer, inner);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<T> decode(BsonReader reader, DecoderContext ctx) {
    if (reader.getCurrentBsonType() == BsonType.NULL) {
      reader.readNull();
      return Optional.empty();
    }
    // Fallback: decode as Document for complex objects
    Codec<org.bson.Document> codec = registry.get(org.bson.Document.class);
    return (Optional<T>) Optional.ofNullable(codec.decode(reader, ctx));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<Optional<T>> getEncoderClass() {
    return (Class<Optional<T>>) (Class<?>) Optional.class;
  }
}

