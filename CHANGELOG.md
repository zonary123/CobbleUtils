# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.2] - 2026-07-22

### Added
- Added `cumulativeLootTable` boolean option to `AdvancedItemChance` (defaults to `true`). When set to `false`, the loot table evaluation stops at the first permission match (top-to-bottom priority), rather than merging all lists.
- Implemented `LinkedHashMap` for the default `lootTable` to preserve layout ordering from configuration files.

### Changed
- Upgraded project version to `1.2.2` in `gradle.properties`.
