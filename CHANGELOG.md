# Changelog

## [Unreleased]

## [1.2.4] - 2026-07-23

### Added

- Added `apiJar` Gradle task to build and package a lightweight API JAR containing only the API, Model, Util, and Adapter classes. This packages:
  - `ConditionApi` (`register`, `check`, `getDefaultConditions`, `getRegisteredTypes`) to easily manage, verify, and retrieve custom conditions.
  - `PermissionApi` to check LuckPerms permissions and commands.
  - `RewardsApi` to query loot tables and items.
  - `EconomyApi` to handle economy integration.
  - `BlocksAPI` to interact with custom block features.
  - `PartyAPI` and `GuildAPI` for group/guild management integrations.
- Added external placeholder registration API in `PokemonUtils` (`registerPlaceholder` and `unregisterPlaceholder`) to allow external mods to add dynamic placeholders to Pokémon lore and text.

## [1.2.3] - 2026-07-22

### Optimizations

- Optimized Pokémon blacklist property matching to reduce server load.

## [1.2.2] - 2026-07-22

### Added

- Added `cumulativeLootTable` boolean option to `AdvancedItemChance` (defaults to `true`). When set to `false`, the loot table evaluation stops at the first permission match (top-to-bottom priority), rather than merging all lists.
- Implemented cross-server teleportation command (`TeleportCommand`) and Redis-based location/teleportation synchronizer (`RedisTeleportHandler`).
- Added automatic world synchronization across server instances using Redis messaging.
