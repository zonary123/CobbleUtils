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
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.*;
import com.kingpixel.cobbleutils.Model.conditions.Condition;
import com.kingpixel.cobbleutils.Model.messages.HiperMessage;
import com.kingpixel.cobbleutils.Model.zones.zoneshapes.ZoneShape;
import com.kingpixel.cobbleutils.adapter.*;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import com.kingpixel.cobbleutils.util.redis.handlers.RedisMessageHandler;
import com.mojang.authlib.GameProfile;
import kotlin.ranges.IntRange;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Miscellaneous utility bridge providing bridging helpers for item parsing,
 * broadcast routing channels, and legacy file management delegations.
 */
public abstract class Utils {

  @Deprecated(forRemoval = true)
  public static final Random RANDOM = new Random();

  private static final Charset charset = StandardCharsets.UTF_8;

  /**
   * Retrieves a high-performance thread-isolated random generator scalar.
   *
   * @return Active ThreadLocalRandom context instance.
   */
  public static ThreadLocalRandom getRandom() {
    return ThreadLocalRandom.current();
  }

  @Deprecated(forRemoval = true)
  private static final class GsonPrettyHolder {
    private static final Gson gsonPretty = adapters()
      .setPrettyPrinting()
      .create();
  }

  @Deprecated(forRemoval = true)
  public static Gson newGson() {
    return GsonPrettyHolder.gsonPretty;
  }

  @Deprecated(forRemoval = true)
  private static final class GsonNotPrettyHolder {
    private static final Gson gsonNotPretty = adapters()
      .create();
  }

  @Deprecated(forRemoval = true)
  public static Gson newWithoutSpacingGson() {
    return GsonNotPrettyHolder.gsonNotPretty;
  }

  @Deprecated(forRemoval = true)
  private static GsonBuilder adapters() {
    return addAdapters(new GsonBuilder()
      .disableHtmlEscaping());
  }

  /**
   * Evaluates if placeholders API hooks are registered on the classpath environment.
   *
   * @return True if placeholders library classes resolve cleanly.
   */
  public static boolean isPlaceholder() {
    try {
      Class.forName("eu.pb4.placeholders.api.Placeholders");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Deprecated(forRemoval = true)
  private static GsonBuilder addAdapters(GsonBuilder builder) {
    return builder
      .registerTypeAdapter(ElementalType.class, ElementalTypeAdapter.INSTANCE)
      .registerTypeAdapter(IntRange.class, IntRangeAdapter.INSTANCE)
      .registerTypeAdapter(NbtCompound.class, NbtCompoundAdapter.INSTANCE)
      .registerTypeAdapter(Move.class, MoveTemplateAdapter.INSTANCE)
      .registerTypeAdapter(NbtCompoundAdapter.class, NbtCompoundAdapter.INSTANCE)
      .registerTypeAdapter(DateTypeAdapter.class, new DateTypeAdapter())
      .registerTypeAdapter(Pokemon.class, PokemonAdapter.INSTANCE)
      .registerTypeAdapter(ItemStack.class, ItemStackAdapter.INSTANCE)
      .registerTypeAdapter(Instant.class, InstantTypeAdapter.INSTANCE)
      .registerTypeAdapter(ItemChance.class, ItemChanceAdapter.INSTANCE)
      .registerTypeAdapter(DurationValue.class, DurationValue.INSTANCE)
      .registerTypeAdapter(ScheduleValue.class, ScheduleValue.INSTANCE)
      .registerTypeAdapter(DataBaseType.class, DataBaseTypeAdapter.INSTANCE)
      .registerTypeAdapter(Storage.class, StorageAdapter.INSTANCE)
      .registerTypeAdapter(Condition.class, ConditionAdapter.INSTANCE)
      .registerTypeAdapter(ZoneShape.class, ZoneShapeAdapter.INSTANCE)
      .registerTypeAdapter(BlockPos.class, BlockPosAdapter.INSTANCE)
      .registerTypeAdapter(HiperMessage.class, HiperMessage.EMPTY)
      .registerTypeAdapter(Vec3d.class, Vec3dAdapter.INSTANCE)
      .registerTypeAdapter(AtomicReference.class, AtomicReferenceAdapter.INSTANCE)
      .registerTypeAdapter(Box.class, BoxAdapter.INSTANCE);
  }

  @Deprecated(forRemoval = true)
  public static Object convertNbtValue(NbtElement element) {
    return switch (element) {
      case null -> null;
      case NbtByte byteTag -> {
        byte b = byteTag.byteValue();
        if (b == 0 || b == 1) yield b == 1;
        yield b;
      }
      case NbtShort shortTag -> shortTag.shortValue();
      case NbtInt intTag -> intTag.intValue();
      case NbtLong longTag -> longTag.longValue();
      case NbtFloat floatTag -> floatTag.floatValue();
      case NbtDouble doubleTag -> doubleTag.doubleValue();
      case NbtString stringTag -> stringTag.asString();
      default -> null;
    };
  }

  @Deprecated(forRemoval = true)
  public static CompletableFuture<Boolean> writeFileAsync(
    String filePath,
    String filename,
    String data
  ) {
    if (filePath == null || filename == null || data == null) return CompletableFuture.completedFuture(false);

    Path targetPath = Paths.get(new File("").getAbsolutePath() + filePath, filename);

    return UtilsFile.writeTextAsync(targetPath, data)
      .thenApply(v -> true)
      .exceptionally(ex -> {
        CobbleUtils.LOGGER_RAW.error("Failed async file write operation via UtilsFile for path: " + targetPath, ex);
        return false;
      });
  }

  @Deprecated(forRemoval = true)
  public static boolean writeFileSync(File file, String data) {
    try {
      UtilsFile.writeText(file.toPath(), data);
      return true;
    } catch (IOException e) {
      CobbleUtils.LOGGER_RAW.error("Failed structural file write handshake for location: " + file.getPath(), e);
      return false;
    }
  }

  @Deprecated(forRemoval = true)
  public static CompletableFuture<Boolean> readFileAsync(String filePath, String filename, Consumer<String> callback) {
    if (filePath == null || filename == null || callback == null) {
      return CompletableFuture.completedFuture(false);
    }

    Path targetPath = Paths.get(new File("").getAbsolutePath() + filePath, filename);

    return UtilsFile.readTextAsync(targetPath)
      .thenApply(content -> {
        if (content == null) return false;
        callback.accept(content);
        return true;
      })
      .exceptionally(ex -> {
        CobbleUtils.LOGGER_RAW.error("Failed async text extraction loop for location: " + targetPath, ex);
        return false;
      });
  }

  @Deprecated(forRemoval = true)
  public static boolean readFileSync(File file, Consumer<String> callback) {
    try {
      String content = UtilsFile.readText(file.toPath());
      if (content == null) return false;
      callback.accept(content);
      return true;
    } catch (IOException e) {
      // Fallback handling matching historic legacy ISO-8859 extraction rules on malformed data drops
      try {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
        String content = String.join("\n", lines);
        callback.accept(content);
        return true;
      } catch (IOException ex) {
        CobbleUtils.LOGGER_RAW.error("Fallback secondary recovery mapping failed for layout path: " + file.getPath(), ex);
        return false;
      }
    }
  }

  @Deprecated(forRemoval = true)
  public static String readFileSync(File file) throws IOException {
    String content = UtilsFile.readText(file.toPath());
    if (content == null) {
      throw new IllegalArgumentException("File does not exist or is not readable: " + file.getPath());
    }
    return content;
  }

  @Deprecated(forRemoval = true)
  public static CompletableFuture<Boolean> writeFileAsync(File file, String content) {
    if (file == null || content == null) return CompletableFuture.completedFuture(false);

    return UtilsFile.writeTextAsync(file.toPath(), content)
      .thenApply(v -> true)
      .exceptionally(ex -> {
        CobbleUtils.LOGGER_RAW.error("Async context block text deployment failed on path target: " + file.getPath(), ex);
        return false;
      });
  }

  /**
   * Broadcasts a text sequence string across localized display contexts or proxy redis networks.
   *
   * @param message Text sequence payload targeting dispatch.
   */
  public static void broadcastMessage(String message) {
    if (CobbleUtils.config.isRedisMessaging()) {
      RedisMessageHandler.sendBroadcast(message, "");
    } else {
      MinecraftServer server = CobbleUtils.server;
      if (server == null) return;
      ArrayList<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
      for (ServerPlayerEntity pl : players) {
        pl.sendMessage(AdventureTranslator.toNative(message));
      }
    }
  }

  /**
   * Broadcasts a fully structured Text component layout across network players.
   *
   * @param message Native Text object payload.
   */
  public static void broadcastMessage(Text message) {
    if (CobbleUtils.config.isRedisMessaging()) {
      String textAsString = message.getString();
      RedisMessageHandler.sendBroadcast(textAsString, "");
    } else {
      MinecraftServer server = CobbleUtils.server;
      if (server == null) return;
      ArrayList<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
      for (ServerPlayerEntity pl : players) {
        pl.sendMessage(message);
      }
    }
  }

  /**
   * Broadcasts a prefix annotated text layout sequence out across matching platforms.
   *
   * @param message Text string payload data.
   * @param prefix  System tag label string mapped to prepending layouts.
   */
  public static void broadcastMessage(String message, String prefix) {
    if (CobbleUtils.config.isRedisMessaging()) {
      RedisMessageHandler.sendBroadcast(message, prefix);
    } else {
      var text = AdventureTranslator.toNative(message, prefix);
      MinecraftServer server = CobbleUtils.server;
      if (server == null) return;
      ArrayList<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
      for (ServerPlayerEntity pl : players) {
        pl.sendMessage(text);
      }
    }
  }

  @Deprecated(forRemoval = true)
  public static ItemStack parseItemId(String id) {
    return parseItemId(id, 1);
  }

  @Deprecated(forRemoval = true)
  public static ItemStack parseItemId(String id, int amount) {
    return new ItemStack(Registries.ITEM.get(Identifier.of(id)), amount);
  }

  @Deprecated(forRemoval = true)
  public static File getAbsolutePath(String directoryPath) {
    return new File(Paths.get(new File("").getAbsolutePath()) + directoryPath);
  }

  @Deprecated(forRemoval = true)
  public static List<File> getFiles(File directory) {
    List<File> fileList = new ArrayList<>();
    if (directory.exists() && directory.isDirectory()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isFile()) {
            fileList.add(file);
          } else if (file.isDirectory()) {
            fileList.addAll(getFiles(file));
          }
        }
      }
    } else {
      CobbleUtils.LOGGER_RAW.info("Directory " + directory.getPath() + " does not exist or is not a directory.");
    }
    return fileList;
  }

  @Deprecated(forRemoval = true)
  public static void removeFiles(String directoryPath) {
    File directory = getAbsolutePath(directoryPath);
    if (directory.exists() && directory.isDirectory()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isFile()) {
            file.delete();
          } else if (file.isDirectory()) {
            removeFiles(file.getAbsolutePath());
          }
        }
      }
    } else {
      CobbleUtils.LOGGER_RAW.info("Directory " + directoryPath + " does not exist or is not a directory.");
    }
  }

  @Deprecated(forRemoval = true)
  public static ItemStack parseItemModel(ItemModel itemModel, int amount) {
    String item = itemModel.getItem();
    String nbt = itemModel.getNbt();

    String[] nbtSplit = item.split("#");
    if (nbtSplit.length > 1) {
      item = nbtSplit[0];
      nbt = nbtSplit[1];
    }

    if (item.startsWith("item:")) {
      item = item.replace("item:", "");
      String[] split = item.split(":");
      item = split[1] + ":" + split[2];
      amount = Integer.parseInt(split[0]);
    }

    ItemStack itemStack = parseItemId(item, amount);
    itemStack = addThingsItemStack(itemStack, itemModel, nbt);
    return itemStack;
  }

  @Deprecated(forRemoval = true)
  public static ItemStack addThingsItemStack(ItemStack itemStack, ItemModel itemModel, String nbt) {
    if (nbt != null && !nbt.isEmpty()) {
      String item = itemModel.getItem();
      String supportNbt = "";
      String[] split = item.split("#");
      if (split.length > 1) {
        item = split[0];
        supportNbt = split[1];
      }
      String[] splitItem = item.split(":");
      if (splitItem.length > 2) {
        item = splitItem[2] + ":" + splitItem[3];
      } else {
        item = splitItem[0] + ":" + splitItem[1];
      }
      itemStack = ItemUtils.applyNbt(item, itemStack, itemModel.getNbt() == null || itemModel.getNbt().isEmpty() ?
          supportNbt
          : nbt,
        itemStack.getCount());
    }

    itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNativeWithOutPrefix(
      itemModel.getDisplayname() != null ? itemModel.getDisplayname() : "Please set a displayname for this item"));

    if (itemModel.getCustomModelData() != 0)
      itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent((int) itemModel.getCustomModelData()));

    if (itemModel.getLore() != null && !itemModel.getLore().isEmpty()) {
      itemStack.set(DataComponentTypes.LORE,
        new LoreComponent(AdventureTranslator.toNativeL(itemModel.getLore())));
    }
    return itemStack;
  }

  @Deprecated(forRemoval = true)
  public static void createDirectoryIfNeeded(String directoryPath) {
    File directory = getAbsolutePath(directoryPath);
    if (!directory.exists()) {
      directory.mkdirs();
    }
  }

  @Deprecated(forRemoval = true)
  public static ItemStack getHead(String replace, int amount) {
    ItemStack itemStack = Items.PLAYER_HEAD.getDefaultStack();
    var profile = new GameProfile(UUID.randomUUID(), replace);
    itemStack.set(DataComponentTypes.PROFILE, new ProfileComponent(profile));
    itemStack.setCount(amount);
    return itemStack;
  }

  @Deprecated(forRemoval = true)
  public static ItemStack parseItemId(String item, int amount, long customModelData) {
    ItemStack itemStack = parseItemId(item, amount);
    if (customModelData != 0)
      itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent((int) customModelData));
    return itemStack;
  }

  @Deprecated(forRemoval = true)
  public static StringBuilder replaceStringBuilder(StringBuilder sb, Map<String, String> placeholders) {
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      String placeholder = entry.getKey();
      String value = entry.getValue();
      int index = sb.indexOf(placeholder);
      while (index != -1) {
        sb.replace(index, index + placeholder.length(), value);
        index += value.length();
        index = sb.indexOf(placeholder, index);
      }
    }
    return sb;
  }
}