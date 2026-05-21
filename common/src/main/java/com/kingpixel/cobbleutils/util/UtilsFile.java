package com.kingpixel.cobbleutils.util;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.adapters.MoveTemplateAdapter;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.adapters.ElementalTypeAdapter;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.adapters.IntRangeAdapter;
import com.cobblemon.mod.common.util.adapters.NbtCompoundAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.ScheduleValue;
import com.kingpixel.cobbleutils.Model.conditions.Condition;
import com.kingpixel.cobbleutils.Model.messages.HiperMessage;
import com.kingpixel.cobbleutils.Model.zones.zoneshapes.ZoneShape;
import com.kingpixel.cobbleutils.adapter.*;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.async.AsyncContext;
import com.kingpixel.cobbleutils.util.async.UtilsAsync;
import kotlin.ranges.IntRange;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Utility class providing safe and high-performance JSON file operations.
 *
 * <p>This class features an advanced multi-threaded design tailored for concurrent
 * high-traffic Minecraft server environments, ensuring thread-safe operations
 * and preventing blocking operations from stalling the main server ticks.</p>
 *
 * <p>Key Architecture Enhancements:</p>
 * <ul>
 * <li>Thread-safe per-file access coordination using {@link ReadWriteLock} mechanics.</li>
 * <li>Atomic transaction file swaps preventing unexpected binary or text corruption.</li>
 * <li>Integrated with a resilient, fault-tolerant {@link AsyncContext} worker architecture.</li>
 * <li>Centralized, cached {@link Gson} instance infrastructure using dynamic type adapters.</li>
 * <li>Atomic read-modify-write transactional mutation boundaries.</li>
 * </ul>
 *
 * @author Carlos Varas Alonso
 * @since 1.0
 */
public final class UtilsFile {

  private UtilsFile() {
    // Utility class instantiation guard
  }

  /**
   * Dedicated asynchrony worker sub-pool context tailored for CobbleUtils I/O tasks.
   * Handles high-load spikes smoothly with an elastic configuration (2-8 core threads).
   */
  public static final AsyncContext ASYNC = UtilsAsync.createContext("cobbleutils-io", "CobbleUtils-IO", 2, 8);

  private static final ConcurrentMap<Path, ReadWriteLock> FILE_LOCKS = new ConcurrentHashMap<>();

  private static volatile Gson GSON;

  private static final ConcurrentMap<Type, Object> ADAPTERS = new ConcurrentHashMap<>();

  static {
    registerAdapter(NbtCompound.class, NbtCompoundAdapter.INSTANCE);
    registerAdapter(NbtCompoundAdapter.class, NbtCompoundAdapter.INSTANCE);
    registerAdapter(DateTypeAdapter.class, new DateTypeAdapter());
    registerAdapter(ItemStack.class, ItemStackAdapter.INSTANCE);
    registerAdapter(Instant.class, InstantTypeAdapter.INSTANCE);
    registerAdapter(ItemChance.class, ItemChanceAdapter.INSTANCE);
    registerAdapter(DurationValue.class, DurationValue.INSTANCE);
    registerAdapter(ScheduleValue.class, ScheduleValue.INSTANCE);
    registerAdapter(DataBaseType.class, DataBaseTypeAdapter.INSTANCE);
    registerAdapter(Storage.class, StorageAdapter.INSTANCE);
    registerAdapter(HiperMessage.class, HiperMessage.EMPTY);
    registerAdapter(Vec3d.class, Vec3dAdapter.INSTANCE);
    registerAdapter(AtomicReference.class, AtomicReferenceAdapter.INSTANCE);
    registerAdapter(Box.class, BoxAdapter.INSTANCE);
    registerAdapter(BlockPos.class, BlockPosAdapter.INSTANCE);
    registerAdapter(ZoneShape.class, ZoneShapeAdapter.INSTANCE);
    registerAdapter(Condition.class, ConditionAdapter.INSTANCE);
    // Java type adapters
    registerAdapter(BigDecimal.class, BigDecimalAdapter.INSTANCE);
    registerAdapter(Optional.class, OptionalAdapter.INSTANCE);
    // Cobblemon type adapters
    registerAdapter(Pokemon.class, PokemonAdapter.INSTANCE);
    registerAdapter(Move.class, MoveTemplateAdapter.INSTANCE);
    registerAdapter(IntRange.class, IntRangeAdapter.INSTANCE);
    registerAdapter(ElementalType.class, ElementalTypeAdapter.INSTANCE);
  }

  /**
   * Resolves or maps a specialized thread-safe lock for a specific target file path.
   *
   * @param path The targeted file path structure.
   *
   * @return A thread-safe ReadWriteLock allocated for the absolute normalized file reference.
   */
  private static ReadWriteLock lock(Path path) {
    Path normalized = path.toAbsolutePath().normalize();
    return FILE_LOCKS.computeIfAbsent(normalized, p -> new ReentrantReadWriteLock());
  }

  /**
   * Registers a globally available custom mapping type adapter to the shared Gson pipeline.
   *
   * @param type    The target class structure handled by the mapper.
   * @param adapter The custom deserializer instance mapping logic.
   */
  public static synchronized void registerAdapter(@Nonnull Type type, @Nonnull Object adapter) {
    ADAPTERS.put(type, adapter);
    rebuildGson();
  }

  /**
   * Retrieves the globally configured shared Gson mapping pipeline instance.
   *
   * @return The ready-to-use configured Gson instance.
   */
  @Nonnull
  public static Gson getGson() {
    Gson local = GSON;
    if (local == null) {
      synchronized (UtilsFile.class) {
        if (GSON == null) {
          rebuildGson();
        }
        local = GSON;
      }
    }
    return local;
  }

  /**
   * Flushes and dynamically reconstructs the internal Gson configuration pipeline.
   */
  private static synchronized void rebuildGson() {
    GsonBuilder builder = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping();

    ADAPTERS.forEach(builder::registerTypeAdapter);
    GSON = builder.create();
  }

  /**
   * Reads a local JSON file transactionally and maps it into an explicit target object structure.
   *
   * @param path  The source filesystem path context.
   * @param clazz The parsed mapping conversion destination class.
   * @param <T>   The returned abstract mapped target layout.
   *
   * @return An instantiated representation mapping the file tokens, or null if missing.
   *
   * @throws IOException If disk reading operations experience hardware or lock limits.
   */
  @Nullable
  public static <T> T read(@Nonnull Path path, @Nonnull Class<T> clazz) throws IOException {
    if (Files.notExists(path)) return null;

    ReadWriteLock lock = lock(path);
    lock.readLock().lock();
    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return getGson().fromJson(reader, clazz);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Non-blocking transaction layer requesting asynchronous JSON data extraction routed
   * via the resilient fallback-protected AsyncContext worker pool.
   *
   * @param path  The target filesystem data path.
   * @param clazz The final evaluation mapping class type destination.
   * @param <T>   The structural abstract type tracking context.
   *
   * @return A trackable CompletableFuture mapping out the targeted parsed file state.
   */
  public static <T> CompletableFuture<@Nullable T> readAsync(Path path, Class<T> clazz) {
    return ASYNC.supply(() -> {
      try {
        return read(path, clazz);
      } catch (IOException e) {
        throw new CompletionException(e);
      }
    });
  }

  /**
   * Reads a local JSON array and transforms it seamlessly into an explicit parameterized Java List collection.
   *
   * @param path  The target directory or file structure path.
   * @param clazz The underlying collection data structural mapping class.
   * @param <T>   The inner list generic entity tracking definition type.
   *
   * @return A loaded collection matching the filesystem persistence state, or null if missing.
   *
   * @throws IOException If streaming pipeline boundaries lock or fail internally.
   */
  @Nullable
  public static <T> List<T> readList(Path path, Class<T> clazz) throws IOException {
    if (Files.notExists(path)) return null;

    ReadWriteLock lock = lock(path);
    lock.readLock().lock();
    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      Type type = TypeToken.getParameterized(List.class, clazz).getType();
      return getGson().fromJson(reader, type);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Serializes and persists a target object data block structure down to disk transactionally.
   * Features an automated, non-destructive atomic temporary-file swap backup replacement pipeline.
   *
   * @param path   The final storage filesystem destination path.
   * @param object The structured data block instance targeted for serialization.
   *
   * @throws IOException If data conversion pipeline triggers hardware or access violations.
   */
  public static void write(@Nonnull Path path, @Nonnull Object object) throws IOException {
    ReadWriteLock lock = lock(path);
    lock.writeLock().lock();
    try {
      Files.createDirectories(path.getParent());
      Path tmp = path.resolveSibling(path.getFileName() + ".tmp");

      try (BufferedWriter writer = Files.newBufferedWriter(
        tmp,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )) {
        getGson().toJson(object, writer);
      }

      try {
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Non-blocking async data stream serialization layer dispatching data preservation
   * blocks cleanly via the background execution layers of the AsyncContext.
   *
   * @param path   The targeted filesystem destination path.
   * @param object The structured payload block instance slated for serialization.
   *
   * @return A CompletableFuture tracking background completion cycles of the disk mutation.
   */
  public static CompletableFuture<Void> writeAsync(Path path, Object object) {
    return ASYNC.runAsync(() -> {
      try {
        write(path, object);
      } catch (IOException e) {
        throw new CompletionException(e);
      }
    });
  }

  /**
   * Coordinates a strict thread-safe thread-synchronized read-modify-write transactional sequence.
   * Guarantees abstract model state mutability checks safely across high-concurrency player interactions.
   *
   * @param path            The targeting source structural storage path.
   * @param clazz           The structural evaluation target model class.
   * @param updater         The functional operation sequence modifying the existing values.
   * @param defaultSupplier Fallback instantiation supplier fallback block if file mappings are missing.
   * @param <T>             The operational tracking structural data class template.
   *
   * @return The freshly modified and safely preserved updated data object state.
   *
   * @throws IOException If lock acquisition metrics or operational storage pipelines experience faults.
   */
  public static <T> T updateAtomic(
    Path path,
    Class<T> clazz,
    UnaryOperator<T> updater,
    Supplier<T> defaultSupplier
  ) throws IOException {
    ReadWriteLock lock = lock(path);
    lock.writeLock().lock();
    try {
      T value = Files.exists(path) ? read(path, clazz) : null;

      if (value == null && defaultSupplier != null) {
        value = defaultSupplier.get();
      }

      value = updater.apply(value);
      write(path, value);
      return value;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Transactionally mutates a targeted in-memory structural instance data layout and flushes it to disk.
   *
   * @param path    The targeting file context configuration layout.
   * @param clazz   The destination mapping layout model class reference.
   * @param mutator The inline tracking lambda altering the active fields.
   * @param <T>     The model tracking layout category template type.
   *
   * @throws IOException If disk pipeline writing restrictions block updates.
   */
  public static <T> void mutateAtomic(Path path, Class<T> clazz, Consumer<T> mutator) throws IOException {
    updateAtomic(path, clazz, value -> {
      mutator.accept(value);
      return value;
    }, null);
  }

  /**
   * Attempts to locate and parse a JSON storage entity path, generating it instantly using default definitions if absent.
   *
   * @param path     The target data path context tracking variable.
   * @param clazz    The explicit destination configuration data class.
   * @param supplier Fallback block providing data defaults if the layout is missing.
   * @param <T>      The target generic definition configuration template category.
   *
   * @return The existing persistent file state structure context or a newly minted representation.
   *
   * @throws IOException If internal structural generation or writing boundaries collapse.
   */
  public static <T> T readOrCreate(Path path, Class<T> clazz, Supplier<T> supplier) throws IOException {
    T value = read(path, clazz);
    if (value != null) return value;

    value = supplier.get();
    write(path, value);
    return value;
  }

  /**
   * Evaluates and extracts a persistent model path layout, routing back to explicit safe instances if missing.
   *
   * @param path         The matching structural storage configuration file path.
   * @param clazz        The final conversion output matching model class.
   * @param defaultValue The structural safe object instance used if missing.
   * @param <T>          The general abstract structural template configuration classifier.
   *
   * @return The target parsed content block context or the provided baseline default.
   *
   * @throws IOException If disk reading locks cannot be acquired or cleared safely.
   */
  public static <T> T readOrDefault(Path path, Class<T> clazz, T defaultValue) throws IOException {
    T value = read(path, clazz);
    return value == null ? defaultValue : value;
  }

  /**
   * Evaluates if a target structural file path location is present in the filesystem.
   *
   * @param path The matching target file reference tracking context.
   *
   * @return True if the targeted element exists and can be located cleanly.
   */
  public static boolean exists(Path path) {
    return Files.exists(path);
  }

  /**
   * Returns the structural byte capacity metrics mapped for the target filesystem path.
   *
   * @param path The matching target data file structural path context.
   *
   * @return The tracked structural length capacity mapping metrics in bytes.
   *
   * @throws IOException If checking file capacity maps fails on hardware boundaries.
   */
  public static long size(Path path) throws IOException {
    return Files.size(path);
  }

  /**
   * Opens an active read stream resolving child nodes or items stored directly inside the parent target context path.
   *
   * @param directory The parent structural container path targeted for analysis.
   *
   * @return An open, sequential Path Stream mapping child directory assets.
   *
   * @throws IOException If access rights, locks, or path contexts are closed.
   */
  public static Stream<Path> list(Path directory) throws IOException {
    return Files.list(directory);
  }

  /**
   * Scans a root context folder recursively, resolving and sorting out all elements matching a strict verification query filter.
   *
   * @param folder The parent starting folder directory reference.
   * @param filter The condition logic validating items for entry.
   *
   * @return A list collection filled with all structural paths meeting matching metrics.
   */
  public static List<Path> getFiles(Path folder, Predicate<Path> filter) {
    if (folder == null || !Files.exists(folder) || !Files.isDirectory(folder)) {
      return List.of();
    }

    try (Stream<Path> walk = Files.walk(folder)) {
      return walk
        .filter(Files::isRegularFile)
        .filter(filter)
        .toList();
    } catch (IOException e) {
      e.printStackTrace();
      return List.of();
    }
  }

  /**
   * Scans and aggregates every unique file element located anywhere within a target directory tree framework.
   *
   * @param folder The parent target location container context.
   *
   * @return A populated List collection referencing all child elements found inside the directory tree.
   */
  public static List<Path> getAllFiles(Path folder) {
    return getFiles(folder, p -> true);
  }

  /**
   * Scans and aggregates every element featuring a strict localized validation layout match ending in standard .json extensions.
   *
   * @param folder The parent configuration folder location targeted for scanning.
   *
   * @return A sorted List grouping matching file references.
   */
  public static List<Path> getAllJsonFiles(Path folder) {
    return getFiles(folder, p ->
      p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")
    );
  }

  /**
   * Reads plain unformatted text content context values cleanly out of a target filesystem location.
   *
   * @param path The targeted flat text file source storage destination.
   *
   * @return A complete text block representation string context, or null if missing.
   *
   * @throws IOException If operational text stream reading boundaries drop or error out.
   */
  @Nullable
  public static String readText(@Nonnull Path path) throws IOException {
    if (Files.notExists(path)) return null;

    ReadWriteLock lock = lock(path);
    lock.readLock().lock();
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Non-blocking transactional process requesting raw flat text extraction safely routed
   * via background threads via the shared AsyncContext system wrapper.
   *
   * @param path The targeted flat data text file storage source.
   *
   * @return A CompletableFuture mapping tracking abstract string resolution.
   */
  public static CompletableFuture<@Nullable String> readTextAsync(Path path) {
    return ASYNC.supply(() -> {
      try {
        return readText(path);
      } catch (IOException e) {
        throw new CompletionException(e);
      }
    });
  }

  /**
   * Persists a string text blocks safely to disk using atomic replacement routines.
   *
   * @param path    The targeted file storage path destination.
   * @param content The plain text string block structure requested for preservation.
   *
   * @throws IOException If text processing limits or file lock boundaries fail.
   */
  public static void writeText(@Nonnull Path path, @Nonnull String content) throws IOException {
    ReadWriteLock lock = lock(path);
    lock.writeLock().lock();
    try {
      Files.createDirectories(path.getParent());
      Path tmp = path.resolveSibling(path.getFileName() + ".tmp");

      Files.writeString(tmp, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

      try {
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Non-blocking async transaction layer requesting plain text preservation safely dispatched
   * using backend threads via the AsyncContext architecture.
   *
   * @param path    The final destination target storage path.
   * @param content The text content requested for preservation.
   *
   * @return A CompletableFuture abstracting the ongoing background thread lifecycle states.
   */
  public static CompletableFuture<Void> writeTextAsync(Path path, String content) {
    return ASYNC.runAsync(() -> {
      try {
        writeText(path, content);
      } catch (IOException e) {
        throw new CompletionException(e);
      }
    });
  }

  /**
   * Pulls structural metrics and worker runtime operational diagnostics telemetry directly
   * out of the underlying active I/O environment for debugging commands.
   *
   * @return A formatted telemetry monitoring layout context tracking worker load metrics.
   */
  public static String getIOStats() {
    return ASYNC.getStatsSummary();
  }
}