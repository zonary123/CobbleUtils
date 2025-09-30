package com.kingpixel.cobbleutils.util;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.adapters.MoveTemplateAdapter;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.adapters.ElementalTypeAdapter;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.adapters.IntRangeAdapter;
import com.cobblemon.mod.common.util.adapters.NbtCompoundAdapter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.messages.HiperMessage;
import com.kingpixel.cobbleutils.adapter.*;
import com.mojang.authlib.GameProfile;
import kotlin.ranges.IntRange;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public abstract class Utils {
  /**
   * @deprecated Use {@link Utils#getRandom()} instead.
   */
  @Deprecated
  public static final Random RANDOM = new Random();

  private static final Charset charset = StandardCharsets.UTF_8;

  public static ThreadLocalRandom getRandom() {
    return ThreadLocalRandom.current();
  }

  private static final class GsonPrettyHolder {
    private static final Gson gsonPretty = adapters()
      .setPrettyPrinting()
      .create();
  }

  public static Gson newGson() {
    return GsonPrettyHolder.gsonPretty;
  }

  private static final class GsonNotPrettyHolder {
    private static final Gson gsonNotPretty = adapters()
      .create();
  }

  public static Gson newWithoutSpacingGson() {
    return GsonNotPrettyHolder.gsonNotPretty;
  }


  private static GsonBuilder adapters() {
    return addAdapters(new GsonBuilder()
      .disableHtmlEscaping());
  }

  public static boolean isPlaceholder() {
    try {
      Class.forName("eu.pb4.placeholders.api.Placeholders");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

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
      .registerTypeAdapter(DataBaseType.class, DataBaseTypeAdapter.INSTANCE)
      .registerTypeAdapter(HiperMessage.class, HiperMessage.EMPTY);
  }

  public static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(16, new ThreadFactoryBuilder()
    .setNameFormat("CobbleUtils IO Executor-%d")
    .build());

  public static CompletableFuture<Boolean> writeFileAsync(String filePath, String filename, String data) {
    if (filePath == null || filename == null || data == null) {
      CobbleUtils.LOGGER.error("Invalid input: filePath, filename, or data is null.");
      return CompletableFuture.completedFuture(false);
    }

    return CompletableFuture.supplyAsync(() -> {
      Path path = Paths.get(new File("").getAbsolutePath() + filePath, filename);
      File file = path.toFile();

      try {
        if (!Files.exists(path.getParent())) {
          Files.createDirectories(path.getParent());
        }

        Files.writeString(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return true;
      } catch (IOException e) {
        CobbleUtils.LOGGER.error("Error writing file: " + file.getPath() + ". " + e);
        return false;
      }
    }, IO_EXECUTOR);
  }

  public static boolean writeFileSync(File file, String data) {
    try (FileWriter writer = new FileWriter(file, charset)) {
      writer.write(data);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  public static CompletableFuture<Boolean> readFileAsync(String filePath, String filename, Consumer<String> callback) {
    if (filePath == null || filename == null || callback == null) {
      CobbleUtils.LOGGER.error("Invalid input: filePath, filename, or callback is null.");
      return CompletableFuture.completedFuture(false);
    }

    // Optional: Add timeout handling
    return CompletableFuture.supplyAsync(() -> {
      Path path = Paths.get(new File("").getAbsolutePath() + filePath, filename);
      File file = path.toFile();

      if (!file.exists()) {
        CobbleUtils.LOGGER.warn("File does not exist: " + file.getPath());
        return false;
      }

      try {
        String content = Files.readString(path, charset);
        callback.accept(content);
        return true;
      } catch (IOException e) {
        CobbleUtils.LOGGER.error("Error reading file: " + file.getPath() + ". " + e);
        return false;
      }
    }, IO_EXECUTOR);
  }

  public static boolean readFileSync(File file, Consumer<String> callback) {
    if (!file.exists() || !file.isFile()) {
      return false;
    }
    try {
      // Always try UTF-8 first
      List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
      String content = String.join("\n", lines);
      callback.accept(content);
      return true;
    } catch (MalformedInputException mie) {
      System.err.println("[CobbleUtils] File is not UTF-8 encoded, trying ISO-8859-1: " + file.getName());
      try {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
        String content = String.join("\n", lines);
        callback.accept(content);
        return true;
      } catch (IOException ex) {
        ex.printStackTrace();
        return false;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }


  public static String readFileSync(File file) throws IOException {
    if (!file.exists() || !file.isFile()) {
      throw new IllegalArgumentException("El archivo no existe o no es válido: " + file.getPath());
    }
    return Files.readString(file.toPath(), charset);
  }

  public static CompletableFuture<Boolean> writeFileAsync(File file, String content) {
    if (file == null || content == null) {
      CobbleUtils.LOGGER.error("Invalid input: file or content is null.");
      return CompletableFuture.completedFuture(false);
    }

    return CompletableFuture.supplyAsync(() -> {
      try {
        Files.writeString(file.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return true;
      } catch (IOException e) {
        CobbleUtils.LOGGER.error("Error al escribir el archivo: " + file.getPath() + ". " + e);
        return false;
      }
    }, IO_EXECUTOR);
  }

  /**
   * @deprecated Use {@link #broadcastMessage(Text)} instead.
   */
  @Deprecated
  public static void broadcastMessage(String message) {
    if (CobbleUtils.config.isRedisMessaging()) {
      RedisManager.sendMessage(message);
    } else {
      MinecraftServer server = CobbleUtils.server;
      ArrayList<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
      for (ServerPlayerEntity pl : players) {
        pl.sendMessage(AdventureTranslator.toNative(message));
      }
    }
  }

  public static void broadcastMessage(Text message) {
    if (CobbleUtils.config.isRedisMessaging()) {
      // Convert Text to String and send via Redis
      String textAsString = message.getString();
      RedisManager.sendMessage(textAsString);
    } else {
      MinecraftServer server = CobbleUtils.server;
      ArrayList<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
      for (ServerPlayerEntity pl : players) {
        pl.sendMessage(message);
      }
    }
  }

  public static void broadcastMessage(String message, String prefix) {
    if (CobbleUtils.config.isRedisMessaging()) {
      RedisManager.sendMessage(message, prefix);
    } else {
      var text = AdventureTranslator.toNative(message, prefix);
      MinecraftServer server = CobbleUtils.server;
      ArrayList<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
      for (ServerPlayerEntity pl : players) {
        pl.sendMessage(text);
      }
    }
  }

  public static ItemStack parseItemId(String id) {
    return parseItemId(id, 1);
  }

  public static ItemStack parseItemId(String id, int amount) {
    return new ItemStack(Registries.ITEM.get(Identifier.of(id)), amount);
  }

  public static File getAbsolutePath(String directoryPath) {
    return new File(Paths.get(new File("").getAbsolutePath()) + directoryPath);
  }

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
      CobbleUtils.LOGGER.info("Directory " + directory.getPath() + " does not exist or is not a directory.");
    }
    return fileList;
  }

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
      CobbleUtils.LOGGER.info("Directory " + directoryPath + " does not exist or is not a directory.");
    }
  }

  public static ItemStack parseItemModel(ItemModel itemModel, int amount) {
    String item = itemModel.getItem();
    String nbt = itemModel.getNbt();

    // Split item string to handle NBT if present
    String[] nbtSplit = item.split("#");
    if (nbtSplit.length > 1) {
      item = nbtSplit[0];
      nbt = nbtSplit[1];
    }

    // Handle custom item format
    if (item.startsWith("item:")) {
      item = item.replace("item:", "");
      String[] split = item.split(":");
      item = split[1] + ":" + split[2];
      amount = Integer.parseInt(split[0]);
    }

    // Parse the item ID and create the ItemStack
    ItemStack itemStack = parseItemId(item, amount);

    // Apply additional NBT and properties to the ItemStack
    itemStack = addThingsItemStack(itemStack, itemModel, nbt);

    return itemStack;
  }

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

  public static void createDirectoryIfNeeded(String directoryPath) {
    File directory = getAbsolutePath(directoryPath);
    if (!directory.exists()) {
      if (directory.mkdirs()) {
        CobbleUtils.LOGGER.info("Created directory: " + directoryPath);
      } else {
        CobbleUtils.LOGGER.error("Failed to create directory: " + directoryPath);
      }
    }
  }

  public static ItemStack getHead(String replace, int amount) {
    ItemStack itemStack = Items.PLAYER_HEAD.getDefaultStack();
    var profile = new GameProfile(UUID.randomUUID(), replace);
    itemStack.set(DataComponentTypes.PROFILE, new ProfileComponent(profile));
    itemStack.setCount(amount);
    return itemStack;
  }

  public static ItemStack parseItemId(String item, int amount, long customModelData) {
    ItemStack itemStack = parseItemId(item, amount);
    if (customModelData != 0)
      itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent((int) customModelData));
    return itemStack;
  }

  public static StringBuilder replaceStringBuilder(StringBuilder sb, Map<String, String> placeholders) {
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      String placeholder = entry.getKey();
      String value = entry.getValue();
      int index = sb.indexOf(placeholder);
      while (index != -1) {
        sb.replace(index, index + placeholder.length(), value);
        index += value.length(); // Move to the end of the replaced value
        index = sb.indexOf(placeholder, index);
      }
    }
    return sb;
  }
}