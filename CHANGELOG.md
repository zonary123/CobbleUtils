# Changelog

## [Unreleased]

### Fixed

- **MongoDB Connection Pool & Idle Socket Handling**: Configured explicit `maxConnectionIdleTime` (60s), `maxConnectionLifeTime` (30m), socket timeouts (10s connect / 30s read), and automatic `retryWrites` in `MongoDBManager` to eliminate `MongoSocketReadException: Prematurely reached end of stream` errors caused by intermediate firewalls, NAT, or server-side idle connection drops.

## [1.2.8] - 2026-08-24

### Improved

- **CobbleDollars Economy Integration**: Improved stability, reliability, and precision when handling player balances, deposits, and withdrawals with CobbleDollars.

### Fixed

- **Economy Startup Logs**: Fixed unnecessary error warnings appearing in the server console when optional economy mods are not installed.
- **Transaction Safety**: Added extra safety checks to prevent unexpected errors when transferring money or checking balances if players are offline.

## [1.2.7] - 2026-08-21

### Added

- **Unified Placeholders Engine**: Added universal placeholder API (`PlaceholderApi`, `PlaceholdersUtils`, `CobblePlaceholderContext`, `PlaceholderValueConverter`) with equitable, bidirectional support for **MiniPlaceholders** (Adventure / MiniMessage) and **PlaceholderAPI** (PB4 / Minecraft Text), along with an internal in-memory fallback replacement engine.
- **Mutual Context Object**: Added `CobblePlaceholderContext` encapsulating players, audiences, relational targets, objects (`Pokemon`, `ItemStack`, etc.), servers, and typed argument helpers (`getArgInt`, `getArgBool`, `targetAs`).
- **Defensive Error Handling**: Fully protected all placeholder evaluations and class lookups with isolated `try-catch` blocks and dedicated logging to guarantee that runtime or third-party placeholder errors never crash the server.
- **Unit Test Coverage**: Added comprehensive JUnit test suites for placeholder context, safe object casting, cross-engine parsing, and unregistration.

## [1.2.6] - 2026-08-17

### Fixed

- Standardized robust `try-catch` exception handling catching `Throwable` across all Mixin injection handlers to prevent unhandled mixin errors from crashing the server tick loop.
- Replaced raw `e.printStackTrace()` calls across all mixins and `EventChannel` with structured `CobbleUtils.LOGGER_RAW.error(...)` logs tagged with class and method names for clear bug reporting.

## [1.2.5] - 2026-08-13

### Fixed

- Fixed `ClassCastException` and item drop cancellation in `SweetBerriesMixin`.
- Added exception isolation to `EventChannel.emit` so failing event listeners cannot break game logic or other listeners.

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
