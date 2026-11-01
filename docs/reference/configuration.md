# Configuration

`config/ldtd.cfg` holds LDTD's **switches**: what's on, what's protected, and how the client draws
things. It's an ordinary Forge config file. Edit it while the game is stopped.

The **balance numbers** (tower stats, unit stats, raid sizes, Hearth levels, blueprints) are in
the JSON files instead. See [Data files](data-files.md).

## raids

| Setting | Default | What it does |
| --- | :---: | --- |
| `enabled` | `true` | Master switch for raids. `false` makes LDTD building-only |
| `nightRaids` | `true` | Whether raids start on their own at nightfall |
| `callEarly` | `true` | Whether players may call a raid early at the Hearth |
| `maxAlivePerHearth` | `120` | Most Gloam alive at once per Hearth during a raid (10–500). The rest wait their turn |
| `defenderRange` | `96` | How close (in blocks) the Hearth's owner must be, online, for a night raid to start (16–1024) |
| `routeNodeBudget` | `60000` | Most steps the route planner considers for one route before giving up. Bounds the server time a route can take |

## hearth

| Setting | Default | What it does |
| --- | :---: | --- |
| `fallStockpileLoss` | `0.25` | Share of each stockpile stack lost when a Hearth falls (0–1) |
| `relightFuel` | `minecraft:coal` | The item that relights a fallen Hearth |
| `relightFuelCount` | `8` | How many it takes (1–64) |
| `ownerOnlyBreak` | `true` | Whether only a Hearth's owner (and operators) may break it |
| `territorySpacing` | `4` | Extra gap, in blocks, between the edges of two fully grown territories |
| `instantBuild` | `false` | Whether survival blueprints build at once (paid up front) instead of being built by sprites. Creative always builds at once |
| `giveGuide` | `true` | Give each player *The Hearthkeeper's Guide* the first time they join |
| `villageBuildings` | `true` | Whether new villages can have a Hearth Square and Watch Posts. Needs a restart |

## griefing

| Setting | Default | What it does |
| --- | :---: | --- |
| `gloamDamageBlocks` | `true` | Whether the Gloam may break blocks at all. Even when `true`, only inside a lit territory |
| `siegeDropsBlocks` | `true` | Whether blocks the Gloam break drop as items |
| `breachCostPerHardness` | `4.0` | How strongly raids prefer walking around walls to breaking them: a block's route cost per point of hardness, in blocks walked |

## gloam

| Setting | Default | What it does |
| --- | :---: | --- |
| `lightThreshold` | `10` | Block light at or above which the Gloam are weakened (0–15) |
| `litDamageMultiplier` | `1.5` | Damage multiplier the Gloam take while weakened by light |
| `litSlowdown` | `0.15` | Share of their speed the Gloam lose while weakened by light |
| `burnInDaylight` | `true` | Whether the Gloam smoulder and take damage under the open sky by day |

## protection

| Setting | Default | What it does |
| --- | :---: | --- |
| `enabled` | `true` | Master switch for territory protection. Never affects the Gloam |
| `blocks` | `true` | Untrusted players can't break or place blocks, empty buckets or light fires in a territory |
| `stockpile` | `true` | Untrusted players can't open a Hearth's stockpile |
| `explosions` | `true` | Explosions break no blocks inside a claimed territory (they still hurt) |
| `opsBypass` | `true` | Operators ignore territory protection |

## spires

| Setting | Default | What it does |
| --- | :---: | --- |
| `enabled` | `true` | Whether Dusk Spires generate in new chunks |
| `rarity` | `300` | One chunk in this many gets a spire, before the other rules |
| `minSpawnDistance` | `256` | No spire within this many blocks of the world spawn |
| `spacing` | `384` | No two spires closer than this |
| `influence` | `384` | Spires within this many blocks of a Hearth make its raids bigger and aim them |
| `budgetPerTier` | `3` | Extra raid budget per tier of each spire in range |
| `darknessRadius` | `32` | No Gloam within this many blocks of a standing spire counts as lit. 0 turns it off |
| `eclipseAfterSpires` | `3` | Spires razed near a Hearth before the Eclipse comes for it |

## randomSpawn

| Setting | Default | What it does |
| --- | :---: | --- |
| `enabled` | `true` | Whether `/randomspawn` (`/rs`) is available |
| `permissionLevel` | `0` | Permission level needed: 0 = everyone, 2 = operators |
| `cooldownSeconds` | `300` | Wait between uses. Operators skip it |
| `fallbackRadius` | `1000` | On a world generated less than this far from spawn, `/rs` may pick ungenerated ground within this radius |

## client

These only matter on the client, and each player can set their own.

| Setting | Default | What it does |
| --- | :---: | --- |
| `showTerritoryBorders` | `holding` | When to draw territory borders: `holding` (while holding a Hearth), `always` or `never` |
| `edgeParticles` | `true` | Ember sparks along a territory's border when you're near it |
| `healthBars` | `true` | A small health bar over wounded Gloam within 32 blocks |
