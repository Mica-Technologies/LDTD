# Installing

LDTD is a **Minecraft 1.12.2 Forge** mod. It needs to be installed on **both the client and the
server**: the client draws the Gloam, the towers, the ghost previews and the territory border, and
the server runs everything else.

## What you need

| | |
| --- | --- |
| **Minecraft** | 1.12.2 |
| **Forge** | Forge for 1.12.2. The latest 1.12.2 build is recommended |
| **Java** | Java 8, as for any 1.12.2 game |
| **Other mods** | None. LDTD has no required dependencies |

## Steps

1. Install Forge for 1.12.2, if your launcher doesn't already have it.
2. Download the latest `ldtd-<version>.jar` from the
   [GitHub releases](https://github.com/Mica-Technologies/LDTD/releases).
   Full releases are dated (`2026.11.01`). Every change to the mod also makes a *pre-release*,
   which is fine for trying the newest features but may have rough edges.
3. Put the jar in your instance's `mods/` folder (and in the server's `mods/` folder, for
   multiplayer).
4. Start the game. LDTD shows up in the **Mods** list as *LDTD: Limitless Development: Tower
   Defense*, and in the creative inventory as the **LDTD: Tower Defense** tab.

!!! info "Use the jar without `-dev` or `-sources` in its name"

    A release lists several files. The one to install is the plain `ldtd-<version>.jar`. The
    `-dev` jar is for mod developers and the `-sources` jar is the source code.

## Adding LDTD to an existing world

You can. LDTD changes nothing until somebody sets down a Hearth, so existing builds are safe. Two
things only happen in **newly generated** chunks:

- **Dusk Spires** generate in new chunks, at least 256 blocks from spawn.
- **Villages** that generate after LDTD is installed may have a **Hearth Square** and
  **Watch Posts**.

To find fresh, ungenerated ground for a Hearth, `/randomspawn` (or `/rs`) sends you to a random
safe spot. See [Commands](../reference/commands.md#randomspawn).

## First launch

On first launch LDTD writes:

- `config/ldtd.cfg`, the switches (raids on or off, griefing, protection, spires, the client's
  border display and so on). See [Configuration](../reference/configuration.md).
- `config/ldtd/*.json`, every balance number: towers, Gloam, raids, Hearth levels and
  blueprints. See [Data files](../reference/data-files.md).

You don't have to touch either. The defaults are what the mod is balanced around.

## Uninstalling

Remove the jar. Forge will warn that the world was saved with LDTD's blocks and items. If you
continue, LDTD's blocks (Hearths, tower cores, Dusk Spire stone) disappear. Vanilla blocks that
Hearth-sprites built (stone bricks, planks, fences) stay where they are.
