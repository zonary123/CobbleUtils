package com.kingpixel.cobbleutils.util.mongodb;

import com.kingpixel.cobbleutils.util.mongodb.adapters.*;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Central BSON {@link CodecProvider} for CobbleUtils.
 *
 * <p>Maintains a runtime registry of codecs so that any mod can contribute
 * custom BSON codecs without touching CobbleUtils internals.</p>
 *
 * <h3>Built-in codecs</h3>
 * <table border="1">
 *   <tr><th>Java type</th><th>BSON type</th><th>Codec</th></tr>
 *   <tr><td>{@link BigDecimal}</td><td>Decimal128</td><td>{@link BigDecimalCodec}</td></tr>
 *   <tr><td>{@link UUID}</td><td>Binary (subtype 4)</td><td>{@link UUIDCodec}</td></tr>
 *   <tr><td>{@link Instant}</td><td>DateTime</td><td>{@link InstantCodec}</td></tr>
 *   <tr><td>{@link Optional}</td><td>inner value or null</td><td>{@link OptionalCodec}</td></tr>
 *   <tr><td>{@link AtomicInteger}</td><td>Int32</td><td>{@link AtomicCodecs#INTEGER}</td></tr>
 *   <tr><td>{@link AtomicLong}</td><td>Int64</td><td>{@link AtomicCodecs#LONG}</td></tr>
 *   <tr><td>{@link AtomicBoolean}</td><td>Boolean</td><td>{@link AtomicCodecs#BOOLEAN}</td></tr>
 *   <tr><td>{@link AtomicReference}</td><td>inner value or null</td><td>{@link AtomicCodecs#reference}</td></tr>
 * </table>
 *
 * <h3>Registering a custom codec (simple — no registry needed)</h3>
 * <pre>{@code
 * MongoCodecProvider.register(MyType.class, MyTypeCodec.INSTANCE);
 * }</pre>
 *
 * <h3>Registering a codec that needs the CodecRegistry (e.g. for nested documents)</h3>
 * <pre>{@code
 * MongoCodecProvider.registerFactory(MyType.class, registry -> new MyTypeCodec(registry));
 * }</pre>
 *
 * <h3>Removing a codec</h3>
 * <pre>{@code
 * MongoCodecProvider.unregister(MyType.class);
 * }</pre>
 *
 * <h3>Checking registration</h3>
 * <pre>{@code
 * boolean registered = MongoCodecProvider.isRegistered(MyType.class);
 * }</pre>
 *
 * <h3>Registration in MongoDBManager</h3>
 * <pre>{@code
 * MongoClientSettings settings = MongoClientSettings.builder()
 *     .codecRegistry(CodecRegistries.fromRegistries(
 *         CodecRegistries.fromProviders(MongoCodecProvider.INSTANCE),
 *         MongoClientSettings.getDefaultCodecRegistry()
 *     ))
 *     .build();
 * }</pre>
 *
 * @see BigDecimalCodec
 * @see UUIDCodec
 * @see OptionalCodec
 * @see InstantCodec
 */
public class MongoCodecProvider implements CodecProvider {

  public static final MongoCodecProvider INSTANCE = new MongoCodecProvider();

  /**
   * Registry of codec factories indexed by Java type.
   * A factory receives the live {@link CodecRegistry} so codecs that need to
   * delegate to other codecs (e.g. for nested documents) can do so.
   */
  private final Map<Class<?>, Function<CodecRegistry, Codec<?>>> registry = new ConcurrentHashMap<>();

  private MongoCodecProvider() {
    // Scalar codecs — no registry needed
    registry.put(BigDecimal.class, c -> BigDecimalCodec.INSTANCE);
    registry.put(UUID.class, c -> UUIDCodec.INSTANCE);
    registry.put(Instant.class, c -> InstantCodec.INSTANCE);
    registry.put(AtomicInteger.class, c -> AtomicCodecs.INTEGER);
    registry.put(AtomicLong.class, c -> AtomicCodecs.LONG);
    registry.put(AtomicBoolean.class, c -> AtomicCodecs.BOOLEAN);
    registry.put(Optional.class, OptionalCodec::new);
    registry.put(AtomicReference.class, AtomicCodecs::reference);
  }

  // ─── CodecProvider ────────────────────────────────────────────────────────

  @SuppressWarnings("unchecked")
  @Override
  public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
    Function<CodecRegistry, Codec<?>> factory = this.registry.get(clazz);
    if (factory == null) return null;
    return (Codec<T>) factory.apply(registry);
  }

  // ─── Registration API ─────────────────────────────────────────────────────

  /**
   * Registers a codec for the given type.
   *
   * <p>Use this overload when your codec does not need access to the live
   * {@link CodecRegistry} (i.e. it handles a scalar or self-contained type).</p>
   *
   * <pre>{@code
   * MongoCodecProvider.register(MyType.class, MyTypeCodec.INSTANCE);
   * }</pre>
   *
   * @param type  The Java type handled by the codec.
   * @param codec The codec instance.
   * @param <T>   The type parameter.
   */
  public <T> void register(Class<T> type, Codec<T> codec) {
    registry.put(type, c -> codec);
  }

  /**
   * Registers a codec factory for the given type.
   *
   * <p>Use this overload when your codec needs access to the live {@link CodecRegistry}
   * to encode/decode nested types (e.g. documents, lists, or other custom types).</p>
   *
   * <pre>{@code
   * MongoCodecProvider.registerFactory(MyType.class, reg -> new MyTypeCodec(reg));
   * }</pre>
   *
   * @param type    The Java type handled by the codec.
   * @param factory A function that receives the registry and returns a codec instance.
   * @param <T>     The type parameter.
   */
  public <T> void registerFactory(Class<T> type, Function<CodecRegistry, Codec<T>> factory) {
    registry.put(type, factory::apply);
  }

  /**
   * Removes the codec registered for the given type.
   *
   * <pre>{@code
   * MongoCodecProvider.unregister(MyType.class);
   * }</pre>
   *
   * @param type The type whose codec should be removed.
   */
  public void unregister(Class<?> type) {
    registry.remove(type);
  }

  /**
   * Returns {@code true} if a codec is registered for the given type.
   *
   * @param type The type to check.
   * @return {@code true} if registered.
   */
  public boolean isRegistered(Class<?> type) {
    return registry.containsKey(type);
  }

  /**
   * Returns the number of codecs currently registered.
   *
   * @return Codec count.
   */
  public int size() {
    return registry.size();
  }
}
