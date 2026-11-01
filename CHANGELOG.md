# Changelog

LDTD versions are dated (`YYYY.MM.DD`). Every push to `main` also makes a pre-release.

## Unreleased

### New
- **Flare Tower** (anti-air): fires flares at flying Gloam only. A hit also leaves the target lit
  for 4 seconds, so every other tower hits it harder. Blueprint: paper, gunpowder and a torch.
- **Lantern Post tiers:** Lantern Posts now upgrade like other towers. Tiers II and III add an aura
  (8 and 12 blocks) that lights every Gloam inside it, whatever the block light.
- **Darkness around Dusk Spires:** no Gloam within 32 blocks of a standing spire counts as lit
  (`spires.darknessRadius`). Assaults on a spire are fought at the Gloam's full strength.
- **A fallen Hearth looks cold:** an ash-filled texture, and it gives off no light until relit.

### Changed
- **Guard Post range now works:** Ember Guards fight within their post's tier-scaled range (12,
  13.8, 15.6 blocks) instead of a fixed 14, and guards already out pick up an upgrade at once.
- **Tower cores are protected:** only the territory's owner, players they trust and operators can
  change a tower's aim or upgrade it. Relighting a fallen Hearth is still open to anyone.

### Fixed
- **New blueprints reach existing servers.** `blueprints.json` now records the default blueprints
  it has seen (`_defaultsSeen`). A blueprint added by an update is added to the file once; one an
  owner deletes stays deleted. Files from the first release are treated as having seen its set.

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
