# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Purpose

LDTD ("Limitless Development: Tower Defense") is a **Minecraft 1.12.2 Forge mod** (mod id: `ldtd`)
that adds a **survival tower-defense system**. A Hearth block claims a territory. Builder mobs
construct towers and walls from blueprints using stockpiled materials. Raids from an original enemy
faction come at night (or when called early). World-generated enemy outposts send the raids and
can be assaulted, leading to a boss.

Background research is in `docs/research/tower-defense-research.md`. **Names come from
`docs/GLOSSARY.md`** (add a name there, clash-checked, before using it), and the look comes
from `docs/design/ART_DIRECTION.md`.

## This is a public, MIT-licensed release: originality rules

Every name, asset and line of code must be original or compatibly licensed. In short:

- **No Mojang/Microsoft assets in the jar.** Don't copy vanilla textures, sounds, models or lang
  text. Referencing vanilla resources at runtime is fine.
- **No Minecraft Legends names or designs.** That includes allay, piglin, prismarine-as-currency
  and its named structures or heroes, and look-alikes of its units. The same goes for other TD
  franchises (Bloons, Kingdom Rush, Orcs Must Die and others). Generic genre terms like "archer
  tower" or "wave" are fine.
- **Clean-room code.** Don't paste from other mods unless the license is MIT-compatible and it's
  credited. Third-party source kept for study goes in `example-source/` (gitignored), for ideas
  only.
- **Every asset gets a row in `docs/ASSET_PROVENANCE.md`** in the same commit that adds it. Allowed:
  original, script-generated, CC0, CC-BY (credited in `CREDITS.md`), and AI-generated only if
  disclosed per `docs/design/ART_DIRECTION.md`. Not allowed: NC, ND, SA, unknown origin.
- **New dependencies:** check the license and record it in `CREDITS.md`. Prefer soft dependencies.
- Don't put "Minecraft" in the mod's name or logo. Keep the Mojang disclaimer in the README.

## Build Commands

Set `JAVA_HOME` to a **JDK 17–22** install before each `./gradlew` invocation. **21 is
recommended** and is what CI uses. The mod code targets **Java 8** via Jabel regardless.

```bash
JAVA_HOME="..." ./gradlew build      # compile + unit tests + jar
JAVA_HOME="..." ./gradlew test       # unit tests only (pure JVM)
JAVA_HOME="..." ./gradlew runClient  # dev client
JAVA_HOME="..." ./gradlew runServer  # dev dedicated server
```

Don't rebuild while a `runClient` is live. `build.gradle` is the upstream GTCEu buildscript with
two deliberate edits (the RFG 1.4.7 pin and the lwjgl3ify NOTE). **Never run
`updateBuildScript`**, because it overwrites both. Project build logic goes in `addon.gradle`.

## Rules that are load-bearing

1. **Common code must never reach client-only classes.** Renderers, HUD and ghost previews are
   reached only through `LdtdClientProxy`. A stray `net.minecraft.client` import in common code
   compiles and then fails on a dedicated server, which is what the CI server smoke test catches.
2. **The server is authoritative.** Hearth, raid, routing, tower targeting and builder logic run on
   the server. The client only renders.
3. **Simulation cores stay Minecraft-free** so they are unit-testable on a bare JVM: raid budget
   generation, routing over an abstract grid, balance maths. They live in `ldtd.sim`, which must
   never import a `net.minecraft` or `net.minecraftforge` type. Convert at the boundary.
4. **Content is data-driven** (towers, enemies, waves, upgrades as JSON under `config/ldtd/`, with
   defaults shipped in the jar under `src/main/resources/ldtd/defaults/`, loaded by `LdtdData`).
   Don't hard-code balance numbers in Java. A broken file must never take a server down: report
   it and keep the last good values.
5. **Enemies only ever damage blocks inside a Hearth territory.** Every block-damage path must go
   through the territory check.

## Conventions

- Package root: `com.micatechnologies.minecraft.ldtd`. Feature code goes in its own subpackage.
- Registry names use the `ldtd:` prefix.
- Never hard-code the mod id, name or version. Use `LdtdConstants`, which reads the build-generated
  `Tags` class. The version comes from the git tag (`YYYY.MM.DD` for releases). Keep `modVersion =`
  empty in `buildscript.properties`, because the release workflow pins it.
- Every push to `main` publishes a GitHub pre-release through `.github/workflows/`.

## Planning docs

`docs/agent-plans/` is **gitignored**. It holds the phased implementation plan
(`LDTD-v1-plan.md`) and agent working notes. Read its STATUS section first when resuming work.

## Testing gameplay headlessly

Pure logic gets JUnit tests in `ldtd.sim`. For in-world behaviour, drive a dev dedicated server
(`./gradlew runServer`) through its console: pipe commands into stdin once `Done (` appears in
`run/logs/latest.log`, and read the results from the server output.

- Vanilla stops ticking entities 15 seconds after the last player leaves, so run
  `/ldtd debug keepticking true` first, or marches and sieges silently freeze.
- The console has no position. Run position-based commands through an entity:
  `summon minecraft:armor_stand x y z {NoGravity:1b}` then
  `execute @e[type=armor_stand] ~ ~ ~ ldtd spawn shade 4`.
- `tp @e[type=ldtd:gloam_shade] ~ ~ ~` prints each mob's position, and `/ldtd route show` prints a
  route's length and breach count.
- `setworldspawn 0 100 0` keeps the chunks around the origin loaded for a test arena.

- After any change to recipes, models or data files, grep `run/logs/latest.log` for
  `Parsing error loading recipe`, `JsonParseException`, `Exception loading model` and
  `Unable to resolve texture`. These failures don't stop the game and are easy to miss. (A
  sub-typed result such as `ldtd:blueprint` needs `"data": 0` in its recipe.)

Rendering, GUIs and anything a player does with their hands still need the client. Drive it
through MCMCP when it is connected.
