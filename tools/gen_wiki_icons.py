"""Generate the wiki's item and block icons from LDTD's own textures.

Run from the repository root:

    py tools/gen_wiki_icons.py

Writes PNGs to docs/assets/images/icons/ (and the site logo, docs/assets/images/hearth-icon.png).
Every input is one of LDTD's original 16x16 textures under src/main/resources/assets/ldtd/textures,
so the outputs are original too. Nothing from vanilla Minecraft is read.

Blocks are drawn as a simple isometric cube (top, left side, right side, the sides shaded the way
vanilla shades block faces). Items are scaled up with nearest-neighbour sampling, so the pixel art
stays crisp.
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src" / "main" / "resources" / "assets" / "ldtd" / "textures"
OUT = ROOT / "docs" / "assets" / "images" / "icons"

SCALE = 8  # 16 px texture -> 128 px face


def load(rel):
    return Image.open(TEX / rel).convert("RGBA")


def shade(img, factor):
    px = img.copy()
    r, g, b, a = px.split()
    r = r.point(lambda v: int(v * factor))
    g = g.point(lambda v: int(v * factor))
    b = b.point(lambda v: int(v * factor))
    return Image.merge("RGBA", (r, g, b, a))


def iso_cube(top, side_left, side_right, size=128):
    """Draw a cube in a 2:1 isometric projection, pixel-exact (nearest sampling)."""
    s = size
    w = 2 * s
    h = 2 * s
    canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    top = top.resize((s, s), Image.NEAREST)
    left = shade(side_left.resize((s, s), Image.NEAREST), 0.8)
    right = shade(side_right.resize((s, s), Image.NEAREST), 0.6)

    # Each output pixel is mapped back to a texture coordinate on one face.
    out = canvas.load()
    tp, lp, rp = top.load(), left.load(), right.load()
    half = s / 2.0
    for y in range(h):
        for x in range(w):
            # Top face: the diamond (s,0) (2s,half) (s,s) (0,half). A point on it is
            # (s,0) + u*(s,half) + v*(-s,half), solved here for u and v.
            u = (x / s - 1.0 + y / half) / 2.0  # along the texture's x axis
            v = (y / half - x / s + 1.0) / 2.0  # along the texture's y axis
            if 0 <= u < 1 and 0 <= v < 1 and y < s:
                out[x, y] = tp[int(u * s), int(v * s)]
                continue
            if x < s:
                # Left face: x in [0, s), skewed down by 1/2 per pixel.
                fy = y - half - x / 2.0
                if 0 <= fy < s:
                    out[x, y] = lp[int(x), int(fy)]
                    continue
            else:
                fx = x - s
                fy = y - s + fx / 2.0
                if 0 <= fy < s:
                    out[x, y] = rp[int(fx), int(fy)]
    return canvas


def item(rel, name, size=128):
    load(rel).resize((size, size), Image.NEAREST).save(OUT / f"{name}.png")


def block(top_rel, side_rel, name):
    side = load(side_rel)
    iso_cube(load(top_rel), side, side).resize((128, 128), Image.LANCZOS).save(OUT / f"{name}.png")


def main():
    OUT.mkdir(parents=True, exist_ok=True)

    item("items/duskglass.png", "duskglass")
    item("items/blueprint.png", "blueprint")
    item("items/command_brand.png", "command_brand")

    block("blocks/hearth_top.png", "blocks/hearth_side.png", "hearth")
    block("blocks/dusk_stone.png", "blocks/dusk_stone.png", "dusk_stone")
    block("blocks/spire_core.png", "blocks/spire_core.png", "spire_core")
    for kind in ("archer", "cannon", "frost", "guardpost"):
        for suffix, tier in (("", 1), ("_t2", 2), ("_t3", 3)):
            block(f"blocks/tower_core_{kind}_top.png", f"blocks/tower_core_{kind}_side{suffix}.png",
                  f"tower_core_{kind}_t{tier}")
    block("blocks/tower_core_lantern_top.png", "blocks/tower_core_lantern_side.png", "tower_core_lantern")

    # The site logo and favicon: the Hearth block, small.
    iso_cube(load("blocks/hearth_top.png"), load("blocks/hearth_side.png"), load("blocks/hearth_side.png")) \
        .resize((96, 96), Image.LANCZOS).save(OUT.parent / "hearth-icon.png")


if __name__ == "__main__":
    main()
