# LDTD glossary

The single source of truth for LDTD's names. Code identifiers, registry names, lang text and docs
all use the names here. Add a name here **before** you use it anywhere else.

Every name was checked for clashes with vanilla Minecraft, Minecraft Legends and other well-known
games before it was adopted (see "Clash notes" below). LDTD is a public release, so don't
introduce a name without that check.

## The setting, in one paragraph

Every settlement keeps a **Hearth**: a fire that claims the land around it. The **Gloam** are what
lives in the dark between settlements. They are shadow given shape, bone-white masks over
smoke, and they hate light the way fire hates water. At night they come out of their **Dusk
Spires** to put the Hearths out. Each Hearth has its **Hearth-sprites**, small ember-spirits that
build and mend its defenses. When a Gloam is destroyed, its shadow hardens into **Duskglass**,
which a Hearth can burn to grow stronger.

## Core terms

| Name | What it is | Registry / code id | Notes |
|---|---|---|---|
| Hearth | The territory core block | `ldtd:hearth` | Holds the stockpile, raid state and territory |
| Territory | The area a Hearth claims | (concept) | The Gloam may only damage blocks inside one (D4) |
| Raid | One night's (or a called) attack on a Hearth | (concept) | "Wave" is the internal term for one spawn group inside a raid |
| Call the raid | Ringing the Hearth to start a raid early, for a bonus | (concept) | D7 |
| Hearth-sprite | Builder/repair helper | `ldtd:hearth_sprite` | Floating ember-spirit with little tongs. Hides in the Hearth during raids |
| Blueprint | Item that places a planned structure for sprites to build | `ldtd:blueprint` | Variants by structure |
| Duskglass | The raid currency, dropped by the Gloam and paid out for raids survived | `ldtd:duskglass` | Fed to the Hearth to raise its level, and to a tower's core to raise its tier (I-III) |
| Stockpile | The Hearth's material store | (concept) | Real items: wood, stone, iron and so on |

## The Gloam (enemy faction)

| Name | Archetype | Registry id | Role / counter |
|---|---|---|---|
| Shade | Grunt | `ldtd:gloam_shade` | Masked footsoldier. Countered by anything |
| Flicker | Runner | `ldtd:gloam_flicker` | Blinks short distances. Countered by slows and area light |
| Smother | Siege | `ldtd:gloam_smother` | Hulking. Snuffs light sources and claws walls. Countered by single-target DPS |
| Moth-wraith | Flyer | `ldtd:gloam_moth_wraith` | Ignores the ground route and walls, steering towards the brightest light nearby. Lanterns can lure it into a kill zone. Countered by the Flare tower (anti-air) |
| Dimcaster | Ranged | `ldtd:gloam_dimcaster` | Throws darkness bolts (`ldtd:dark_bolt`, "Darkness Bolt") that hurt and weaken. Countered by range and walls |
| The Eclipse | Boss | `ldtd:gloam_eclipse` | Dims the whole territory while it lives |
| Dusk Spire | Outpost | structure of `ldtd:dusk_stone` around a `ldtd:spire_core` | Generates in the world, guarded at night; feeds and aims nearby raids. Breaking the core razes it |

Registry ids carry a `gloam_` prefix so the faction's entities sort together and can't collide
with a generic word that another mod also registers.

## Defenses

Every tower is a `ldtd:tower_core` block of the given kind, inside the structure its blueprint builds.

| Name | Role | Core kind / blueprint |
|---|---|---|
| Archer tower | Single target | `archer` / `archer_tower` |
| Cannon tower | Splash | `cannon` / `cannon_tower` |
| Frost tower | Slow | `frost` / `frost_tower` |
| Lantern post | Support. Light weakens the Gloam; tiers II and III add an aura that lights every Gloam near it | `lantern` / `lantern_post` |
| Guard post | Keeps a squad of Ember Guards | `guardpost` / `guard_post` |
| Flare tower | Anti-air. Fires flares at flying Gloam; a hit leaves the target lit | `flare` / `flare_tower` |
| Stone wall | Barrier | blueprint `stone_wall` |

## Villages (Phase 9)

| Name | What it is |
|---|---|
| Hearth Square | A village plaza with an unclaimed Hearth at its centre and room for every growth stage. Sneak-use claims it |
| Watch Post | A village's ready-built archer tower |

## Defenders (Phase 8)

| Name | What it is | Registry id |
|---|---|---|
| Ember Guard | A stone-and-ember warrior kept by a guard post. Holds a spot, follows, attacks on command | `ldtd:ember_guard` |
| Command Brand | A smouldering baton that gives Ember Guards their orders | `ldtd:command_brand` |

Generic genre terms ("archer tower", "cannon") are fine and deliberately plain.

## The light mechanic

The Gloam's defining trait, and the reason the faction fits night raids:

- **Light weakens them.** A Gloam standing at block light ≥ the configured threshold is slowed
  and takes extra damage. In darkness it is at full strength.
- **They hunt light.** Smothers snuff light sources inside a territory (they are blocks, so the D4
  territory rule applies). Moth-wraiths dive at them.
- **Light is a defense.** Lanterns, the Lantern tower and a lit-up base are real defensive choices.
- **Light can be carried.** A Flare tower's hit, and a tier II+ Lantern post's aura, light a Gloam
  whatever the block light around it.
- **Darkness spreads from Dusk Spires.** No Gloam near a standing spire counts as lit, however
  bright it is (`spires.darknessRadius`, 32 blocks), and raids from a
  stronger spire are bigger.

## Clash notes (2026-11-01)

- **Gloam:** an ordinary English word (Scots for twilight). *The Elder Scrolls Online* uses it as
  an adjective on Daedra names (Gloam Knight, Gloam Hag, Gloam Crone, Gloam Wolf, Gloam Strider,
  Gloam Minotaur). **Never name a unit "Gloam &lt;creature&gt;"**, and none of ours are. No Minecraft
  mod with a "Gloam" mob was found.
- **Flare tower** (checked 2026-11-01): no game, franchise or Minecraft mod with a "Flare Tower" was
  found. *Tower Defense Simulator* (Roblox) had an *enemy* called Flare in a removed event, and a
  Minecraft mod adds flare guns; neither is a tower, and "flare" is an ordinary word.
- **Hexer** was rejected for the ranged unit (too close to *The Witcher*). It is now Dimcaster.
- **Duskglass, Dusk Spire, Hearth-sprite:** no matches in any game found.
- **Hearth:** a generic word. Never pair it with "-stone" (Hearthstone is a Blizzard trademark).
- **Allay, piglin, prismarine-as-currency** and Legends' named structures and heroes are banned
  outright (CLAUDE.md).
