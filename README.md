# LDTD: Limitless Development: Tower Defense

A survival **tower defense** mod for **Minecraft 1.12.2 (Forge)**.

Claim your base or a village with a **Hearth**. Have builders raise towers and walls from
blueprints, block by block, with materials from your stockpile. Hold the line when raids come at
night. Then take the fight to the enemy outposts that send them.

> **Status: early development.** Nothing playable yet. The mod is being built in phases. Expect
> nothing to be stable until the first full release.

## Planned features

- **Hearth territories:** a core block claims an area and holds its stockpile and raid state.
- **Builders:** helper mobs carry materials and construct (and repair) your defenses in the world.
- **Towers with real roles:** single-target, splash and slowing towers to start, plus walls and
  gates. All of it is real blocks that enemies have to get through.
- **Raids:** night raids by default, or ring the Hearth to call one early for a bonus. Raids are
  built from a growing budget, and you get a preview of what is coming.
- **An original enemy faction** that routes to your Hearth and sieges walls when it is blocked.
  Enemies can only damage blocks inside a Hearth's territory.
- **Outposts and a boss:** enemy outposts generate in the world, send the raids and can be
  assaulted.
- **Data-driven:** towers, enemies, waves and upgrades are JSON under `config/ldtd/`, so they can
  be tuned without a rebuild.

## Commands

- `/randomspawn` (or `/rs`): teleports you to a random safe spot, for finding fresh ground for a
  Hearth. On a world that is already explored, it only picks chunks that have been generated. On a
  new world, smaller than 1000 blocks from spawn, it may pick anywhere within that radius. Open to all
  players with a 5-minute cooldown. Operators skip the cooldown and can send others
  (`/rs <player>`). All of it is configurable under `randomSpawn` in `config/ldtd.cfg`.
- `/hearth trust <player>`, `/hearth untrust <player>`, `/hearth trusted`: who may build in your
  territory and use your stockpile. Run them while standing in your territory.
- `/ldtd ...`: operator tools for Hearths, raids and testing.

## Territory protection

Inside a claimed territory, players the owner hasn't trusted can't break or place blocks, empty
buckets, light fires or open the stockpile, and explosions break no blocks. Doors, chests,
levers, PvP and animals work as normal. Each rule can be switched off under `protection` in
`config/ldtd.cfg`. None of this stops the Gloam: their siege is the point.

## Building

Needs a JDK 17-22 for Gradle (21 recommended). The mod itself targets Java 8.

```bash
./gradlew build      # compile, unit tests, jar in build/libs/
./gradlew runClient  # dev client
./gradlew runServer  # dev dedicated server
```

## License

[MIT](LICENSE). Third-party notices are in [CREDITS.md](CREDITS.md).

All of LDTD's textures, models, sounds and text are original, or are used under a compatible
license and recorded in [docs/ASSET_PROVENANCE.md](docs/ASSET_PROVENANCE.md).

---

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
