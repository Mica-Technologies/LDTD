# Building from source

## What you need

- **A JDK from 17 to 22** to run Gradle. **21 is recommended**, and is what CI uses. The mod
  itself targets Java 8 (through Jabel), so it runs in any 1.12.2 game.
- Git. The version is taken from the git tag, so build from a clone, not a source zip.

## Tasks

Set `JAVA_HOME` to the JDK first.

```bash
./gradlew build      # compile, unit tests, and the jar in build/libs/
./gradlew test       # unit tests only (plain JVM, no Minecraft)
./gradlew runClient  # a dev client
./gradlew runServer  # a dev dedicated server
```

`build/libs/` gets three jars: `ldtd-<version>.jar` (the one to install), `-dev` (deobfuscated,
for other mods' dev environments) and `-sources`.

!!! warning "Don't rebuild while `runClient` is running"

    Stop the dev client first. A rebuild under a live client can leave it in a confused state.

!!! warning "Never run `updateBuildScript`"

    `build.gradle` is the upstream GTCEu buildscript with two deliberate edits, and that task
    overwrites both. Project build logic goes in `addon.gradle`.

## Versions and releases

- Releases are dated: `YYYY.MM.DD`.
- Every push to `main` publishes a **pre-release** through GitHub Actions.
- A full release is a manual run of the release workflow.
- Leave `modVersion =` empty in `buildscript.properties`. The release workflow sets it.

## After a change

`./gradlew build` passing isn't the whole check. After a change to recipes, models or data
files, start the dev client and search `run/logs/latest.log` for:

- `Parsing error loading recipe`
- `JsonParseException`
- `Exception loading model`
- `Unable to resolve texture`

These failures don't stop the game and are easy to miss.

## Testing in-world without a client

Pure logic is tested with JUnit in `ldtd.sim`. For in-world behaviour, a dev dedicated server
can be driven from its console:

1. `./gradlew runServer`, and wait for `Done (` in `run/logs/latest.log`.
2. `/ldtd debug keepticking true`, or vanilla stops ticking entities 15 seconds after the last
   player leaves.
3. The console has no position, so run position commands through an entity:

    ```text
    summon minecraft:armor_stand 0 100 0 {NoGravity:1b}
    execute @e[type=armor_stand] ~ ~ ~ ldtd spawn shade 4
    ```

4. `tp @e[type=ldtd:gloam_shade] ~ ~ ~` prints each Gloam's position, and `/ldtd route show`
   reports a route's length and breaches.

CI also boots a real dedicated server on every pull request (`.github/scripts/server-smoke-test.sh`),
which catches client-only code reaching the server.
