# Commands

## For everyone

### `/hearth`

Run these **standing in your territory**. They act on that territory's Hearth.

| Command | What it does |
| --- | --- |
| `/hearth trust <player>` | Lets the player build in the territory, use the stockpile, level the Hearth, order blueprints and command its Ember Guards. They must have joined the server before |
| `/hearth untrust <player>` | Takes that away |
| `/hearth trusted` | Lists who the Hearth trusts. Anyone in the territory can run it |
| `/hearth guide` | Gives you a copy of *The Hearthkeeper's Guide* (dropped at your feet if your inventory is full). Works anywhere |

Only the Hearth's owner (and operators) can change the trust list.

### `/randomspawn`

Also `/rs`. Teleports you to a random safe spot in the dimension: on solid ground, out of liquid,
with room to stand. Handy for finding fresh ground for a Hearth.

| | |
| --- | --- |
| Who can use it | Everyone, by default (`randomSpawn.permissionLevel`) |
| Cooldown | 5 minutes per player. Operators skip it |
| Where it looks | Only already-generated chunks. On a new world, where the generated area reaches less than 1000 blocks from spawn, anywhere within 1000 blocks |
| Dimensions | Any dimension with a sky (not the Nether) |

`/rs <player>` sends someone else (operators only).

## For operators

`/ldtd` needs permission level 2 (an operator, or cheats on in singleplayer). Commands marked
*in a territory* act on the territory you're standing in.

### Data

| Command | What it does |
| --- | --- |
| `/ldtd reload` | Re-reads every file in `config/ldtd/`. Lists any that failed (those keep their previous values) |

### Hearths

| Command | What it does |
| --- | --- |
| `/ldtd hearth info` | The territory you're in, or the nearest Hearth if you're in none: owner, level, radius |
| `/ldtd hearth list` | Every Hearth in the dimension |
| `/ldtd hearth setlevel <level>` | *In a territory.* Sets the Hearth's level, free. Raising it queues the growth stages |
| `/ldtd hearth setowner <player>` | *In a territory.* Gives the Hearth to another player |

### Raids

| Command | What it does |
| --- | --- |
| `/ldtd raid start` | *In a territory.* Starts a raid now, day or night, with or without the owner nearby |
| `/ldtd raid start called` | The same, as a called raid (bigger, pays more) |
| `/ldtd raid stop` | *In a territory.* Ends the raid with no reward. Its Gloam vanish |

A raid started in daytime ends at once as a "dawn" ending (survived, and paid) when its countdown
runs out.

### Gloam and routes

| Command | What it does |
| --- | --- |
| `/ldtd spawn <unit> [count]` | Spawns 1–64 Gloam around you, marching on the nearest Hearth. Units: `shade`, `smother`, `flicker`, `moth_wraith`, `dimcaster`, `eclipse`. They aren't part of a raid and aren't toughened |
| `/ldtd route show` | Plans a route from where you stand to the nearest Hearth, and draws it for 10 seconds. Reports its length and how many blocks it breaks |

### Dusk Spires

| Command | What it does |
| --- | --- |
| `/ldtd spire list` | Every standing spire in the dimension: tier and core position |
| `/ldtd spire place [tier]` | Raises a spire on the ground where you stand. Tier 1–3, or set by distance from spawn |

### Debug

| Command | What it does |
| --- | --- |
| `/ldtd debug keepticking <true\|false>` | Keeps entities ticking with no player online, for testing on a dedicated server through its console. Resets when the server stops |

!!! tip "Running position commands from the console"

    The server console has no position. Run position commands through an entity:

    ```text
    summon minecraft:armor_stand 0 100 0 {NoGravity:1b}
    execute @e[type=armor_stand] ~ ~ ~ ldtd spawn shade 4
    ```
