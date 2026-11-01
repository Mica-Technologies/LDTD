# Mod page copy (CurseForge / Modrinth)

Ready to paste when the projects are created. Project IDs then go in `buildscript.properties`
(`modrinthProjectId`, `curseForgeProjectId`), and the API tokens go in GitHub secrets, never in
the repository.

- **Name:** LDTD: Limitless Development: Tower Defense
- **Summary (one line):** Survival tower defense: claim land with a Hearth, build towers from
  blueprints, hold off the Gloam each night, and raze their spires.
- **Categories:** Adventure, Mobs, Technology (Modrinth: adventure, mobs, game-mechanics)
- **License:** MIT
- **Source:** https://github.com/Mica-Technologies/LDTD
- **Wiki:** https://mica-technologies.github.io/LDTD/ (Modrinth "Wiki" link, CurseForge "Wiki" link)
- **Loader / version:** Forge, Minecraft 1.12.2. Client and server (it's needed on both).
- **Logo:** `docs/images/logo.png`. **Gallery:** `docs/images/*.png`, and the wiki's screenshots in `docs/assets/images/`.

## Description

Tower defense, played in your survival world.

**Claim your land.** A Hearth claims a territory and holds a stockpile. Hearth-sprites live there
and build whatever you order.

**Build your defenses.** Hold a blueprint to see a ghost of the build, then let the sprites raise
it block by block: Archer, Cannon, Frost and Lantern towers, Guard Posts with Ember Guards you
command, and stone walls. Upgrade towers through three tiers.

**Survive the night.** When you're near, the Gloam gather at your border and march on your
Hearth's flame. They route around your walls, and break through them when they must, but only
inside a territory, so the rest of the world is safe. Light weakens them. Every raid survived pays
Duskglass, which grows your Hearth and upgrades your towers.

**Strike back.** Dusk Spires rise in the wilds and feed the raids. Raze them, and the Eclipse
comes for you.

Features:
- Six original enemy units and a boss
- Five tower types, three tiers each, plus walls
- Raids that scale with every night survived
- Villages with unclaimed Hearths to take up
- Territory protection, with trust for friends
- Everything tunable in JSON, reloadable in game
- An in-game guide book, and a full wiki: https://mica-technologies.github.io/LDTD/

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
