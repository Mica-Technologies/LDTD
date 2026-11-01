# Changelog

LDTD versions are dated (`YYYY.MM.DD`). Every push to `main` also makes a pre-release.

## First full release

The first full release of LDTD for Minecraft 1.12.2 (Forge).

### Hearths and territory
- The Hearth claims a territory around it and holds a stockpile. Its territory border shows while
  you hold a Hearth, or always, or never (client setting).
- Five levels, raised with Duskglass and stockpile materials: wider territory, more Hearth-sprites
  and a growing hall built around the Hearth.
- Hearth-sprites build blueprints from the stockpile and repair siege damage.
- Territory protection: players the owner hasn't trusted can't build, break, empty buckets, light
  fires or open the stockpile. `/hearth trust|untrust|trusted`.
- The Flame: Gloam at the Hearth drain it. A Hearth whose Flame goes out falls dormant, loses part
  of its stockpile, and is relit with coal.
- Villages may have a Hearth Square with an unclaimed Hearth, and Watch Posts (archer towers).

### Towers
- Blueprints with ghost previews for an Archer Tower, Cannon Tower, Frost Tower, Lantern Post,
  Guard Post and Stone Wall.
- Three tiers per tower, bought with Duskglass. Salvage refunds half.
- Target priorities: first, strongest or closest.
- Ember Guards from the Guard Post, ordered with the Command Brand.

### The Gloam and raids
- Six original units: Shade, Smother, Flicker, Moth-wraith, Dimcaster and the Eclipse.
- Light weakens them: lit Gloam take more damage and move slower.
- Night raids when a defender is near, or called early at the Hearth for more reward. The
  direction is announced, three waves come, there's a boss bar, and Duskglass is paid for
  surviving.
- Shared routes around your walls, and a siege when walls block the way, only ever inside a
  territory.
- Raid budgets grow with nights survived and Hearth level, and late raids send tougher Gloam.

### Offense
- Dusk Spires generate in the world, guarded at night. They feed and aim the raids on nearby
  Hearths. Breaking a spire's core razes it.
- The Eclipse joins the raids on a Hearth once three spires near it have fallen.

### Quality of life
- *The Hearthkeeper's Guide*, given on first join, and through `/hearth guide`.
- Item tooltips, health bars over wounded Gloam, and sound cues for raids.
- `/randomspawn` (`/rs`).
- All balance numbers are JSON under `config/ldtd/`, reloadable, never fatal when broken, and
  brought up to date automatically after an update.
