# Originality rules

LDTD is a **public, MIT-licensed release**. Every name, asset and line of code in it must be
original or compatibly licensed. These rules apply to every contribution.

## Names

- Every name comes from the [naming glossary](../GLOSSARY.md). Add it there, after checking it
  doesn't clash with vanilla Minecraft, Minecraft Legends or other well-known games, **before**
  using it anywhere.
- **No Minecraft Legends names or designs.** That includes allay, piglin, prismarine as a
  currency, and its named structures and heroes, and look-alikes of its units.
- **No other tower-defense franchise names or designs** (Bloons, Kingdom Rush, Orcs Must Die and
  the rest). Generic genre terms, like "archer tower", "cannon" or "wave", are fine.
- "Minecraft" never appears in the mod's name or logo.

## Assets

- **No Mojang or Microsoft assets in the jar.** No copied vanilla textures, models, sounds or
  lang text. Referring to vanilla resources at runtime (a vanilla sound event, a vanilla parent
  model) is fine, because nothing is redistributed.
- Every asset gets a row in the [asset provenance ledger](../ASSET_PROVENANCE.md) in the same
  commit that adds it.
- Allowed: original work, script-generated, CC0, CC-BY (credited in `CREDITS.md`), and
  AI-generated **only** when disclosed as the [art direction](../design/ART_DIRECTION.md)
  describes. Not allowed: NC, ND, SA, "free for personal use", or unknown origin.
- Prefer script-generated or hand-made art for anything iconic: the Hearth, the Gloam masks and
  the logo.

## Code

- **Clean-room.** Don't paste from other mods unless their license is MIT-compatible, and
  credit it. Third-party source kept for study goes in `example-source/` (gitignored), for ideas
  only. GPL and all-rights-reserved mods aren't reference material for code.
- New dependencies: check the license, record it in `CREDITS.md`, and prefer soft dependencies.

## The disclaimer

The README, the mod page and this wiki carry:

> NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
