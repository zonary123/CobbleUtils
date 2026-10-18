# 📖 CobbleUtils Complete Wiki & Guide

Welcome to the **CobbleUtils** documentation! CobbleUtils is the core foundational library powering zonary123's Cobblemon mod ecosystem.

---

## 📌 Table of Contents
1. [Overview](#-overview)
2. [Commands & Permissions](#-commands--permissions)
3. [Features](#-features)
   - [Virtual Pokémon Storage / PC](#virtual-pokémon-storage--pc)
   - [Economy Integration](#economy-integration)
   - [Spawn Rates & Rarity Engine](#spawn-rates--rarity-engine)
   - [Automatic Mod Wiki Extraction](#automatic-mod-wiki-extraction)
4. [Configuration](#-configuration)
5. [Developer API](#-developer-api)

---

## 🌟 Overview

CobbleUtils provides essential cross-mod infrastructure for Minecraft Fabric and NeoForge Cobblemon servers:
- **Unified Economy Abstraction**: Seamless support for Impactor, BlanketEconomy, CobbleDollars, SDMEconomy, PebbleEconomy, and Vault.
- **Cross-Server Synchronization**: Multi-server player teleportation, state sharing, and world syncing via Redis.
- **Database Adapters**: Built-in support for MongoDB, MySQL, and SQLite.
- **In-Game Editor Menus & UIs**: Visual editors for Pokémon, blacklists, weighted reward chances, and schedules.

---

## ⌨️ Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/cobbleutils reload` | `cobbleutils.admin` | Reloads all configuration files and database connections. |
| `/cobbleutils pokerenameticket <player>` | `cobbleutils.admin` | Gives a Pokémon rename ticket item to a player. |
| `/cobbleutils shinytoken <player> <amount>` | `cobbleutils.admin` | Grants shiny tokens to a player. |
| `/cobbleutils wiki` | None | Displays the link to the official documentation. |
| `/pokeshout` | `cobbleutils.pokeshout` | Shouts Pokémon information in global chat. |
| `/pokeshoutme` | `cobbleutils.pokeshout` | Displays your Pokémon information to yourself. |
| `/storage` / `/pc` | `cobbleutils.storage` | Opens the portable virtual PC/storage interface. |

---

## ⚙️ Features

### Virtual Pokémon Storage / PC
Players can access their Pokémon boxes anywhere using `/pc` or `/storage` (configurable in `config.json`). Admins can configure slot limits, permissions, and visual display layouts.

### Economy Integration
CobbleUtils bridges external economy mods automatically. Dependent mods can withdraw, deposit, or check balances across any supported economy without individual integrations.

### Spawn Rates & Rarity Engine
Calculates true spawn weights and rarity tiers dynamically across all registered Cobblemon world spawn pools, including intelligent evolutionary line fallbacks for evolved Pokémon.

### Automatic Mod Wiki Extraction
Mods powered by CobbleUtils automatically export their bundled `WIKI.md` file directly into their server configuration folder (`config/<modid>/WIKI.md`). Server administrators always have the latest documentation available locally without searching for external links.

---

## 🛠️ Developer API

Add the CobbleUtils dependency to your `build.gradle` to access shared utilities:

```java
// Extract bundled WIKI.md documentation to the mod folder
UtilsFile.extractWiki(MyMod.class, MyMod.getPath());

// Read a resource from your mod's jar as a string
String guide = UtilsFile.readResource(MyMod.class, "guide.txt");

// Query spawn rarity for any species or Pokemon
float rarity = CobbleUtils.spawnRates.getRarity(pokemon);
```
