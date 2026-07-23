# Changelog

## [Unreleased]

## [1.2.4] - 2026-07-23

### Added

- Added `apiJar` task to build a lightweight API jar (includes only api, models, utils, and adapter packages).
- Added `ConditionApi` (`register`, `check`, `getDefaultConditions`, `getRegisteredTypes`) to handle custom conditions.
- Included existing APIs in the lightweight jar: `PermissionApi`, `RewardsApi`, `EconomyApi`, `BlocksAPI`, `PartyAPI`, and `GuildAPI`.
- Added external placeholder registration in `PokemonUtils` (`registerPlaceholder`, `unregisterPlaceholder`) for external mods.

## [1.2.3] - 2026-07-22

### Optimizations

- Optimized Pokémon blacklist property matching to reduce server load.

## [1.2.2] - 2026-07-22

### Added

- Added `cumulativeLootTable` boolean option to `AdvancedItemChance` (defaults to `true`). When set to `false`, the loot table evaluation stops at the first permission match (top-to-bottom priority), rather than merging all lists.
- Implemented cross-server teleportation command (`TeleportCommand`) and Redis-based location/teleportation synchronizer (`RedisTeleportHandler`).
- Added automatic world synchronization across server instances using Redis messaging.
