# 🧩 CobbleUtils — Comprehensive Architectural Manual & Developer Reference

---

## 📑 Table of Contents
1. [Executive Overview](#-executive-overview)
2. [Core Architecture & Module Subsystems](#-core-architecture--module-subsystems)
   - [Unified Economy Abstraction Layer (`EconomyApi`)](#unified-economy-abstraction-layer-economyapi)
   - [Universal Reward & Probability Engine (`RewardsApi` & `ItemChance`)](#universal-reward--probability-engine-rewardsapi--itemchance)
   - [Asynchronous Concurrency Pipeline (`AsyncContext`)](#asynchronous-concurrency-pipeline-asynccontext)
   - [Multi-Database Drivers (MongoDB, Redis, SQL)](#multi-database-drivers-mongodb-redis-sql)
   - [Interactive GUI Framework (GooeyLibs2 Bridge)](#interactive-gui-framework-gooeylibs2-bridge)
   - [Placeholder Resolution Engine](#placeholder-resolution-engine)
   - [Resource & Documentation Extractor (`extractWiki`)](#resource--documentation-extractor-extractwiki)
3. [Command & Subcommand Reference](#-command--subcommand-reference)
4. [Permissions & Privilege Hierarchy](#-permissions--privilege-hierarchy)
5. [In-Game Features & Portable Storage / PC](#-in-game-features--portable-storage--pc)
6. [Developer API Handbook & Code Recipes](#-developer-api-handbook--code-recipes)
7. [Configuration Reference (`config.json`)](#-configuration-reference-configjson)
8. [Enterprise Production Templates](#-enterprise-production-templates)

---

## 📌 Executive Overview

**CobbleUtils** is the core foundational library, runtime framework, and utility suite that powers the entire **Cobblemon** modular ecosystem created by **zonary123**. Designed to eliminate code duplication across 25+ independent plugins and mods, CobbleUtils delivers high-performance abstractions for currency transactions, weighted probability tables, multi-server Redis pub/sub routing, distributed MongoDB document persistence, relational SQL connection pooling, responsive inventory GUIs, and placeholder evaluation.

By centralizing cross-cutting concerns into a unified, battle-tested API, CobbleUtils ensures that dependent mods (such as UltraQuests, UltraGTS, UltraGuild, UltraRaids, and UltraShop) operate with maximum stability, consistent performance, and thread safety.

### Key Capabilities
- **Universal Multi-Economy Engine**: Unified bridge supporting 8+ economy providers (Impactor, Vault, CobbleDollars, PebbleEconomy, BlanketEconomy, SDMEconomy, UltraEEconomy, and custom token balances).
- **Flexible Rewards & Drops**: Advanced `ItemChance` parser supporting quoted parameters, chance percentages, Pokémon injections, economy payouts, and console commands.
- **Off-Thread Async Pipeline**: Dedicated worker pools (`AsyncContext` and `UtilsAsync`) isolate expensive database and file operations from Minecraft's main tick loop.
- **Enterprise Multi-Database Clustering**: Built-in connection managers for MongoDB (with BSON codecs for UUIDs and BigDecimals), Redis (with cross-server message handlers), and SQL (MySQL, MariaDB, SQLite via HikariCP).
- **GooeyLibs2 GUI Builder Utilities**: Standardized templates (`ChestTemplate`, `PanelsConfig`, `Rectangle`, `ItemModel`) delivering seamless pagination and inventory protection.
- **Automated Documentation Extractor**: Dynamically extracts bundled `WIKI.md` documentation from the mod JAR into the server's configuration directory on startup (`UtilsFile.extractWiki()`).

---

## 🏗️ Core Architecture & Module Subsystems

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CobbleUtils Core Framework                      │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
     ┌───────────────────┬───────────┴───────────┬───────────────────┐
     ▼                   ▼                       ▼                   ▼
┌─────────────┐   ┌─────────────┐         ┌─────────────┐     ┌─────────────┐
│ EconomyApi  │   │ RewardsApi  │         │  Database   │     │ GUI Engine  │
│ 8+ Currency │   │ ItemChance  │         │ Mongo/Redis │     │  GooeyLibs  │
│ Providers   │   │ Loot Tables │         │ SQL Hikari  │     │  Templates  │
└─────────────┘   └─────────────┘         └─────────────┘     └─────────────┘
     │                   │                       │                   │
     └───────────────────┼───────────────────────┼───────────────────┘
                         ▼                       ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Shared Utility Subsystems                       │
│ • AsyncContext   • PlaceholdersUtils   • SpawnRates   • extractWiki()  │
└────────────────────────────────────────────────────────────────────────┘
```

### Unified Economy Abstraction Layer (`EconomyApi`)
CobbleUtils decouples economy calls from specific mod implementations:
- Detects available server economies automatically at startup.
- Supports fractional `BigDecimal` transactions, currency formatting, and balance queries.
- Developers execute transactions using a single static line:
  ```java
  EconomyApi.withdraw(player, new BigDecimal("1500"), "impactor", "");
  ```

### Universal Reward & Probability Engine (`RewardsApi` & `ItemChance`)
The `ItemChance` serialization system allows administrators to define complex, weighted rewards inside JSON files:
- **Item Rewards**: `"minecraft:diamond 64"` or `"cobblemon:rare_candy 5"`
- **Quoted Economy Rewards**: `"economy 500 \"server currency\""`
- **Pokémon Injections**: Spawns or gives Pokémon directly to party or storage box.
- **Command Rewards**: Dispatches console commands with placeholder resolution (`"command:lp user %player% parent add vip"`).

### Asynchronous Concurrency Pipeline (`AsyncContext`)
- Heavy tasks (file I/O, network sockets, SQL queries, MongoDB aggregations) are dispatched to dedicated thread pools.
- Prevents server frame drops and tick stalls during intensive data operations.

### Multi-Database Drivers (MongoDB, Redis, SQL)
- **MongoDB**: Features specialized BSON codecs (`UUIDCodec`, `BigDecimalCodec`, `InstantCodec`) for clean JSON/BSON interop.
- **Redis**: Includes pre-built pub/sub message handlers for cross-server player teleports, network broadcasts, and inventory sync.
- **SQL Manager**: Manages HikariCP connection pools for SQLite, MySQL, and MariaDB.

### Interactive GUI Framework (GooeyLibs2 Bridge)
Provides standardized helper classes for creating responsive inventory menus:
- **`ChestTemplate`**: Auto-sizes inventory layouts (1 to 6 rows).
- **`Rectangle`**: Defines slot boundaries for paginated product or list rendering.
- **`PanelsConfig`**: Configures decorative glass borders and background fillers.

### Placeholder Resolution Engine
Integrates natively with **MiniPlaceholders**, **PB4**, and **PlaceholderAPI** (via bridge):
- Provides contextual placeholder evaluation for player stats, Cobblemon party summaries, and server variables.

### Resource & Documentation Extractor (`extractWiki`)
Every mod in the ecosystem utilizes `UtilsFile.extractWiki(Class<?> mainClass, Path destination)` on initialization:
- Checks if the destination `WIKI.md` exists or requires updating.
- Reads the bundled `WIKI.md` resource directly from inside the `.jar` file.
- Writes the markdown manual directly into the server's `config/<modid>/` folder, ensuring administrators always have complete local documentation.

---

## ⌨️ Command & Subcommand Reference

### Player Commands
| Command | Arguments | Permission Node | Description |
| :--- | :--- | :--- | :--- |
| `/pokeshout` | `[slot]` | `cobbleutils.pokeshout` | Shouts Pokémon stats and IV breakdown in global chat. |
| `/pokeshoutme` | `[slot]` | `cobbleutils.pokeshout` | Displays your Pokémon stats privately to yourself. |
| `/storage` / `/pc` | *None* | `cobbleutils.storage` | Opens the portable virtual PC storage boxes anywhere. |
| `/cobbleutils wiki` | *None* | *None* | Displays links to official documentation and guides. |

### Administrator Commands (`/cobbleutils`)
| Command | Arguments | Permission Node | Description |
| :--- | :--- | :--- | :--- |
| `/cobbleutils reload` | *None* | `cobbleutils.admin` | Hot-reloads all configurations and database pools. |
| `/cobbleutils pokerenameticket` | `<player>` | `cobbleutils.admin` | Gives a Pokémon rename ticket item to a player. |
| `/cobbleutils shinytoken` | `<player> <amount>`| `cobbleutils.admin` | Grants shiny tokens for Pokémon transformations. |

---

## 🛡️ Permissions & Privilege Hierarchy

```
cobbleutils.user                 # Base player permission
├── cobbleutils.pokeshout        # Access to `/pokeshout` and `/pokeshoutme`
└── cobbleutils.storage          # Access to portable `/pc` and `/storage`

cobbleutils.admin                # Administrative access to reload and item grant commands
```

---

## 📦 In-Game Features & Portable Storage / PC

- **Portable PC (`/pc` or `/storage`)**: Allows players to open their Cobblemon PC boxes anywhere in the world without placing physical PC blocks.
- **Shiny Tokens**: Consumable items that allow players to convert a selected party Pokémon into its Shiny variant.
- **Rename Tickets**: Specialized tokens allowing players to rename Pokémon while bypassing standard naming restrictions.

---

## 🛠️ Developer API Handbook & Code Recipes

### 1. Extracting Bundled Documentation
```java
// Inside your mod's onInitialize():
UtilsFile.extractWiki(MyMod.class, MyMod.getPath());
```

### 2. Performing Economy Transactions
```java
// Checking balance:
BigDecimal balance = EconomyApi.getBalance(player, "impactor", "");

// Withdrawing funds:
EconomyResponse response = EconomyApi.withdraw(player, new BigDecimal("500"), "vault", "");
if (response.isSuccess()) {
    PlayerUtils.sendMessage(player, "Purchase successful!", "Shop", TypeMessage.CHAT);
}
```

### 3. Granting Weighted Rewards
```java
ItemChance reward = new ItemChance("minecraft:diamond", 10, 50.0); // 50% chance
reward.giveReward(player);
```

### 4. Querying Dynamic Spawn Rarity
```java
float rarity = CobbleUtils.spawnRates.getRarity(pokemon);
```

---

## ⚙️ Configuration Reference (`config.json`)

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `debug` | `Boolean` | `false` | Enables verbose debug logging for database queries and economy transactions. |
| `lang` | `String` | `"en"` | Language file identifier. |
| `storageCommandEnabled` | `Boolean` | `true` | Enables portable `/pc` and `/storage` command. |
| `pokeshoutCooldown` | `Integer` | `10` | Cooldown in seconds between `/pokeshout` broadcasts. |

---

## 📋 Enterprise Production Templates

### Production `config.json` Template
```json
{
  "debug": false,
  "lang": "en",
  "storageCommandEnabled": true,
  "pokeshoutCooldown": 15,
  "database": {
    "type": "MONGODB",
    "database": "cobbleutils",
    "url": "mongodb://clusterUser:ClusterPass123@10.0.0.5:27017/cobbleutils?authSource=admin"
  },
  "redis": {
    "host": "10.0.0.6",
    "port": 6379,
    "password": "SecureRedisPassword123",
    "channel": "cobbleutils_global_bus"
  }
}
```
