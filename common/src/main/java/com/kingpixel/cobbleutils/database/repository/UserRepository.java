package com.kingpixel.cobbleutils.database.repository;

import com.kingpixel.cobbleutils.Model.ItemChance;
import com.kingpixel.cobbleutils.database.users.UserModel;
import com.kingpixel.cobbleutils.database.users.models.Storage;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Repository contract for all user-data operations.
 * <p>
 * This is the <strong>only</strong> API surface that external mods and internal
 * CobbleUtils code should interact with. Implementations may back it with
 * MongoDB, SQL, JSON files, or any other store — callers don't know and
 * don't care.
 * <p>
 * Rules for callers:
 * <ul>
 *   <li>Never import {@code com.mongodb.*}, {@code org.bson.*}, or JDBC types.</li>
 *   <li>Obtain the instance via {@link com.kingpixel.cobbleutils.database.DataBaseFactory#users()}.</li>
 *   <li>Treat {@link UserModel} as a plain value object; persist changes by calling {@link #save(UserModel)}.</li>
 * </ul>
 *
 * @author Carlos Varas Alonso
 */
public interface UserRepository {

  // ─── Read ─────────────────────────────────────────────────────────────────

  /**
   * Returns the user from the in-process cache, or {@code null} if not cached.
   * Does NOT hit the underlying store.
   */
  @Nullable
  UserModel getUser(@NotNull UUID uuid);

  /**
   * Returns the user from cache if present; otherwise loads from the store,
   * caches and returns it. Returns {@code null} if the user does not exist.
   */
  @Nullable
  UserModel findUser(@NotNull UUID uuid);

  /**
   * Loads the user directly from the backing store, bypassing the cache.
   * Use {@link #findUser(UUID)} in most cases.
   */
  @Nullable
  UserModel findUserByUUID(@NotNull UUID uuid);

  /**
   * Looks up the user by Minecraft display name. Resolves via the server UserCache first.
   */
  @Nullable
  UserModel findUserByName(@NotNull String name);

  /** Returns all users stored in the backing store. */
  List<UserModel> getAllUsers();

  /**
   * Returns users whose last login is older than {@code millis} milliseconds ago.
   *
   * @param millis duration threshold in milliseconds
   */
  List<UserModel> getUsersInactiveSince(long millis);

  /** Returns the UUIDs of players currently flagged as online in the backing store. */
  List<UUID> getOnlinePlayers();

  // ─── Write ────────────────────────────────────────────────────────────────

  /**
   * Unconditionally persists {@code user} to the backing store (upsert).
   * Prefer {@link #saveIfDirty(UserModel)} to avoid unnecessary writes.
   */
  void save(@NotNull UserModel user);

  /**
   * Persists the user only if its dirty flag is set.
   * Uses atomic compare-and-set to prevent double-save races between threads.
   *
   * @return {@code true} if the user was saved; {@code false} if skipped.
   */
  boolean saveIfDirty(@NotNull UserModel user);

  /**
   * Evicts {@code uuid} from the in-process cache and, when Redis messaging
   * is enabled, broadcasts the invalidation to sibling servers.
   */
  void invalidateUser(@NotNull UUID uuid);

  // ─── Lifecycle events ─────────────────────────────────────────────────────

  /**
   * Called when a player disconnects. Implementations should flush any pending
   * state and mark the user as offline in the store.
   */
  void disconnected(@NotNull ServerPlayerEntity player);

  /**
   * Evicts the user from local state. Alias for {@link #invalidateUser(UUID)}
   * used by event handlers that want a semantically named hook.
   */
  void removeIfNecessary(@NotNull UUID uuid);

  // ─── Storage (pending rewards / offline items) ────────────────────────────

  void addStorage(@NotNull Storage storage, @NotNull UUID playerUUID);

  void addStorage(@NotNull List<Storage> storages, @NotNull UUID playerUUID);

  @Nullable
  Storage removeStorage(@NotNull Storage storage, @NotNull UUID playerUUID);

  /**
   * Removes multiple storage entries in a single backing-store operation when possible.
   */
  void removeStorageBatch(@NotNull List<Storage> storages, @NotNull UUID playerUUID);

  // ─── Rewards ──────────────────────────────────────────────────────────────

  /** Returns {@code true} if the player is eligible to claim {@code itemChance}. */
  boolean isAvailableReward(@NotNull UUID playerUUID, @NotNull ItemChance itemChance);

  /** Overload that accepts an online player entity directly. */
  boolean isAvailableReward(@NotNull ServerPlayerEntity player, @NotNull ItemChance itemChance);

  /**
   * Records that {@code playerUUID} has claimed {@code itemChance} and persists
   * the change to the backing store.
   */
  void claimReward(@NotNull UUID playerUUID, @NotNull ItemChance itemChance);

  /** Overload that accepts an online player entity directly. */
  void claimReward(@NotNull ServerPlayerEntity player, @NotNull ItemChance itemChance);

  /**
   * Claims multiple rewards in a single user fetch + single DB write + single
   * cache invalidation. Prefer this over calling {@link #claimReward} in a loop.
   */
  void claimRewardsBatch(@NotNull UUID playerUUID, @NotNull List<ItemChance> rewards);

  /**
   * Atomically claims rewards AND adds storage entries in a single backing-store
   * operation. Used when an offline player must receive both reward tracking
   * updates and physical items simultaneously.
   */
  void claimRewardsAndAddStorage(@NotNull UUID playerUUID,
                                 @Nullable List<ItemChance> rewards,
                                 @Nullable List<Storage> storages);

  // ─── Utilities ────────────────────────────────────────────────────────────

  /**
   * Returns the live {@link ServerPlayerEntity} if the player is online, or a
   * synthetic offline entity loaded from server data if not. Returns {@code null}
   * if the player cannot be resolved at all.
   */
  @Nullable
  ServerPlayerEntity getPlayerOfflineOrOnline(@NotNull String playerName);

  @Nullable
  ServerPlayerEntity getPlayerOfflineOrOnline(@NotNull UUID playerUUID);
}

