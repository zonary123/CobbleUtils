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
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.adapter.ItemStackAdapter;
import com.kingpixel.cobbleutils.adapter.PokemonAdapter;
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
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class Utils {
  public static final Random RANDOM = new Random();
  private static final Charset charset = StandardCharsets.UTF_8;
  private static Gson gsonPretty = null;
  private static Gson gsonnotPretty = null;


  public static Gson newGson() {
    if (gsonPretty == null) {
      gsonPretty = adapters()
        .setPrettyPrinting()
        .create();
    }
    return gsonPretty;
  }

  private static GsonBuilder adapters() {
    return addAdapters(new GsonBuilder()
      .disableHtmlEscaping());
  }

  public static Gson newWithoutSpacingGson() {
    if (gsonnotPretty == null) {
      gsonnotPretty = adapters()
        .create();
    }
    return gsonnotPretty;
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
      .registerTypeAdapter(ItemStack.class, ItemStackAdapter.INSTANCE);
  }

  public static CompletableFuture<Boolean> writeFileAsync(String filePath, String filename, String data) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    Path path = Paths.get(new File("").getAbsolutePath() + filePath, filename);
    File file = path.toFile();

    try {
      if (!Files.exists(path.getParent())) {
        Files.createDirectories(path.getParent());
      }

      AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
        path,
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      );

      ByteBuffer buffer = ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8));

      fileChannel.write(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
        @Override
        public void completed(Integer result, ByteBuffer attachment) {
          try {
            fileChannel.close();
            future.complete(true);
          } catch (IOException e) {
            future.completeExceptionally(e);
          }
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
          CobbleUtils.LOGGER.error("Async file write failed, writing synchronously.");
          exc.printStackTrace();
          boolean syncSuccess = writeFileSync(file, data); // Asegúrate de que writeFileSync() devuelve un boolean válido
          future.complete(syncSuccess);
        }
      });

    } catch (IOException | SecurityException e) {
      CobbleUtils.LOGGER.fatal("Unable to write file asynchronously, attempting sync write.");
      e.printStackTrace();
      boolean syncSuccess = writeFileSync(file, data);
      future.complete(syncSuccess);
    }

    return future;
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
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    Path path = Paths.get(new File("").getAbsolutePath() + filePath, filename);
    File file = path.toFile();

    if (!file.exists()) {
      future.complete(false);
      return future;
    }

    try {
      AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
      ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());

      fileChannel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
        @Override
        public void completed(Integer result, ByteBuffer attachment) {
          attachment.flip();
          byte[] bytes = new byte[attachment.remaining()];
          attachment.get(bytes);
          String fileContent = new String(bytes, StandardCharsets.UTF_8);

          callback.accept(fileContent);

          try {
            fileChannel.close();
          } catch (IOException e) {
            future.completeExceptionally(e);
            return;
          }

          future.complete(true);
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
          CobbleUtils.LOGGER.error("Failed to read file asynchronously, attempting sync read.");
          exc.printStackTrace();
          boolean syncSuccess = readFileSync(file, callback);
          future.complete(syncSuccess);
          try {
            fileChannel.close();
          } catch (IOException e) {
            future.completeExceptionally(e);
          }
        }
      });

    } catch (IOException e) {
      CobbleUtils.LOGGER.fatal("Unable to read file asynchronously, attempting sync read.");
      e.printStackTrace();
      boolean syncSuccess = readFileSync(file, callback);
      future.complete(syncSuccess);
    }

    return future;
  }


  public static boolean readFileSync(File file, Consumer<String> callback) {
    if (!file.exists() || !file.isFile()) {
      System.err.println("El archivo no existe o no es válido: " + file.getPath());
      return false;
    }
    try {
      List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
      String content = String.join("\n", lines); // Mantiene los saltos de línea
      callback.accept(content);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  public static String readFileSync(File file) throws IOException {
    if (!file.exists() || !file.isFile()) {
      throw new IllegalArgumentException("El archivo no existe o no es válido: " + file.getPath());
    }
    return Files.readString(file.toPath(), StandardCharsets.UTF_8);
  }

  public static CompletableFuture<Boolean> writeFileAsync(File file, String content) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Files.writeString(file.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return true; // Indica que se escribió con éxito
      } catch (IOException e) {
        CobbleUtils.LOGGER.error("Error al escribir el archivo: " + file.getPath());
        e.printStackTrace();
        return false; // Retorna false si hubo un error
      }
    });
  }


  public static void broadcastMessage(String message) {
    MinecraftServer server = CobbleUtils.server;
    ArrayList<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
    for (ServerPlayerEntity pl : players) {
      pl.sendMessage(AdventureTranslator.toNative(message));
    }
  }

  public static void broadcastMessage(Text message) {
    MinecraftServer server = CobbleUtils.server;
    ArrayList<ServerPlayerEntity> players = new ArrayList<>(server.getPlayerManager().getPlayerList());
    for (ServerPlayerEntity pl : players) {
      pl.sendMessage(message);
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


}