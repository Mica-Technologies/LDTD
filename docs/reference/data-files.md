# Data files (JSON)

LDTD keeps **every balance number** in JSON under `config/ldtd/`, so a server or modpack can
rebalance it without a rebuild. Minecraft 1.12 has no datapacks, so this is LDTD's equivalent.

| File | What it holds |
| --- | --- |
| [`towers.json`](#towersjson) | Tower ranges, damage and fire rates, and the upgrade tiers |
| [`gloam.json`](#gloamjson) | Each Gloam unit's health, speed, attack, armour, dig power, cost and Duskglass drop |
| [`raid.json`](#raidjson) | Raid timing, budget, toughness, which units can come, Flame and payout |
| [`hearth_levels.json`](#hearth_levelsjson) | Each Hearth level's radius, sprite count and cost |
| [`blueprints.json`](#blueprintsjson) | The shape and cost of every blueprint, including the Hearth's growth stages |

Every file starts with a `_comment` explaining its fields.

## Editing safely

1. Edit the file (the server can be running).
2. Run `/ldtd reload`.

> *LDTD data reloaded.*

**A broken file never takes the server down.** If a file doesn't parse, or a value is out of
range, LDTD keeps that file's **previous values**, logs an error, and lists it:

> *LDTD data reloaded with 1 problem(s). The previous values were kept for those files:*
> *- towers.json: archer: range must be 0-64 (kept the previous values)*

Changes to unit stats apply to Gloam spawned **after** the reload.

### Updates fill in what's missing

When LDTD updates and its defaults gain a new field, your files are **brought up to date
automatically**: missing fields are added with their default values, and the file is rewritten
(the log lists what was added). Your own values always win. A list you've shortened (for
example, fewer Hearth levels) stays short.

To go back to the defaults for a file, delete it. It's recreated on the next start.

## towers.json

```json
{
  "towers": {
    "archer":    { "range": 20, "cooldownTicks": 20, "damage": 4 },
    "cannon":    { "range": 16, "cooldownTicks": 60, "damage": 8, "splashRadius": 2.5 },
    "frost":     { "range": 10, "cooldownTicks": 40, "damage": 1, "slowAmplifier": 0, "slowTicks": 60 },
    "lantern":   { "range": 0,  "cooldownTicks": 20, "damage": 0 },
    "guardpost": { "range": 12, "cooldownTicks": 600, "damage": 6, "units": 3 }
  },
  "upgrade": {
    "cost":     [10, 25],
    "damage":   [1.0, 1.35, 1.7],
    "range":    [1.0, 1.15, 1.3],
    "cooldown": [1.0, 0.9, 0.8]
  }
}
```

| Field | Meaning |
| --- | --- |
| `range` | Reach in blocks (0–64) |
| `cooldownTicks` | Time between shots (20 = 1 second). For a Guard Post: time to replace a fallen guard |
| `damage` | Half-hearts per hit. For a Guard Post: each guard's blow |
| `splashRadius` | Cannon only: radius of the burst |
| `slowAmplifier`, `slowTicks` | Frost only: Slowness level (0 = Slowness I) and duration |
| `units` | Guard Post only: guards kept |
| `upgrade.cost` | Duskglass for tier I→II and II→III |
| `upgrade.damage` / `range` / `cooldown` | Multipliers at tiers I, II and III |

## gloam.json

```json
{
  "units": {
    "shade": { "health": 20, "speed": 0.25, "attack": 3, "armor": 0, "digPower": 1.0,
               "budgetCost": 1, "duskglassChance": 0.3, "duskglassCount": 1 },
    "...": {}
  }
}
```

| Field | Meaning |
| --- | --- |
| `health`, `attack` | In half-hearts. The Dimcaster's `attack` is its bolt's damage |
| `speed` | Movement attribute (a vanilla zombie is 0.23). For the Moth-wraith, scales its flight |
| `armor` | Armour points, as for players |
| `digPower` | Damage per hit to a wall (1 = a bare-handed player, 0 = never digs) |
| `budgetCost` | What the unit costs a raid's budget |
| `duskglassChance`, `duskglassCount` | Chance to drop Duskglass when killed, and how much |

Units: `shade`, `smother`, `flicker`, `moth_wraith`, `dimcaster`, `eclipse`.

## raid.json

```json
{
  "countdownSeconds": 30,
  "waves": 3,
  "waveIntervalSeconds": 40,
  "budget":    { "base": 6, "perNight": 4, "perLevel": 8, "max": 400 },
  "toughness": { "perNight": 0.03, "max": 2.5 },
  "callEarlyBonus": 0.25,
  "flame":  { "max": 100, "regenPerMinute": 5 },
  "units": {
    "shade":       { "fromNight": 1, "weight": 10, "drain": 1, "maxShare": 1.0 },
    "flicker":     { "fromNight": 3, "weight": 4,  "drain": 1, "maxShare": 0.4 },
    "dimcaster":   { "fromNight": 3, "weight": 3,  "drain": 1, "maxShare": 0.3 },
    "smother":     { "fromNight": 3, "weight": 2,  "drain": 4, "maxShare": 0.3 },
    "moth_wraith": { "fromNight": 4, "weight": 3,  "drain": 2, "maxShare": 0.3 },
    "eclipse":     { "fromNight": 1, "weight": 0,  "drain": 6, "maxShare": 0.0 }
  },
  "payout": { "base": 2, "perRaid": 1, "calledMultiplier": 1.25 }
}
```

| Field | Meaning |
| --- | --- |
| `countdownSeconds` | Time between the announcement and the first wave |
| `waves`, `waveIntervalSeconds` | How many waves, and how far apart |
| `budget` | Budget = `base + perNight × raids survived + perLevel × (level − 1)`, capped at `max` |
| `toughness` | Raid Gloam health × `1 + perNight × raids survived`, capped at `max` |
| `callEarlyBonus` | Extra budget share for a called raid |
| `flame.max`, `flame.regenPerMinute` | The Hearth's Flame, and how fast it refills between raids |
| `units.*.fromNight` | First raid number the unit can appear in |
| `units.*.weight` | How likely the unit is to be picked (0 = never bought; the Eclipse comes another way) |
| `units.*.drain` | Flame lost per second for each of these at the Hearth |
| `units.*.maxShare` | Most of the budget this unit may take |
| `payout` | Duskglass for a raid survived: `base + perRaid × raid number`, × `calledMultiplier` if called |

At least one unit must be able to appear on raid 1.

## hearth_levels.json

```json
{
  "levels": [
    { "level": 1, "radius": 24, "maxSprites": 2, "duskglass": 0,  "materials": {} },
    { "level": 2, "radius": 32, "maxSprites": 3, "duskglass": 8,  "materials": { "minecraft:stonebrick": 32 } },
    "..."
  ]
}
```

| Field | Meaning |
| --- | --- |
| `level` | 1, 2, 3... with no gaps |
| `radius` | Territory radius, 8–128. Never smaller than the level before |
| `maxSprites` | Hearth-sprites the Hearth keeps |
| `duskglass` | Duskglass (in hand) to reach this level |
| `materials` | Items taken from the stockpile. Keys are `modid:name` or `modid:name@meta` |

The largest radius also sets how far apart Hearths must be (twice the radius plus
`hearth.territorySpacing` on each side), so raising it spaces Hearths further apart.

## blueprints.json

```json
{
  "blueprints": {
    "archer_tower": {
      "tower": "archer",
      "palette": { "S": "minecraft:stonebrick", "L": "minecraft:log",
                   "P": "minecraft:planks", "F": "minecraft:fence" },
      "layers": [
        ["SSS", "SSS", "SSS"],
        ["L.L", "...", "L.L"],
        ["L.L", "...", "L.L"],
        ["PPP", "PPP", "PPP"],
        ["F.F", ".C.", "F.F"]
      ],
      "extraCost": { "minecraft:arrow": 16 }
    }
  }
}
```

A blueprint is a stack of **layers**, bottom first. Each layer is a list of **rows**, front to
back, and each row a string of **cells**, left to right.

| Cell | Meaning |
| --- | --- |
| A palette letter | Build this block |
| `.` | Must be air (replaceable blocks are cleared) |
| (space) | Leave whatever is there |
| `C` | The tower's core. Exactly one in a tower blueprint, none in anything else |

| Field | Meaning |
| --- | --- |
| `tower` | Which tower the core runs: `archer`, `cannon`, `frost`, `lantern`, `guardpost`. Leave out for walls and other plain structures |
| `palette` | Letter → block, as `modid:name` or `modid:name@meta` (`minecraft:stone_slab@5` is a stone brick slab). Blocks from other mods work |
| `extraCost` | Items charged on top of the blocks |
| `internal` | `true` for the Hearth's growth stages (`hearth_stage_<level>`). Never offered to players |

Limits: a footprint of 1–9 × 1–9, at most 16 layers, ids of lowercase letters, digits and
underscores.

### Adding a blueprint

Add an entry under a new id, run `/ldtd reload`, and give it out:

```text
/give @p ldtd:blueprint 1 0 {blueprint:"watchtower_tall"}
```

There's no crafting recipe for a new blueprint unless you add one (with a recipe mod, or
CraftTweaker).

### Changing the Hearth's look

The growth stages are ordinary blueprints called `hearth_stage_1`, `hearth_stage_3` and
`hearth_stage_5`, built centred on the Hearth. Edit them to restyle every Hearth's hall, or add a
`hearth_stage_2` or `hearth_stage_4` to give those levels a stage too. A missing stage that ships
with LDTD is restored on load.
