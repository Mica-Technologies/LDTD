#!/usr/bin/env python3
"""Generate LDTD's block textures as 16x16 PNGs.

All art here is original and drawn from code, so it can be reproduced and reviewed (see
docs/design/ART_DIRECTION.md for the palettes and docs/ASSET_PROVENANCE.md for the ledger).
No image library is needed: the PNG writer below uses only the standard library.

Run:  py tools/gen_textures.py            (writes into src/main/resources/assets/ldtd/textures)
      py tools/gen_textures.py --preview  (also writes 16x-scaled previews to tools/preview/)
"""

import os
import random
import struct
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "ldtd", "textures")
PREVIEW = os.path.join(ROOT, "tools", "preview")

# Hearth-side palette (ART_DIRECTION.md).
EMBER = (0xFF, 0x9A, 0x3C)
EMBER_HI = (0xFF, 0xD2, 0x7A)
SOOT = (0x2B, 0x25, 0x22)
BRASS = (0xC9, 0xA0, 0x4A)
MASONRY = (0x7A, 0x72, 0x6B)


def write_png(path, pixels, scale=1):
    """Write a list of rows of (r, g, b, a) tuples as an RGBA PNG."""
    h = len(pixels) * scale
    w = len(pixels[0]) * scale
    raw = bytearray()
    for row in pixels:
        line = bytearray([0])
        for px in row:
            line.extend(bytes(px) * scale)
        for _ in range(scale):
            raw.extend(line)

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def shade(c, d):
    """Lighten (d > 0) or darken (d < 0) a colour by d steps of 10."""
    return tuple(max(0, min(255, v + d * 10)) for v in c)


def rgba(c, a=255):
    return (c[0], c[1], c[2], a)


def masonry(rng):
    """A 16x16 grid of grey stone bricks with darker mortar, offset every other course."""
    px = [[None] * 16 for _ in range(16)]
    for y in range(16):
        course = y // 4
        offset = 4 if course % 2 else 0
        for x in range(16):
            mortar = y % 4 == 3 or (x + offset) % 8 == 7
            if mortar:
                px[y][x] = rgba(shade(MASONRY, -4))
            else:
                px[y][x] = rgba(shade(MASONRY, rng.choice((-1, 0, 0, 1))))
    return px


def hearth_side(rng):
    px = masonry(rng)
    # An arched fire opening, cols 4-11, rows 6-14, with a soot-stained lintel above it.
    for y in range(5, 15):
        for x in range(3, 13):
            inside = 4 <= x <= 11 and y >= 7
            arch = 5 <= x <= 10 and y == 6
            if inside or arch:
                depth = (y - 6) / 8.0
                if y >= 13:
                    c = EMBER_HI if rng.random() < 0.6 else EMBER
                elif y >= 10:
                    c = EMBER if rng.random() < 0.7 else EMBER_HI
                else:
                    c = shade(SOOT, int(depth * 3))
                    if rng.random() < depth * 0.6:
                        c = shade(EMBER, -6)
                px[y][x] = rgba(c)
            elif y == 5 and 4 <= x <= 11 or (x in (3, 12) and y >= 7):
                px[y][x] = rgba(shade(SOOT, 2))
    # A brass sill under the opening.
    for x in range(3, 13):
        px[15][x] = rgba(BRASS if x not in (3, 12) else shade(BRASS, -3))
    return px


def hearth_top(rng):
    px = [[None] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            rim = x in (1, 14) or y in (1, 14)
            if edge:
                px[y][x] = rgba(shade(BRASS, -2))
            elif rim:
                px[y][x] = rgba(BRASS if (x + y) % 3 else shade(BRASS, 2))
            elif x % 3 == 0 or y % 3 == 0:
                # Iron grate bars over the fire.
                px[y][x] = rgba(shade(SOOT, rng.choice((0, 1))))
            else:
                # Embers glowing between the bars, hotter towards the middle.
                dist = abs(x - 7.5) + abs(y - 7.5)
                hot = dist < 6 and rng.random() < 0.7
                px[y][x] = rgba(EMBER_HI if hot else EMBER)
    return px


def hearth_bottom(rng):
    return masonry(rng)


# ---------------------------------------------------------------------------------------------
# Entity atlases.
#
# Each model box unwraps onto the atlas the vanilla way. A W x H x D box at texture offset (u, v):
#   top    (u+D,     v)     W x D      bottom (u+D+W,   v)     W x D
#   right  (u,       v+D)   D x H      front  (u+D,     v+D)   W x H
#   left   (u+D+W,   v+D)   D x H      back   (u+2D+W,  v+D)   W x H
# The (u, v) offsets and sizes below MUST match the ModelRenderer boxes in the Java model classes
# (client/model/ModelShade.java, client/model/ModelSmother.java).

INK = (0x14, 0x10, 0x19)
SMOKE = (0x2E, 0x24, 0x40)
VIOLET = (0x4B, 0x2E, 0x6F)
BONE = (0xE9, 0xE4, 0xD8)
EYE = (0x5C, 0xF2, 0xE6)
CLEAR = (0, 0, 0, 0)


def blank(w, h):
    return [[CLEAR] * w for _ in range(h)]


def paint_box(img, u, v, w, h, d, painter):
    """Fill each face of a box. painter(face, x, y, fw, fh) returns an RGBA tuple."""
    faces = {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + 2 * d + w, v + d, w, h),
    }
    for face, (fu, fv, fw, fh) in faces.items():
        for y in range(fh):
            for x in range(fw):
                img[fv + y][fu + x] = painter(face, x, y, fw, fh)


def cloth(rng, base=INK, fold=SMOKE, edge=VIOLET):
    """Dark cloth with vertical folds and a violet sheen near the top edge."""
    def p(face, x, y, fw, fh):
        if y == 0 and face not in ("top", "bottom"):
            return rgba(edge)
        if x % 3 == 1 and rng.random() < 0.7:
            return rgba(fold)
        return rgba(shade(base, rng.choice((0, 0, 1))))
    return p


def ragged(rng, inner, rows=3):
    """Wrap a painter so the bottom rows of the side faces fray into nothing."""
    def p(face, x, y, fw, fh):
        if face not in ("top", "bottom") and y >= fh - rows and rng.random() < (y - (fh - rows) + 1) / (rows + 1.0):
            return CLEAR
        return inner(face, x, y, fw, fh)
    return p


def mask_face(rng, eye_cols, eye_row, hood):
    """A blank bone mask on the front face with dark eye slits. Other faces are the hood."""
    def p(face, x, y, fw, fh):
        if face == "front":
            if 0 < x < fw - 1 and 0 < y < fh - 1:
                if y == eye_row and x in eye_cols:
                    return rgba(INK)
                return rgba(shade(BONE, rng.choice((-1, 0, 0))))
            return rgba(VIOLET)
        return hood(face, x, y, fw, fh)
    return p


def eyes_only(eye_cols, eye_row):
    """The full-bright eye layer: cyan where the mask's eye slits are, nothing anywhere else."""
    def p(face, x, y, fw, fh):
        if face == "front" and y == eye_row and x in eye_cols:
            return rgba(EYE)
        return CLEAR
    return p


def shade_atlas(rng, eyes=False):
    img = blank(64, 64)
    eye_cols, eye_row = (1, 2, 5, 6), 3
    if eyes:
        paint_box(img, 0, 0, 8, 8, 8, eyes_only(eye_cols, eye_row))
        return img
    paint_box(img, 0, 0, 8, 8, 8, mask_face(rng, eye_cols, eye_row, cloth(rng)))       # head
    paint_box(img, 16, 16, 8, 12, 4, ragged(rng, cloth(rng)))                          # body
    paint_box(img, 40, 16, 3, 14, 3, ragged(rng, cloth(rng, fold=VIOLET), rows=2))     # arm
    paint_box(img, 0, 16, 4, 12, 4, cloth(rng, base=shade(INK, -1)))                   # leg
    return img


def smother_atlas(rng, eyes=False):
    img = blank(64, 64)
    eye_cols, eye_row = (1, 4), 2
    if eyes:
        paint_box(img, 0, 0, 6, 6, 6, eyes_only(eye_cols, eye_row))
        return img
    paint_box(img, 0, 0, 6, 6, 6, mask_face(rng, eye_cols, eye_row, cloth(rng)))       # head

    def plated(face, x, y, fw, fh):
        # Heavy violet bands across the chest and shoulders: armour of hardened shadow.
        if face != "bottom" and (y in (1, 2, 6) or face == "top"):
            return rgba(shade(VIOLET, rng.choice((-1, 0))))
        return cloth(rng)(face, x, y, fw, fh)
    paint_box(img, 0, 12, 14, 14, 8, plated)                                           # body

    def arm(face, x, y, fw, fh):
        # Bone claws at the bottom of the front face.
        if face == "front" and y >= fh - 2 and x % 2 == 0:
            return rgba(BONE)
        if y >= fh - 3:
            return rgba(shade(INK, -1))
        return cloth(rng, fold=VIOLET)(face, x, y, fw, fh)
    paint_box(img, 40, 34, 6, 16, 6, arm)                                              # arm
    paint_box(img, 0, 34, 5, 10, 5, cloth(rng, base=shade(INK, -1)))                   # leg
    return img


def flicker_atlas(rng, eyes=False):
    """The Shade's layout, in thinner, smokier cloth with more violet: a quicker, fainter thing."""
    img = blank(64, 64)
    eye_cols, eye_row = (2, 5), 3
    if eyes:
        paint_box(img, 0, 0, 8, 8, 8, eyes_only(eye_cols, eye_row))
        return img
    wisp = cloth(rng, base=SMOKE, fold=VIOLET, edge=shade(VIOLET, 3))
    paint_box(img, 0, 0, 8, 8, 8, mask_face(rng, eye_cols, eye_row, wisp))             # head
    paint_box(img, 16, 16, 8, 12, 4, ragged(rng, wisp, rows=5))                        # body
    paint_box(img, 40, 16, 3, 14, 3, ragged(rng, wisp, rows=4))                        # arm
    paint_box(img, 0, 16, 4, 12, 4, ragged(rng, wisp, rows=6))                         # leg
    return img


def dimcaster_atlas(rng, eyes=False):
    """The Shade's layout plus a hood at (0, 40) 9x3x9 and an orb at (40, 40) 4x4x4."""
    img = blank(64, 64)
    eye_cols, eye_row = (1, 2, 5, 6), 3
    if eyes:
        paint_box(img, 0, 0, 8, 8, 8, eyes_only(eye_cols, eye_row))
        # The orb glows all over: a ball of cold light.
        paint_box(img, 40, 40, 4, 4, 4,
                  lambda face, x, y, fw, fh: rgba(EYE) if (x + y) % 2 == 0 else rgba(shade(EYE, -6)))
        return img
    robe = cloth(rng, base=INK, fold=VIOLET, edge=VIOLET)
    paint_box(img, 0, 0, 8, 8, 8, mask_face(rng, eye_cols, eye_row, robe))             # head
    paint_box(img, 16, 16, 8, 12, 4, ragged(rng, robe, rows=2))                        # body
    paint_box(img, 40, 16, 3, 14, 3, robe)                                             # arm
    paint_box(img, 0, 16, 4, 12, 4, cloth(rng, base=shade(INK, -1)))                   # leg
    paint_box(img, 0, 40, 9, 3, 9, cloth(rng, base=VIOLET, fold=shade(VIOLET, -2), edge=shade(VIOLET, 2)))  # hood
    paint_box(img, 40, 40, 4, 4, 4, lambda face, x, y, fw, fh: rgba(shade(INK, 1)))  # orb (lit by the eye layer)
    return img


def moth_atlas(rng, eyes=False):
    """Body (0, 0) 4x6x3, mask (16, 0) 6x6x2, wing (0, 16) 14x1x10."""
    img = blank(64, 64)
    eye_cols, eye_row = (1, 4), 2
    if eyes:
        paint_box(img, 16, 0, 6, 6, 2, eyes_only(eye_cols, eye_row))
        return img
    paint_box(img, 0, 0, 4, 6, 3, cloth(rng))                                          # body

    def mask(face, x, y, fw, fh):
        if face == "front":
            if y == eye_row and x in eye_cols:
                return rgba(INK)
            return rgba(shade(BONE, rng.choice((-1, 0, 0))))
        return rgba(VIOLET)
    paint_box(img, 16, 0, 6, 6, 2, mask)                                               # mask

    def wing(face, x, y, fw, fh):
        # Dusty violet membrane with dark veins and a frayed outer edge.
        if face in ("top", "bottom"):
            if x >= fw - 2 and rng.random() < 0.5:
                return CLEAR
            if x % 4 == 0 or y == fh // 2:
                return rgba(INK)
            return rgba(shade(SMOKE, rng.choice((0, 1, 2))))
        return rgba(INK)
    paint_box(img, 0, 16, 14, 1, 10, wing)                                             # wing
    return img


def dark_bolt(rng):
    """A 16x16 orb of darkness with a cold glint, for the billboard renderer."""
    px = [[CLEAR] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3.5:
                px[y][x] = rgba(INK)
            elif d < 5.5:
                px[y][x] = rgba(VIOLET, 230)
            elif d < 7.0 and rng.random() < 0.5:
                px[y][x] = rgba(SMOKE, 140)
    px[6][6] = rgba(EYE)
    px[6][7] = rgba(EYE)
    px[7][6] = rgba(EYE)
    return px


TEXTURES = {
    "blocks/hearth_side.png": hearth_side,
    "blocks/hearth_top.png": hearth_top,
    "blocks/hearth_bottom.png": hearth_bottom,
    "entity/gloam_shade.png": shade_atlas,
    "entity/gloam_shade_eyes.png": lambda rng: shade_atlas(rng, eyes=True),
    "entity/gloam_smother.png": smother_atlas,
    "entity/gloam_smother_eyes.png": lambda rng: smother_atlas(rng, eyes=True),
    "entity/gloam_flicker.png": flicker_atlas,
    "entity/gloam_flicker_eyes.png": lambda rng: flicker_atlas(rng, eyes=True),
    "entity/gloam_moth_wraith.png": moth_atlas,
    "entity/gloam_moth_wraith_eyes.png": lambda rng: moth_atlas(rng, eyes=True),
    "entity/gloam_dimcaster.png": dimcaster_atlas,
    "entity/gloam_dimcaster_eyes.png": lambda rng: dimcaster_atlas(rng, eyes=True),
    "entity/dark_bolt.png": dark_bolt,
}


def main():
    preview = "--preview" in sys.argv
    for name, fn in TEXTURES.items():
        # A fixed seed per texture: the same script always produces the same pixels.
        pixels = fn(random.Random(name))
        write_png(os.path.join(OUT, name), pixels)
        if preview:
            scale = 16 if len(pixels) <= 16 else 8
            write_png(os.path.join(PREVIEW, name.replace("/", "_")), pixels, scale=scale)
        print("wrote", name)


if __name__ == "__main__":
    main()
