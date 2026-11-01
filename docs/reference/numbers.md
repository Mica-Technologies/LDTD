# Numbers and formulas

How LDTD works things out, with its default values. Every input here can be changed in
`config/ldtd.cfg` or the [data files](data-files.md).

## Raids

**Raid number** *n* = the Hearth's raids survived + 1.

**Budget**

```text
budget = min(400, 6 + 4·(n − 1) + 8·(level − 1))
       × 1.25 if called early (then capped at 400 again)
       + 3 × (sum of the tiers of standing spires within 384 blocks), capped at 400
```

**Toughness** (raid Gloam only):

```text
health × min(2.5, 1 + 0.03·(n − 1))
```

**Payout**, for a raid that holds or lasts until dawn:

```text
Duskglass = round((2 + n) × 1.25 if called early)
```

**Composition:** units are bought one at a time, picked at random by `weight` among those that
are allowed this raid (`fromNight ≤ n`), affordable, and under their `maxShare` of the budget,
until nothing more fits. They're sorted cheapest first and split into three equal waves.

**Timeline:** announcement → 30 s countdown → wave 1 → +40 s wave 2 → +40 s wave 3. Two Gloam
spawn every half second while fewer than 120 are alive.

**Flame:** each second after the countdown, Flame −= the sum of `drain` for every raid Gloam
within 3.5 blocks of the Hearth. Between raids, Flame += 5 per minute, up to 100.

## Towers

| | Formula |
| --- | --- |
| Range | `range × [1.0, 1.15, 1.3][tier]` |
| Damage | `damage × [1.0, 1.35, 1.7][tier]` |
| Time between shots | `cooldownTicks × [1.0, 0.9, 0.8][tier]` |
| Cannon splash | `damage × (1 − 0.5 × distance / 2.5)` for every Gloam within 2.5 blocks |
| Broken tower | Fewer than 75% of its blueprint blocks in place |
| Salvage | Half of each cost item (rounded down) + half the Duskglass spent on tiers |

**Damage against armour** is vanilla's: `damage × (1 − min(20, max(armour / 5, armour − damage
/ 2)) / 25)`. A tier-I Cannon's 8 half-hearts does 7.36 against a Smother's 6 armour, and 6.08 against the Eclipse's 10.

**Light:** at block light ≥ 10, damage taken × 1.5 and speed × 0.85 (light's slowdown doesn't
apply while the Gloam is already slowed by frost).

### Tower stats by tier

| Tower | Tier | Range | Damage (half-hearts) | Cooldown (ticks) | Damage per second |
| --- | :---: | :---: | :---: | :---: | :---: |
| Archer | I | 20 | 4 | 20 | 4.0 |
| | II | 23 | 5.4 | 18 | 6.0 |
| | III | 26 | 6.8 | 16 | 8.5 |
| Cannon | I | 16 | 8 (splash) | 60 | 2.7 per target |
| | II | 18.4 | 10.8 | 54 | 4.0 |
| | III | 20.8 | 13.6 | 48 | 5.7 |
| Frost | I | 10 | 1 (all) | 40 | 0.5 per target |
| | II | 11.5 | 1.35 | 36 | 0.75 |
| | III | 13 | 1.7 | 32 | 1.06 |
| Guard Post | I | 14 (leash) | 6 per guard | 600 (replacement) | - |
| | II | 14 | 8.1 | 540 | - |
| | III | 14 | 10.2 | 480 | - |

## Sieges

```text
block hit points = 10 × max(1, hardness)
route cost of breaking a block = 1 + hardness × 4      (in blocks walked)
```

| Block | Hardness | Hit points | Shade hits | Smother hits | Route cost |
| --- | :---: | :---: | :---: | :---: | :---: |
| Dirt | 0.5 | 10 | 10 | 2 | 3 |
| Planks | 2 | 20 | 20 | 4 | 9 |
| Stone bricks | 1.5 | 15 | 15 | 3 | 7 |
| Cobblestone | 2 | 20 | 20 | 4 | 9 |
| Iron block | 5 | 50 | 50 | 9 | 21 |
| Obsidian | 50 | 500 | 500 | 84 | 201 |

Gloam hit once a second, and damage from several adds up. A cracked block heals after 30 seconds
without a hit.

## Hearth

| | |
| --- | --- |
| Spacing between Hearths | `2 × (largest radius + territorySpacing)` = 2 × (64 + 4) = 136 blocks |
| Sprites | One new sprite every 10 s, up to the level's `maxSprites` |
| Sprite trips | Up to 4 blocks per trip |
| Fall | Each stockpile stack loses `round(count × 0.25)` |
| Relight | 8 coal; Flame comes back at 50 |

## Dusk Spires

| | |
| --- | --- |
| Generation | 1 in 300 chunks, ≥ 256 from spawn, ≥ 384 from another spire |
| Tier | < 600 blocks from spawn: 1; < 1200: 2; otherwise 3 |
| Raid influence | +3 budget per tier within 384 blocks |
| Night guards | 1 + tier, while a player is within 32 blocks |
| Razing | 5 × tier Duskglass |
