# Asset provenance ledger

LDTD is a public, MIT-licensed release. **Every** asset file in `src/main/resources/assets/ldtd/`
(textures, models, sounds, lang text, structure templates) gets a row here **in the same commit
that adds it**.

## What is allowed

- **Original work** made for LDTD.
- **CC0 / public domain.**
- **CC-BY**, with the credit also added to [CREDITS.md](../CREDITS.md).
- **AI-generated**, disclosed: Kind is `AI-generated`, Source names the tool and model, and it
  meets the rules in [design/ART_DIRECTION.md](design/ART_DIRECTION.md#ai-generated-assets-allowed-disclosed-decided-2026-11-01).

## What is not allowed

- Anything copied or traced from Minecraft, Minecraft Legends, or any other game.
- CC-NC (non-commercial), CC-ND (no derivatives) or CC-SA (share-alike) assets.
- "Free for personal use", unclear licenses, or unknown origin.
- Assets from other mods, unless their license allows it and they are credited.

Referencing vanilla resources at runtime (for example, a vanilla sound event or a vanilla model
parent) is fine and does not need a row: nothing of Mojang's is redistributed in our jar.

## Ledger

| Path (under `assets/ldtd/`) | Kind (`original` / `script-generated` / `CC0` / `CC-BY` / `AI-generated`) | Author | Source | License | Notes |
|---|---|---|---|---|---|
| `lang/en_us.lang` | original | Mica Technologies | Original | MIT | |
| `textures/blocks/hearth_side.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | |
| `textures/blocks/hearth_top.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | |
| `textures/blocks/hearth_bottom.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | |
| `blockstates/hearth.json`, `models/block/hearth.json`, `models/item/hearth.json` | original | Mica Technologies | Original (parent: vanilla `block/cube_bottom_top`, referenced, not copied) | MIT | |
| `recipes/hearth.json` | original | Mica Technologies | Original | MIT | |
| `textures/entity/gloam_shade.png`, `textures/entity/gloam_shade_eyes.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | Atlas matches `client/model/ModelShade.java` |
| `textures/entity/gloam_smother.png`, `textures/entity/gloam_smother_eyes.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | Atlas matches `client/model/ModelSmother.java` |
