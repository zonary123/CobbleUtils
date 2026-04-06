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
import com.kingpixel.cobbleutils.Model.conditions.Condition;
import com.kingpixel.cobbleutils.Model.messages.HiperMessage;
import com.kingpixel.cobbleutils.Model.zones.zoneshapes.ZoneShape;
import com.kingpixel.cobbleutils.adapter.*;
import com.kingpixel.cobbleutils.database.users.models.Storage;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Utility class providing safe JSON file operations.
 *
 * <p>This class is designed for concurrent environments such as Minecraft
 * servers or asynchronous plugins.</p>
 *
 * <p>Main features:</p>
 * <ul>
 *     <li>Thread-safe file access using {@link ReadWriteLock}</li>
 *     <li>Atomic file writes</li>
 *     <li>Async IO operations</li>
 *     <li>Shared {@link Gson} instance</li>
 *     <li>Atomic read-modify-write operations</li>
 * </ul>
 *
 * @author Carlos Varas Alonso
 * @since 1.0
 */
public final class UtilsFile {

  private UtilsFile() {
  }


  public static final ExecutorService IO_EXECUTOR =
    new ThreadPoolExecutor(
      1,
      8,
      5L,
      TimeUnit.MINUTES,
      new LinkedBlockingQueue<>(),
      r -> new Thread(r, "UtilsFile-IO")
    );

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
    registerAdapter(DataBaseType.class, DataBaseTypeAdapter.INSTANCE);
    registerAdapter(Storage.class, StorageAdapter.INSTANCE);
    registerAdapter(HiperMessage.class, HiperMessage.EMPTY);
    registerAdapter(Vec3d.class, Vec3dAdapter.INSTANCE);
    registerAdapter(AtomicReference.class, AtomicReferenceAdapter.INSTANCE);
    registerAdapter(Box.class, BoxAdapter.INSTANCE);
    registerAdapter(BlockPos.class, BlockPosAdapter.INSTANCE);
    registerAdapter(ZoneShape.class, ZoneShapeAdapter.INSTANCE);
    registerAdapter(Condition.class, ConditionAdapter.INSTANCE);
    // Cobblemon adapters
    registerAdapter(Pokemon.class, PokemonAdapter.INSTANCE);
    registerAdapter(Move.class, MoveTemplateAdapter.INSTANCE);
    registerAdapter(IntRange.class, IntRangeAdapter.INSTANCE);
    registerAdapter(ElementalType.class, ElementalTypeAdapter.INSTANCE);
  }

  /**
   * Retrieves the lock associated with a file path.
   *
   * @param path file path
   * @return lock used for that file
   */
  private static ReadWriteLock lock(Path path) {
    Path normalized = path.toAbsolutePath().normalize();
    return FILE_LOCKS.computeIfAbsent(normalized, p -> new ReentrantReadWriteLock());
  }

  /**
   * Registers a custom Gson adapter.
   *
   * @param type    type handled by the adapter
   * @param adapter adapter instance
   */
  public static synchronized void registerAdapter(@Nonnull Type type, @Nonnull Object adapter) {
    ADAPTERS.put(type, adapter);
    rebuildGson();
  }

  /**
   * Returns the shared Gson instance.
   *
   * @return configured Gson instance
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
   * Rebuilds the Gson instance using the registered adapters.
   */
  private static synchronized void rebuildGson() {

    GsonBuilder builder = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping();

    ADAPTERS.forEach(builder::registerTypeAdapter);

    GSON = builder.create();
  }

  /**
   * Reads a JSON file and converts it into an object.
   *
   * @param path  file path
   * @param clazz class type
   * @param <T>   object type
   * @return parsed object or null if the file does not exist
   * @throws IOException if file reading fails
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
   * Reads a JSON file asynchronously.
   *
   * @param path  file path
   * @param clazz class type
   * @param <T>   object type
   * @return future containing the parsed object
   */
  public static <T> CompletableFuture<@Nullable T> readAsync(Path path, Class<T> clazz) {

    return CompletableFuture.supplyAsync(() -> {
      try {
        return read(path, clazz);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, IO_EXECUTOR);
  }

  /**
   * Reads a JSON file containing a list.
   *
   * @param path  file path
   * @param clazz element class
   * @param <T>   list element type
   * @return list of objects or null if file does not exist
   * @throws IOException if reading fails
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
   * Writes an object to a JSON file using atomic replacement.
   *
   * @param path   file path
   * @param object object to write
   * @throws IOException if writing fails
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

        Files.move(
          tmp,
          path,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE
        );

      } catch (AtomicMoveNotSupportedException e) {

        Files.move(
          tmp,
          path,
          StandardCopyOption.REPLACE_EXISTING
        );

      }

    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Writes a JSON file asynchronously.
   *
   * @param path   file path
   * @param object object to serialize
   * @return future representing completion
   */
  public static CompletableFuture<Void> writeAsync(Path path, Object object) {

    return CompletableFuture.runAsync(() -> {
      try {
        write(path, object);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, IO_EXECUTOR);
  }

  /**
   * Performs an atomic read-modify-write operation.
   *
   * @param path            file path
   * @param clazz           object class
   * @param updater         function used to update the object
   * @param defaultSupplier supplier used if the file does not exist
   * @param <T>             object type
   * @return updated object
   * @throws IOException if IO fails
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
   * Mutates an existing JSON object stored in a file.
   *
   * @param path    file path
   * @param clazz   object class
   * @param mutator mutation function
   * @param <T>     object type
   * @throws IOException if IO fails
   */
  public static <T> void mutateAtomic(
    Path path,
    Class<T> clazz,
    Consumer<T> mutator
  ) throws IOException {

    updateAtomic(
      path,
      clazz,
      value -> {
        mutator.accept(value);
        return value;
      },
      null
    );
  }

  /**
   * Reads a JSON file or creates it using the supplied value.
   *
   * @param path     file path
   * @param clazz    object class
   * @param supplier default supplier
   * @param <T>      object type
   * @return existing or newly created object
   * @throws IOException if IO fails
   */
  public static <T> T readOrCreate(Path path, Class<T> clazz, Supplier<T> supplier) throws IOException {

    T value = read(path, clazz);

    if (value != null) return value;

    value = supplier.get();

    write(path, value);

    return value;
  }

  /**
   * Reads a file or returns a default value.
   *
   * @param path         file path
   * @param clazz        object class
   * @param defaultValue default value
   * @param <T>          object type
   * @return object or default
   * @throws IOException if IO fails
   */
  public static <T> T readOrDefault(Path path, Class<T> clazz, T defaultValue) throws IOException {

    T value = read(path, clazz);

    return value == null ? defaultValue : value;
  }

  /**
   * Returns whether a file exists.
   *
   * @param path file path
   * @return true if exists
   */
  public static boolean exists(Path path) {
    return Files.exists(path);
  }

  /**
   * Returns the size of a file.
   *
   * @param path file path
   * @return file size
   * @throws IOException if IO fails
   */
  public static long size(Path path) throws IOException {
    return Files.size(path);
  }

  /**
   * Lists files inside a directory.
   *
   * @param directory directory path
   * @return stream of paths
   * @throws IOException if IO fails
   */
  public static Stream<Path> list(Path directory) throws IOException {
    return Files.list(directory);
  }

  /**
   * Returns all files matching a predicate.
   *
   * @param folder folder to search
   * @param filter filter predicate
   * @return list of files
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
   * Returns all files in a folder.
   *
   * @param folder folder path
   * @return list of files
   */
  public static List<Path> getAllFiles(Path folder) {
    return getFiles(folder, p -> true);
  }

  /**
   * Returns all JSON files inside a folder.
   *
   * @param folder folder path
   * @return list of JSON files
   */
  public static List<Path> getAllJsonFiles(Path folder) {
    return getFiles(folder, p ->
      p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")
    );
  }

  /**
   * Reads plain text from a file.
   *
   * @param path file path
   * @return text content, or null if file does not exist
   * @throws IOException if reading fails
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
   * Reads plain text asynchronously.
   *
   * @param path file path
   * @return future containing text content
   */
  public static CompletableFuture<@Nullable String> readTextAsync(Path path) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return readText(path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, IO_EXECUTOR);
  }

  /**
   * Writes plain text to a file using atomic replacement.
   *
   * @param path    file path
   * @param content text content
   * @throws IOException if writing fails
   */
  public static void writeText(@Nonnull Path path, @Nonnull String content) throws IOException {

    ReadWriteLock lock = lock(path);
    lock.writeLock().lock();

    try {
      Files.createDirectories(path.getParent());

      Path tmp = path.resolveSibling(path.getFileName() + ".tmp");

      Files.writeString(
        tmp,
        content,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      );

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
   * Writes plain text asynchronously.
   *
   * @param path    file path
   * @param content text content
   * @return future representing completion
   */
  public static CompletableFuture<Void> writeTextAsync(Path path, String content) {
    return CompletableFuture.runAsync(() -> {
      try {
        writeText(path, content);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }, IO_EXECUTOR);
  }

}