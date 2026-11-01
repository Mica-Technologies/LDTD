# Contributing

LDTD is open source under the [MIT License](https://github.com/Mica-Technologies/LDTD/blob/main/LICENSE),
and contributions are welcome: bug reports, balance feedback, translations, art, code and fixes
to this wiki.

<div class="grid cards" markdown>

-   :material-hammer-wrench: **[Building from source](building.md)**

    ---

    The toolchain, the Gradle tasks, and running a dev client or server.

-   :material-sitemap: **[How LDTD is built](architecture.md)**

    ---

    The packages, the rules that keep a dedicated server safe, and where each feature lives.

-   :material-scale-balance: **[Balance simulator](balance-simulator.md)**

    ---

    The offline raid simulator that checks every balance change.

-   :material-shield-check: **[Originality rules](originality.md)**

    ---

    LDTD is a public release: what that means for names, art and code.

</div>

## Editing this wiki

Every page is a Markdown file under
[`docs/`](https://github.com/Mica-Technologies/LDTD/tree/main/docs) in the mod's repository. The
:material-pencil: icon at the top of each page opens it on GitHub, so a correction is an ordinary
pull request. The site is rebuilt and published automatically when a change to `docs/` reaches
`main`.

To preview it locally:

```bash
python -m pip install -r docs/requirements.txt
mkdocs serve
```

Then open <http://127.0.0.1:8000>. `mkdocs build --strict` is what CI runs: it fails on a broken
link or a missing page.

### Screenshots

Screenshots live in `docs/assets/images/` as JPGs, about 1280 pixels wide. Take them with the HUD
hidden (++f1++) unless the HUD is the point, and with clouds off. Every image needs a row in
[`docs/ASSET_PROVENANCE.md`](../ASSET_PROVENANCE.md).

The item and block icons in `docs/assets/images/icons/` are generated from LDTD's own textures by
`py tools/gen_wiki_icons.py`. Re-run it after a texture changes.
