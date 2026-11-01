# LDTD research: tower defense, Minecraft Legends, and how to build it on Forge 1.12.2

Research notes for **LDTD (Limitless Development: Tower Defense)**. Written 2026-11-01, before any
code. This covers what the genre is, the mechanics it is built from, what Minecraft Legends adds, and
how those map onto Forge 1.12.2. Nothing here is decided yet; section 7 lists the decisions.

---

## 1. What a tower defense game is

Every tower defense (TD) game uses the same loop:

```
prepare (build / upgrade / spend) -> wave arrives -> defenses fight it -> reward -> escalate -> repeat
```

The player defends a **goal** (a core, gate, village or life counter) from **waves** of enemies that
take a **route** to it. The player does not usually fight directly. They spend a limited **economy**
on **defenses** placed in space. The skill is in placing and spending ahead of time. The central
tension is "spend now or save for something better". Every other mechanic is there to make that
choice interesting.

### Sub-genres (from most rigid to most free)

| Sub-genre | Examples | Route | Player role | Notes for us |
|---|---|---|---|---|
| **Fixed path** | Kingdom Rush, Bloons TD | Designer-drawn lane | Pick tower spots along it | Deterministic and easy to balance. Least agency. |
| **Mazing** | Desktop TD, Fieldrunners, WC3 TD maps | Open field. Towers *are* the walls | Build the path itself | Must keep a route open. Enemies repath on every placement. |
| **Action TD** | Orcs Must Die, Dungeon Defenders, Sanctum | Fixed or semi-fixed | Player fights alongside traps/towers in first/third person | The natural fit for a first-person game like Minecraft. |
| **Base-defense / RTS-lite** | Minecraft Legends, They Are Billions, Thronefall, Bad North | Open world. Enemies pathfind and break walls | Gather, build a base, command units, defend at night | Closest to the "immersive" goal. |
| **Roguelite TD** | Rogue Tower, Dorfromantik-likes | Grows run by run | Draft towers/upgrades between waves | Good for replay value. A possible later mode. |

**Takeaway:** "immersive TD in Minecraft" sits between **action TD** (the player is physically there
with a sword and bow) and **base-defense** (open world, enemies attack walls, building is
construction and not just placing an icon). Pure fixed-path TD would feel like a minigame map, and
plenty of those already exist.

---

## 2. The mechanics toolkit

### 2.1 Defenses (towers)

**Every tower needs a distinct role.** Two towers that differ only in numbers are one tower.
The standard roles are:

| Role | Example | Answers |
|---|---|---|
| Single-target DPS | Archer, ballista, sniper | Tanks, bosses |
| Area of effect / splash | Cannon, mortar, TNT launcher | Swarms |
| Control / slow | Frost, web, slime, knockback | Fast enemies. Buys time for the other towers |
| Damage over time | Fire, poison, lava | High-HP enemies. Regenerating enemies |
| Anti-air / detection | Spotter, beacon | Flyers, invisible enemies |
| Support / aura | Banner, beacon-like buff | Makes clusters of towers better. Rewards layout |
| Economy | Farm, mine, trader | Pay now, profit later. The purest "spend vs save" choice |
| Barrier | Walls, gates, spikes | Steers or holds enemies inside the kill zone |

**Targeting priority** (first / last / strongest / weakest / closest), set per tower, adds depth at
almost no cost.

**Upgrades** come in two styles:
- Linear tiers (I -> II -> III). Simple, and the usual choice.
- Branching paths (Bloons TD style: pick 2 of 3 tracks, or "tier 5 locks the other path"). Much more
  build variety. More content to make.

**Selling** usually refunds 50-80%. That keeps mistakes recoverable without making placement free.

### 2.2 Enemies

Enemy design exists to **test** the tower roles. The standard archetypes:

| Archetype | Tests |
|---|---|
| Swarm (many weak) | AoE |
| Tank (slow, high HP) | Single-target DPS, DoT |
| Runner (fast, fragile) | Control, coverage |
| Armored / resistant to a damage type | Damage-type variety |
| Flyer (ignores the ground route and walls) | Anti-air |
| Shielded / healer / buffer | Target priority |
| Splitter (spawns children on death) | Overkill and AoE |
| **Siege** (targets structures, not the goal) | Wall/tower durability, repair. *Very important for an open-world version* |
| Stealth | Detection |
| Boss / mini-boss | Everything at once. Wave climax |

### 2.3 Waves

- **Composition.** Waves are designed (hand-written lists) or **budget-generated**: each wave gets a
  point budget that grows each round, and enemy types are bought from it under rules such as
  "no flyers before wave 5" or "at most 2 bosses". Budget generation scales to endless mode for free.
- **Telegraphing.** Show the next wave's makeup and where it comes from. Players hate surprises they
  could not plan for.
- **Call early.** Start the next wave sooner for a bonus. A skill and risk lever.
- **Scaling.** Enemy HP usually scales faster than linearly. Armor and resistances scale in steps.
  Tune with simulation (sample DPS against wave HP, leak rate) rather than by feel alone.

### 2.4 Economy

Common income sources: kill bounty, per-wave stipend, interest on banked money, income buildings.
**Immersive variant (Legends style):** no abstract gold at all. Defenses cost **real materials** that
the player or helper mobs gather from the world. Enemies drop a special currency (Legends'
prismarine) that unlocks upgrades.

### 2.5 Other common systems

- **Goal health / lives.** Leaking enemies costs lives, or damages a core block.
- **Hero / player unit.** A controllable unit with abilities. In Minecraft this is the player.
- **Active abilities** on cooldown (airstrike, freeze, reinforcements).
- **Map design.** Choke points, several lanes, several spawn points, terrain height (towers on high
  ground get range).
- **Readability.** Clear tower ranges, enemy health bars, damage-type icons. TD lives or dies on how
  readable it is.

---

## 3. Minecraft Legends (the "immersive" reference)

What it does ([Wikipedia](https://en.wikipedia.org/wiki/Minecraft_Legends),
[Twinfinite tips](https://twinfinite.net/2023/04/minecraft-legends-tips-tricks-for-beginners/),
[Sportskeeda review](https://sportskeeda.com/minecraft/minecraft-legends-review)):

- **Allays do the work.** Blue allays gather resources and yellow allays build. The player rides a
  mount, picks a structure, and the allays assemble it on the spot if the materials are there.
  Building looks like real construction, not an instant pop-in.
- **Finite resource nodes** spread across biomes force the player to travel.
- **Structures:** arrow towers, walls, gates, golem spawners, and upgrade structures paid for with
  prismarine. Prismarine comes only from destroying piglin structures.
- **Units:** golems spawned from spawner structures, sent with a banner command ("go there", "attack
  that").
- **Two sides of play.** *Offense*: assault piglin bases (bridging up to plateau bases).
  *Defense*: piglins plan by day and **raid villages at night**. The player fast-travels to the
  village and fortifies it with walls, gates and towers around the village well. Clearing a group of
  bases triggers a boss that attacks every night until it is destroyed.

**What critics said**, which we should learn from: the loop is shallow ("attack piglins, build
structures and upgrades"), the strategy has little depth, and the unit AI is poor. The parts that
landed were the presentation of building and the night-raid tension.

**Lessons for LDTD**
1. Keep the **allay-builder** idea: watching your defenses go up block by block is the immersion.
2. Keep **night raids on something you care about**. A village or your own base is a far better
   goal than an abstract core.
3. Add the **depth Legends lacked**: distinct tower roles, enemy archetypes that counter towers,
   real upgrade choices, and wave telegraphing (sections 2.1-2.3).
4. **Don't depend on clever unit AI.** Minecraft 1.12 mob AI is weak. Towers and walls are reliable.
   Commanded units should be simple (hold point, follow, attack target).

---

## 4. What already exists on 1.12.2

| Thing | What it is | Gap |
|---|---|---|
| [OpenModularTurrets](https://www.mc-mod.net/openmodularturrets-mod/) (+ [Passive Defense](https://www.mc-mod.net/?p=178131)) | Modular tech turrets and walls for base security | No waves, no attackers, no loop. Just turrets |
| [Totem Defender](https://www.mc-mod.net/?p=101667) | 4 configurable totems that guard an area | Same: defense with nothing to defend against |
| [Tower Defense Units](https://moddex.gg/mod/tower-defense-units) | Arrow turret and defensive units | Small scope. 1.12.2 support unclear |
| [World of Invasions](https://www.curseforge.com/minecraft/modpacks/world-of-invasions/files/4634795) | Modpack: build and defend a factory against waves | Assembled from other mods, not a single system |
| [Mirage Tower Defense](https://www.boxtoplay.com/en-cm/minecraft-hosting/minecraft-server/curseforge-maps/map-mctsts-tower-defense-565) | Fixed-path TD **map** | A map, not a mod |

**Nobody on 1.12.2 owns the whole loop** (a director that sends waves, enemies that route and
siege, towers with roles, building, and an economy). That is the gap LDTD fills.

---

## 5. How it maps onto Forge 1.12.2

### 5.1 The defended area: a "Hearth" / core block
A controller block (a tile entity) claims a **territory** (a radius or a set of chunks) and holds the
defense state: wave number, the enemy budget, spawn points, the stockpile. The state is saved in the
TE, plus `WorldSavedData` for anything that spans territories. This makes a village or a player base
into something the wave director can target. A boss can target one specific Hearth.

### 5.2 Building: blueprints plus builder mobs
- Towers are **structure templates**. Vanilla 1.12 `Template` / structure-block NBT works, and so do
  our own JSON definitions. Each has one **controller block** that does the targeting and firing.
  The rest is ordinary blocks, so the tower is real terrain: you can see it, climb it, and enemies can
  break it.
- Placement: a blueprint item shows a **ghost preview** (translucent render in
  `RenderWorldLastEvent`, as other LD mods already do), with rotation and a validity check.
- **Builder mobs** (our allays) carry materials from the Hearth stockpile and place blocks one at a
  time over a few seconds. They also repair. Offer an instant-build option for creative and testing.

### 5.3 Enemies and routing (the hardest part)
- Vanilla `PathNavigateGround` is short-range (follow range, limited node budget) and costly when
  hundreds of mobs each repath. **Don't let every mob pathfind to the core on its own.**
- Recommended: one **shared route per wave/spawn**, computed on the server. A* or a **flow field**
  over a coarse grid from the spawn to the Hearth, recomputed only when the terrain or walls in the
  territory change. Enemies follow the waypoints or flow vectors and use vanilla navigation only
  for the short hop to the next one.
- **Walls: block, don't forbid.** Classic mazing refuses any placement that blocks the path. In
  Minecraft it is more natural (and more Legends / They Are Billions) to allow full walls. If no
  open route exists, the router finds the **cheapest breach** (the wall with the fewest HP to cut
  through) and siege enemies attack it. Block damage is a server-side `BlockPos -> HP` map with crack
  visuals through `sendBlockBreakProgress`. A config switch protects non-territory blocks or keeps
  enemy griefing on whitelisted blocks only.
- Flyers ignore the route (straight-line steering at a fixed altitude).

### 5.4 Towers in code
- Controller TE ticks on the **server only**, retargets every N ticks with one AABB query, and
  stores its targeting priority.
- Projectiles: real entities for slow, visible ones (cannonballs, arrows). **Hitscan plus particles**
  for fast ones (lightning, beams) to keep the entity count down.
- Damage types (pierce / blunt / fire / frost / magic) against enemy resistances, as data.

### 5.5 Performance budget (1.12 limits)
- Cap living enemies (for example 100-200 per territory) and **trickle-spawn** to that cap rather
  than spawning a wave all at once.
- Shared routing (5.3) removes most of the AI cost. Keep the custom AI task lists short.
- Spread tower retargeting across ticks (stagger by position hash).
- Use a `ForgeChunkManager` ticket to keep the battle area loaded **only during a wave**.
- We can measure all of this with MCMCP's tick/profile tools (`server_profile_ticking`,
  `server_tick_stats`) on a real raid.

### 5.6 Waves and time
- Trigger options: **night raids** (world time, Legends style), **player-triggered** ("ring the
  bell", with a call-early bonus), or both.
- A wave director per Hearth: budget-based generation from JSON enemy and wave tables, a telegraph
  HUD (next wave, direction, makeup), and boss events.

### 5.7 Economy
- **Real items** as the construction cost (wood, stone, iron), taken from the Hearth stockpile.
  This is the immersive version and it plugs into survival.
- An **enemy-drop currency** for upgrades and unlocks (our prismarine).
- Optional abstract "gold" for an arena/minigame mode only.

### 5.8 Data-driven content
1.12 has no datapacks, so towers, enemies, waves and upgrade trees go in JSON under
`config/ldtd/` (defaults shipped in the jar), reloadable with a command. Balance changes then don't
need a rebuild, and modpacks can add content.

### 5.9 Stack
Same setup as the other LD mods: the GregTechCEu/RFG buildscript, Java 8, Forge 1.12.2, mixins only
where events can't reach. Multiplayer from day one: the server owns all state and the client only
renders (ghost previews, HUD, range circles).

---

## 6. A draft core loop for LDTD

```
DAY    gather / have builders gather  ->  place blueprints  ->  builders construct & repair
       scout: telegraph shows tonight's raid size, direction, enemy types
NIGHT  raid spawns at the territory edge -> follows the shared route -> sieges walls if blocked
       player fights in person + towers + simple commanded units
DAWN   reward: enemy currency, salvage -> upgrades / unlocks
LATER  push back: assault enemy outposts to stop raids at the source, and to fight bosses
```

**A sensible first playable (MVP):** one Hearth block, 3 towers (archer / cannon / frost), walls
and a gate, 4 enemy types (grunt / runner / brute-siege / flyer), shared routing with wall-breach
fallback, budget waves at night, and a telegraph HUD. Builder mobs, outposts, units and branching
upgrades come after that.

---

## 7. Decisions

Decided 2026-11-01:

| # | Question | Decision |
|---|---|---|
| 1 | Mode | **Survival open world.** Defend a Hearth around a base or village. Night raids in the normal world. |
| 2 | Building | **Builder mobs** (allay-like) build blueprints block by block and repair. Instant build for creative/debug only. |
| 3 | Attackers | **A new faction of our own.** Original look and archetypes, not Legends' piglins. |
| 4 | Griefing | **Territory blocks only.** Enemies may damage any block inside a Hearth's territory and nothing outside it. |

Still open:

5. **Economy:** real gathered items plus an enemy-drop currency (the section 5.7 recommendation), or
   something else.
6. **Offense side:** in scope (enemy outposts to assault), or defense only for v1.
7. **Wave trigger:** night only, player-triggered, or both.

---

### Sources
- [Minecraft Legends: Wikipedia](https://en.wikipedia.org/wiki/Minecraft_Legends)
- [Minecraft Legends tips: Twinfinite](https://twinfinite.net/2023/04/minecraft-legends-tips-tricks-for-beginners/)
- [Minecraft Legends review: Sportskeeda](https://sportskeeda.com/minecraft/minecraft-legends-review)
- [Minecraft Legends review: Gaming Trend](https://gamingtrend.com/feature/reviews/minecraft-legends-review-a-cute-and-simple-minecraft-strategy-game/)
- [Tower defense genre notes (Godot agent skill)](https://claudeskills.info/skills/thedivergentai/gd-agentic-skills/genre-tower-defense/)
- [r/TowerDefense: "Mazing or no mazing"](https://redlib.cypherpunktools.com/r/TowerDefense/comments/1k06wk9/mazing_or_no_mazing)
- [OpenModularTurrets](https://www.mc-mod.net/openmodularturrets-mod/), [Open Modular Passive Defense](https://www.mc-mod.net/?p=178131), [Totem Defender](https://www.mc-mod.net/?p=101667), [Tower Defense Units](https://moddex.gg/mod/tower-defense-units), [World of Invasions](https://www.curseforge.com/minecraft/modpacks/world-of-invasions/files/4634795), [Mirage Tower Defense map](https://www.boxtoplay.com/en-cm/minecraft-hosting/minecraft-server/curseforge-maps/map-mctsts-tower-defense-565)
