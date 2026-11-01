# Asset provenance ledger

LDTD is a public, MIT-licensed release. **Every** asset file in `src/main/resources/assets/ldtd/`
(textures, models, sounds, lang text, structure templates) gets a row here **in the same commit
that adds it**.

## What is allowed

- **Original work** made for LDTD.
- **CC0 / public domain.**
- **CC-BY**, with the credit also added to [CREDITS.md](https://github.com/Mica-Technologies/LDTD/blob/main/CREDITS.md).
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
| `textures/entity/gloam_flicker.png`, `textures/entity/gloam_flicker_eyes.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | Shade layout (`client/model/ModelShade.java`) |
| `textures/entity/gloam_moth_wraith.png`, `textures/entity/gloam_moth_wraith_eyes.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | Atlas matches `client/model/ModelMothWraith.java` |
| `textures/entity/gloam_dimcaster.png`, `textures/entity/gloam_dimcaster_eyes.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | Atlas matches `client/model/ModelDimcaster.java` |
| `textures/entity/dark_bolt.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | |
| `textures/blocks/tower_core_{archer,cannon,frost,lantern}_{side,top}.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | |
| `textures/items/blueprint.png`, `textures/entity/cannon_shell.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | |
| `blockstates/tower_core.json`, `models/block/tower_core_*.json`, `models/item/blueprint.json` | original | Mica Technologies | Original (parents `block/cube_bottom_top`, `item/generated`, referenced) | MIT | |
| `recipes/blueprint_*.json` | original | Mica Technologies | Original | MIT | |
| `textures/entity/hearth_sprite.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | Atlas matches `client/model/ModelHearthSprite.java` |
| `textures/blocks/tower_core_{archer,cannon,frost}_side_t{2,3}.png`, `textures/items/duskglass.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | |
| `models/block/tower_core_*_t{2,3}.json`, `models/item/duskglass.json` | original | Mica Technologies | Original | MIT | |
| `textures/blocks/tower_core_guardpost_*.png`, `textures/entity/ember_guard.png`, `textures/items/command_brand.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | Ember Guard atlas uses the `ModelShade` layout |
| `models/block/tower_core_guardpost*.json`, `models/item/command_brand.json`, `recipes/blueprint_guard_post.json` | original | Mica Technologies | Original (parents `block/cube_bottom_top`, `item/handheld`, referenced) | MIT | |
| `textures/blocks/dusk_stone.png`, `textures/blocks/spire_core.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | |
| `blockstates/{dusk_stone,spire_core}.json`, `models/{block,item}/{dusk_stone,spire_core}.json` | original | Mica Technologies | Original (parent `block/cube_all`, referenced) | MIT | |
| `textures/entity/gloam_eclipse.png`, `textures/entity/gloam_eclipse_eyes.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | Atlas matches `client/model/ModelEclipse.java` |
| `recipes/command_brand.json` | original | Mica Technologies | Original | MIT | |
| `mcmod.info`, `pack.mcmeta` | original | Mica Technologies | Original | MIT | Mod metadata |
| `ldtd_logo.png` (jar root), `docs/images/logo.png` | script-generated | Mica Technologies | `tools/gen_logo.py` | MIT | Original pixel art, no fonts or third-party art |
| `docs/images/{towers_all,raid_active,roster,village,natural_spire}.png` | original | Mica Technologies | In-game screenshots of LDTD | MIT (LDTD content) | Docs only, not in the jar. They show vanilla Minecraft terrain around LDTD's content, as any screenshot of the game does |
| `docs/assets/images/*.jpg` (wiki screenshots) | original | Mica Technologies | In-game screenshots of LDTD, taken in a dev client through MCMCP | MIT (LDTD content) | Wiki only, not in the jar. Like any screenshot of the game, they show vanilla terrain and vanilla screens (the chest screen, the book screen, the boss bar) around LDTD's content |
| `docs/assets/images/icons/*.png`, `docs/assets/images/hearth-icon.png` | script-generated | Mica Technologies | `tools/gen_wiki_icons.py`, from LDTD's own textures above | MIT | Wiki only. Isometric block renders and upscaled item icons |
| `docs/assets/images/logo.png` | script-generated | Mica Technologies | Copy of `docs/images/logo.png` (`tools/gen_logo.py`) | MIT | Wiki copy of the logo |
| `docs/assets/ldtd.css` | original | Mica Technologies | Original | MIT | Wiki theme, colours from `design/ART_DIRECTION.md` |
| `textures/blocks/hearth_cold_{side,top}.png`, `models/block/hearth_cold.json`, `blockstates/hearth.json` (lit/cold variants) | script-generated / original | Mica Technologies | `tools/gen_textures.py`; models and blockstates original (parent `block/cube_bottom_top`, referenced) | MIT | The fallen Hearth's look |
| `textures/blocks/tower_core_flare_{side,side_t2,side_t3,top}.png`, `textures/blocks/tower_core_lantern_side_{t2,t3}.png` | script-generated | Mica Technologies | `tools/gen_textures.py` | MIT | Flare Tower core; Lantern Post tiers |
| `models/block/tower_core_flare{,_t2,_t3}.json`, `models/block/tower_core_lantern_{t2,t3}.json`, `blockstates/tower_core.json` (flare and lantern tier variants), `recipes/blueprint_flare_tower.json` | original | Mica Technologies | Original (parent `block/cube_bottom_top`, referenced) | MIT | |
