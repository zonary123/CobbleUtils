# Changelog

## [1.2.2] - 2026-07-22

### Added

- Added `cumulativeLootTable` boolean option to `AdvancedItemChance` (defaults to `true`). When set to `false`, the loot table evaluation stops at the first permission match (top-to-bottom priority), rather than merging all lists.
- Implemented cross-server teleportation command (`TeleportCommand`) and Redis-based location/teleportation synchronizer (`RedisTeleportHandler`).
- Added automatic world synchronization across server instances using Redis messaging.
