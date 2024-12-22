package com.kingpixel.cobbleutils.util;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.adapters.ElementalTypeAdapter;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.adapters.IntRangeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.features.shops.ShopTransactions;
import com.kingpixel.cobbleutils.features.shops.models.types.ShopActionAdapter;
import com.kingpixel.cobbleutils.features.shops.models.types.ShopType;
import com.kingpixel.cobbleutils.features.shops.models.types.ShopTypeAdapter;
import com.mojang.authlib.GameProfile;
import kotlin.ranges.IntRange;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public abstract class Utils {
  public static final Random RANDOM = new Random();
  private static final Charset charset = StandardCharsets.UTF_8;

  public static Gson newGson() {
    return adapters()
      .setPrettyPrinting()
      .create();
  }

  private static GsonBuilder adapters() {
    return addAdapters(new GsonBuilder()
      .disableHtmlEscaping());
  }

  public static Gson newWithoutSpacingGson() {
    return adapters()
      .create();
  }

  private static GsonBuilder addAdapters(GsonBuilder builder) {
    return builder
      .registerTypeAdapter(ShopType.class, new ShopTypeAdapter())
      .registerTypeAdapter(ShopTransactions.ShopAction.class, new ShopActionAdapter())
      .registerTypeAdapter(ElementalType.class, ElementalTypeAdapter.INSTANCE)
      .registerTypeAdapter(IntRange.class, IntRangeAdapter.INSTANCE)
      .registerTypeAdapter(DateTypeAdapter.class, new DateTypeAdapter());
  }

  public static CompletableFuture<Boolean> writeFileAsync(String filePath, String filename, String data) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    Path path = Paths.get(new File("").getAbsolutePath() + filePath, filename);
    File file = path.toFile();

    if (!Files.exists(path.getParent())) {
      file.getParentFile().mkdirs();
    }

    try (AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
      path,
      StandardOpenOption.WRITE,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING)) {
      ByteBuffer buffer = ByteBuffer.wrap(data.getBytes(charset));

      fileChannel.write(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
        @Override
        public void completed(Integer result, ByteBuffer attachment) {
          attachment.clear();
          try {
            fileChannel.close();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          future.complete(true);
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
          future.complete(writeFileSync(file, data));
        }
      });
    } catch (IOException | SecurityException e) {
      CobbleUtils.LOGGER.fatal("Unable to write file asynchronously, attempting sync write.");
      future.complete(future.complete(false));
    }

    return future;
  }

  public static boolean writeFileSync(File file, String data) {
    try (FileWriter writer = new FileWriter(file, charset)) {
      writer.write(data);
      return true;
    } catch (IOException e) {
      CobbleUtils.LOGGER.fatal("Unable to write to file for " + CobbleUtils.MOD_ID + ". " + e);
      return false;
    }
  }

  public static CompletableFuture<Boolean> readFileAsync(String filePath, String filename,
                                                         Consumer<String> callback) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    ExecutorService executor = Executors.newSingleThreadExecutor();

    Path path = Paths.get(new File("").getAbsolutePath() + filePath, filename);
    File file = path.toFile();

    if (!file.exists()) {
      future.complete(false);
      executor.shutdown();
      return future;
    }

    try (AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ)) {
      ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());

      Future<Integer> readResult = fileChannel.read(buffer, 0);
      readResult.get();
      buffer.flip();

      byte[] bytes = new byte[buffer.remaining()];
      buffer.get(bytes);
      String fileContent = new String(bytes, charset);

      callback.accept(fileContent);

      fileChannel.close();
      executor.shutdown();
      future.complete(true);
    } catch (Exception e) {
      future.complete(readFileSync(file, callback));
      executor.shutdown();
    }

    return future;
  }

  public static boolean readFileSync(File file, Consumer<String> callback) {
    try (Scanner reader = new Scanner(file, charset)) {
      StringBuilder data = new StringBuilder();
      while (reader.hasNextLine()) {
        data.append(reader.nextLine());
      }
      callback.accept(data.toString());
      return true;
    } catch (IOException e) {
      CobbleUtils.LOGGER
        .fatal("Unable to read file " + file.getName() + " for " + CobbleUtils.MOD_ID + "." + e.getMessage());
      return false;
    }
  }

  public static String readFileSync(File file) throws IOException {
    // Verifica si el archivo existe y es un archivo regular
    if (!file.exists() || !file.isFile()) {
      throw new IllegalArgumentException("El archivo no existe o no es un archivo regular: " + file.getPath());
    }
    return Files.readString(Path.of(file.getPath()), charset);
  }

  public static CompletableFuture<Void> writeFileAsync(File file, String content) {
    return CompletableFuture.runAsync(() -> {
      try (FileWriter writer = new FileWriter(file)) {
        writer.write(content);
      } catch (IOException e) {
        throw new RuntimeException("Error al escribir el archivo: " + file.getPath(), e);
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
    ItemStack itemStack = new ItemStack(Registries.ITEM.get(Identifier.of(id)), amount);
    return itemStack;
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

  public static UUID getUUID(String playername) {
    return CobbleUtils.server.getPlayerManager().getPlayer(playername).getUuid();
  }

  public static Pokemon createPokemonParse(String pokemonName) {
    return PokemonProperties.Companion.parse(pokemonName).create();
  }

  public static ItemStack parseItemModel(ItemModel itemModel, int amount) {
    ItemStack itemStack = parseItemId(itemModel.getItem(), amount);
    return addThingsItemStack(itemStack, itemModel);
  }

  public static ItemStack addThingsItemStack(ItemStack itemStack, ItemModel itemModel) {

    /*if (itemModel.getNbt() != null && !itemModel.getNbt().isEmpty()) {
      try {
        itemStack.set(DataComponentTypes.CUSTOM_DATA, NbtHelper.fromNbtProviderString(itemModel.getNbt()));
      } catch (CommandSyntaxException ignored) {
      }
    }*/

    itemStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNativeWithOutPrefix(
      itemModel.getDisplayname() != null ? itemModel.getDisplayname() : "Please set a displayname for this item"));

    if (itemModel.getCustomModelData() != 0)
      itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent((int) itemModel.getCustomModelData()));

    if (itemModel.getLore() != null && !itemModel.getLore().isEmpty()) {
      itemStack.set(DataComponentTypes.LORE,
        LoreComponent.DEFAULT.with(AdventureTranslator.toNative(itemModel.getLore().toString())));
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