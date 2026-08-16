# LifestealLite

A minimal, lightweight Lifesteal SMP plugin for Paper. Built to keep RAM and
CPU overhead as low as possible:

- **No repeating tasks / schedulers** — everything is purely event-driven.
- **No in-memory caches or maps** — elimination state is stored in each
  player's own PersistentDataContainer (already persisted by the server, so
  it costs nothing extra).
- **3 small classes total** — `LifestealLite` (bootstrap + commands),
  `GameListener` (all game logic), `Items` (item creation).
- No particle spam, no potion effects, no extra abilities — just the core
  loop.

## How it works

- PvP kill: victim loses 1 heart (max health), killer gains 1 heart (capped),
  victim drops a **Heart** item.
- Right-click a **Heart** item to consume it and permanently gain a heart
  (also capped).
- Hit 0 hearts → eliminated. By default this puts you in spectator mode
  (set `ban-on-elimination: true` in config for a real ban instead).
- Craft a **Revive Totem** from Hearts + a Totem of Undying, then right-click
  an eliminated player with it to bring them back at starting hearts.

## Recipes

- **Heart**: 4 Redstone (in a plus shape) + 1 Ghast Tear in the center.
- **Revive Totem**: 4 Hearts (corners) + 1 Totem of Undying (center).

Both recipe shapes and the required Heart count are easy to change in
`LifestealLite.registerRecipes()` / `config.yml` if you want a different cost.

## Commands

| Command | Description |
|---|---|
| `/heart [player] [amount]` | Give Heart items |
| `/revivetotem [player]` | Give a Revive Totem |
| `/hearts [player] [amount]` | Check or set a player's hearts |

Requires `lifesteallite.admin` permission (default: op).

## Building

```bash
mvn clean package
```

Output: `target/LifestealLite.jar`. Requires internet access to pull the
Paper API. This project targets **Paper 26.2**, which needs **JDK 25** to
compile and run (Paper moved off the old `{mc-version}-R0.1-SNAPSHOT` Maven
scheme starting with 26.1 — the `pom.xml` now points at
`io.papermc.paper:paper-api:[26.2.build,)`). If your server runs an older
Paper/Minecraft version, swap that dependency version back to the classic
`{version}-R0.1-SNAPSHOT` format (e.g. `1.20.4-R0.1-SNAPSHOT`) and drop the
compiler properties back to Java 17.

## Installing

Drop the built jar into `plugins/`, restart/reload the server. Config
generates at `plugins/LifestealLite/config.yml`.
