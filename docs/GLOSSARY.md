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
| Duskglass | The raid currency, dropped by the Gloam | `ldtd:duskglass` | Spent on upgrades and unlocks (D5) |
| Stockpile | The Hearth's material store | (concept) | Real items: wood, stone, iron and so on |

## The Gloam (enemy faction)

| Name | Archetype | Registry id | Role / counter |
|---|---|---|---|
| Shade | Grunt | `ldtd:gloam_shade` | Masked footsoldier. Countered by anything |
| Flicker | Runner | `ldtd:gloam_flicker` | Blinks short distances. Countered by slows and area light |
| Smother | Siege | `ldtd:gloam_smother` | Hulking. Snuffs light sources and claws walls. Countered by single-target DPS |
| Moth-wraith | Flyer | `ldtd:gloam_moth_wraith` | Ignores the ground route and walls, steering towards the brightest light nearby. Lanterns can lure it into a kill zone. Countered by anti-air |
| Dimcaster | Ranged | `ldtd:gloam_dimcaster` | Throws darkness bolts (`ldtd:dark_bolt`, "Darkness Bolt") that hurt and weaken. Countered by range and walls |
| The Eclipse | Boss | `ldtd:gloam_eclipse` | Dims the whole territory while it lives |
| Dusk Spire | Outpost | `ldtd:dusk_spire` (structure) | Black obelisk that spreads a dimming zone. Raid source (D6) |

Registry ids carry a `gloam_` prefix so the faction's entities sort together and can't collide
with a generic word that another mod also registers.

## Defenses (starting set)

| Name | Role | Registry id |
|---|---|---|
| Archer tower | Single target | `ldtd:tower_archer` |
| Cannon tower | Splash | `ldtd:tower_cannon` |
| Frost tower | Slow | `ldtd:tower_frost` |
| Lantern tower | Support. Light aura that weakens the Gloam | `ldtd:tower_lantern` |
| Wall / gate | Barrier | `ldtd:wall_*`, `ldtd:gate_*` |

Generic genre terms ("archer tower", "cannon") are fine and deliberately plain.

## The light mechanic

The Gloam's defining trait, and the reason the faction fits night raids:

- **Light weakens them.** A Gloam standing at block light ≥ the configured threshold is slowed
  and takes extra damage. In darkness it is at full strength.
- **They hunt light.** Smothers snuff light sources inside a territory (they are blocks, so the D4
  territory rule applies). Moth-wraiths dive at them.
- **Light is a defense.** Lanterns, the Lantern tower and a lit-up base are real defensive choices.
- **Darkness spreads from Dusk Spires.** The land around a spire is dimmer, and raids from a
  stronger spire are bigger.

## Clash notes (2026-11-01)

- **Gloam:** an ordinary English word (Scots for twilight). *The Elder Scrolls Online* uses it as
  an adjective on Daedra names (Gloam Knight, Gloam Hag, Gloam Crone, Gloam Wolf, Gloam Strider,
  Gloam Minotaur). **Never name a unit "Gloam &lt;creature&gt;"**, and none of ours are. No Minecraft
  mod with a "Gloam" mob was found.
- **Hexer** was rejected for the ranged unit (too close to *The Witcher*). It is now Dimcaster.
- **Duskglass, Dusk Spire, Hearth-sprite:** no matches in any game found.
- **Hearth:** a generic word. Never pair it with "-stone" (Hearthstone is a Blizzard trademark).
- **Allay, piglin, prismarine-as-currency** and Legends' named structures and heroes are banned
  outright (CLAUDE.md).
