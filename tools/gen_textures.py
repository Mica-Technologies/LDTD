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


# ---------------------------------------------------------------------------------------------
# Towers (warm Hearth-side palette, plus a cold blue for frost).

WOOD = (0x8A, 0x63, 0x3C)
IRON = (0x8E, 0x92, 0x96)
ICE = (0xA9, 0xD8, 0xF0)
PAPER = (0x3A, 0x6E, 0xB5)
ASH = (0x8C, 0x86, 0x80)


def frame(px, c):
    for i in range(16):
        px[0][i] = px[15][i] = px[i][0] = px[i][15] = rgba(c)


def archer_side(rng):
    px = [[rgba(shade(WOOD, rng.choice((-1, 0, 0, 1)) - (2 if y % 4 == 3 else 0))) for x in range(16)]
          for y in range(16)]
    for y in range(4, 12):          # the arrow slit
        px[y][7] = px[y][8] = rgba(SOOT)
    for x in range(16):             # brass bands
        px[2][x] = px[13][x] = rgba(BRASS)
    return px


def archer_top(rng):
    px = [[rgba(shade(WOOD, rng.choice((-1, 0, 1)))) for x in range(16)] for y in range(16)]
    frame(px, BRASS)
    for i in range(3, 13):          # two crossed arrows
        px[i][i] = rgba(SOOT)
        px[i][15 - i] = rgba(SOOT)
    for (x, y) in ((3, 3), (12, 3)):
        px[y][x] = rgba(shade(IRON, 4))
    return px


def cannon_side(rng):
    px = [[rgba(shade(IRON, rng.choice((-2, -1, 0, 0)))) for x in range(16)] for y in range(16)]
    for y in range(16):
        px[y][7] = rgba(shade(IRON, -4))
    for (x, y) in ((2, 2), (12, 2), (2, 12), (12, 12), (5, 7), (10, 7)):
        px[y][x] = rgba(shade(IRON, 4))   # rivets
    for x in range(16):
        px[0][x] = px[15][x] = rgba(SOOT)
    return px


def cannon_top(rng):
    px = [[rgba(shade(IRON, rng.choice((-1, 0)))) for x in range(16)] for y in range(16)]
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 4.0:
                px[y][x] = rgba(SOOT)        # the barrel's mouth
            elif d < 5.5:
                px[y][x] = rgba(shade(IRON, -4))
    return px


def frost_side(rng):
    px = [[rgba(shade(ICE, rng.choice((-2, -1, 0, 1)))) for x in range(16)] for y in range(16)]
    for i in range(16):             # bright facet lines
        if 0 <= i // 2 + 3 < 16:
            px[i][(i // 2 + 3) % 16] = rgba((0xF4, 0xFB, 0xFF))
        px[i][(12 - i // 3) % 16] = rgba((0xE0, 0xF4, 0xFF))
    return px


def frost_top(rng):
    px = [[rgba(shade(ICE, rng.choice((-1, 0)))) for x in range(16)] for y in range(16)]
    white = rgba((0xF4, 0xFB, 0xFF))
    for i in range(2, 14):          # a six-armed flake, near enough
        px[7][i] = px[8][i] = white
        px[i][7] = px[i][8] = white
    for i in range(4, 12):
        px[i][i] = white
        px[i][15 - i] = white
    return px


def lantern_side(rng):
    px = [[rgba(EMBER_HI if rng.random() < 0.4 else EMBER) for x in range(16)] for y in range(16)]
    frame(px, BRASS)
    for y in range(16):
        px[y][7] = px[y][8] = rgba(shade(BRASS, -2))
    for x in range(16):
        px[7][x] = px[8][x] = rgba(shade(BRASS, -2))
    return px


def lantern_top(rng):
    px = [[rgba(shade(BRASS, rng.choice((-1, 0, 1)))) for x in range(16)] for y in range(16)]
    for y in range(6, 10):
        for x in range(6, 10):
            px[y][x] = rgba(EMBER_HI)
    return px


def hearth_cold_side(rng):
    """A fallen Hearth: the same masonry and arch, but the fire is out. Ash and dead coals."""
    px = hearth_side(random.Random("blocks/hearth_side.png"))
    for y in range(6, 15):
        for x in range(4, 12):
            if y == 6 and not 5 <= x <= 10:
                continue
            if y >= 12:
                c = shade(ASH, rng.choice((-1, 0, 1))) if rng.random() < 0.8 else shade(SOOT, 1)
            else:
                c = shade(SOOT, rng.choice((-1, 0, 0, 1)))
            px[y][x] = rgba(c)
    for x in range(3, 13):
        px[15][x] = rgba(shade(BRASS, -5) if x not in (3, 12) else shade(BRASS, -7))
    return px


def hearth_cold_top(rng):
    """The grate over a dead fire: tarnished rim, grey ash between the bars."""
    px = hearth_top(random.Random("blocks/hearth_top.png"))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            rim = x in (1, 14) or y in (1, 14)
            if edge or rim:
                px[y][x] = rgba(shade(BRASS, -6 if edge else -4))
            elif not (x % 3 == 0 or y % 3 == 0):
                px[y][x] = rgba(shade(ASH, rng.choice((-2, -1, 0, 1))))
    return px


def flare_side(rng):
    """The Flare Tower's core: dark iron, a tall brass-rimmed launch tube with an ember tip."""
    px = [[rgba(shade(SOOT, rng.choice((0, 1, 1, 2)))) for x in range(16)] for y in range(16)]
    frame(px, shade(BRASS, -2))
    for y in range(2, 14):
        for x in (6, 9):
            px[y][x] = rgba(BRASS)
        for x in (7, 8):
            px[y][x] = rgba(shade(IRON, -3))
    for x in (6, 7, 8, 9):
        px[1][x] = rgba(EMBER_HI)
        px[2][x] = rgba(EMBER)
    for (x, y) in ((3, 5), (12, 5), (3, 10), (12, 10)):
        px[y][x] = rgba(shade(IRON, 3))   # rivets
    return px


def flare_top(rng):
    """Looking down the launch tube: a ring of brass around a glowing flare."""
    px = [[rgba(shade(SOOT, rng.choice((0, 1)))) for x in range(16)] for y in range(16)]
    frame(px, shade(BRASS, -2))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 2.2:
                px[y][x] = rgba(EMBER_HI)
            elif d < 3.6:
                px[y][x] = rgba(EMBER)
            elif d < 5.0:
                px[y][x] = rgba(BRASS)
    return px


def blueprint_item(rng):
    px = [[CLEAR] * 16 for _ in range(16)]
    for y in range(2, 14):
        for x in range(2, 14):
            px[y][x] = rgba(shade(PAPER, rng.choice((-1, 0, 0))))
    for y in range(2, 14):          # rolled ends
        px[y][1] = px[y][14] = rgba(shade(PAPER, -4))
    white = rgba((0xDD, 0xE8, 0xF5))
    for x in range(4, 12):          # a tower drawn in white lines
        px[11][x] = white
    for y in range(5, 12):
        px[y][5] = px[y][10] = white
    for x in range(5, 11):
        px[5][x] = white
    px[4][5] = px[4][7] = px[4][10] = white
    return px


def cannon_shell(rng):
    px = [[CLEAR] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 8.5) ** 2) ** 0.5
            if d < 5.0:
                px[y][x] = rgba(shade(SOOT, 1 if d < 2.5 and x < 8 and y < 8 else 0))
    px[2][9] = px[3][9] = rgba(EMBER)     # the lit fuse
    px[1][10] = rgba(EMBER_HI)
    return px


def sprite_atlas(rng):
    """Body (0, 0) 4x4x4, cap (0, 8) 6x2x6, arm (24, 0) 1x3x1, on a 32x32 atlas."""
    img = blank(32, 32)

    def ember(face, x, y, fw, fh):
        hot = (x + y) % 3 == 0 or rng.random() < 0.3
        if face == "front" and y == 1 and x in (1, 2):
            return rgba(SOOT)                                  # two little eyes
        return rgba(EMBER_HI if hot else EMBER)
    paint_box(img, 0, 0, 4, 4, 4, ember)
    paint_box(img, 0, 8, 6, 2, 6, lambda face, x, y, fw, fh: rgba(shade(SOOT, rng.choice((0, 1, 2)))))
    paint_box(img, 24, 0, 1, 3, 1, lambda face, x, y, fw, fh: rgba(BRASS if y < 2 else shade(BRASS, -3)))
    return img


GOLD = (0xE3, 0xB2, 0x3C)


def tiered(fn, tier):
    """A tower core's side texture with its tier marked: one gold band at II, two and a gem at III."""
    def paint(rng):
        px = fn(rng)
        rows = (7, 8) if tier == 2 else (3, 4, 11, 12)
        for y in rows:
            for x in range(16):
                px[y][x] = rgba(shade(GOLD, -1 if x % 4 == 0 else 0))
        if tier == 3:
            for y in (7, 8):
                for x in (7, 8):
                    px[y][x] = rgba(EYE)
        return px
    return paint


def duskglass(rng):
    """A shard of hardened shadow: violet-black glass with a cold cyan glint."""
    px = [[CLEAR] * 16 for _ in range(16)]
    shard = [(7, 1), (8, 1), (6, 2), (9, 2)]
    for y in range(3, 14):
        half = 1 + min(y - 1, 14 - y) // 2
        for x in range(8 - half, 8 + half):
            shard.append((x, y))
    for (x, y) in shard:
        px[y][x] = rgba((0x2A, 0x1C, 0x3D) if (x + y) % 3 else VIOLET)
    for (x, y) in ((7, 4), (7, 5), (8, 6), (6, 9)):
        px[y][x] = rgba(EYE)
    return px


STONE = (0x8A, 0x86, 0x80)


def guardpost_side(rng):
    px = [[rgba(shade(STONE, rng.choice((-1, 0, 0, 1)) - (2 if y % 5 == 4 else 0))) for x in range(16)]
          for y in range(16)]
    for y in range(3, 13):                 # a crossed sword and shield, in brass
        px[y][7] = px[y][8] = rgba(BRASS)
    for x in range(4, 12):
        px[5][x] = rgba(BRASS)
    for y in range(8, 13):
        for x in range(5, 11):
            if abs(x - 7.5) + (y - 8) * 0.6 < 3.5:
                px[y][x] = rgba(EMBER if (x + y) % 2 else EMBER_HI)
    return px


def guardpost_top(rng):
    px = [[rgba(shade(STONE, rng.choice((-1, 0, 1)))) for x in range(16)] for y in range(16)]
    frame(px, BRASS)
    for y in range(6, 10):
        for x in range(6, 10):
            px[y][x] = rgba(EMBER_HI)
    return px


def ember_guard_atlas(rng):
    """The Shade's layout (client/model/ModelShade.java), dressed as a stone warrior with an ember heart."""
    img = blank(64, 64)

    def stone(face, x, y, fw, fh):
        return rgba(shade(STONE, rng.choice((-2, -1, 0, 0, 1))))

    def head(face, x, y, fw, fh):
        if face == "front" and y == 3 and x in (1, 2, 5, 6):
            return rgba(EMBER_HI)              # glowing eye-slits
        if face == "front" and y == 0:
            return rgba(BRASS)
        return stone(face, x, y, fw, fh)

    def body(face, x, y, fw, fh):
        if face == "front" and 3 <= y <= 6 and 2 <= x <= 5:
            return rgba(EMBER if (x + y) % 2 else EMBER_HI)   # the ember heart
        if y in (0, fh - 1):
            return rgba(BRASS)
        return stone(face, x, y, fw, fh)
    paint_box(img, 0, 0, 8, 8, 8, head)
    paint_box(img, 16, 16, 8, 12, 4, body)
    paint_box(img, 40, 16, 3, 14, 3, lambda f, x, y, fw, fh: rgba(BRASS) if y == fh - 1 else stone(f, x, y, fw, fh))
    paint_box(img, 0, 16, 4, 12, 4, stone)
    return img


def command_brand(rng):
    px = [[CLEAR] * 16 for _ in range(16)]
    for i in range(3, 14):                 # a wooden baton, diagonal
        px[16 - i][i - 1] = rgba(WOOD)
        px[16 - i][i] = rgba(shade(WOOD, -2))
    for (x, y) in ((12, 2), (13, 2), (12, 3), (13, 3), (13, 1), (14, 2)):
        px[y][x] = rgba(EMBER_HI if (x + y) % 2 else EMBER)     # the smouldering tip
    for (x, y) in ((10, 5), (11, 5), (10, 6)):
        px[y][x] = rgba(BRASS)
    return px


def dusk_stone(rng):
    """The spires' masonry: ink-violet stone in long courses with faint violet seams."""
    px = [[rgba(shade(INK, rng.choice((0, 1, 1, 2)))) for x in range(16)] for y in range(16)]
    for y in range(16):
        for x in range(16):
            if y % 8 == 7 or (x + (8 if (y // 8) % 2 else 0)) % 16 == 15:
                px[y][x] = rgba(shade(VIOLET, -3))
    return px


def spire_core(rng):
    """A knot of hardened dark with a cold light at its heart."""
    px = [[rgba(shade(INK, rng.choice((0, 1)))) for x in range(16)] for y in range(16)]
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 2.0:
                px[y][x] = rgba(EYE)
            elif d < 4.0:
                px[y][x] = rgba(VIOLET if (x + y) % 2 else shade(VIOLET, 2))
            elif d < 6.5 and (x + y) % 3 == 0:
                px[y][x] = rgba(SMOKE)
    return px


def eclipse_atlas(rng, eyes=False):
    """The Shade's layout in deeper dark, plus the halo bars at (0, 40) 14x2x1 and (32, 40) 2x10x1."""
    img = blank(64, 64)
    eye_cols, eye_row = (1, 2, 5, 6), 3
    def halo(face, x, y, fw, fh):
        if eyes:
            return rgba(EYE) if (x + y) % 3 == 0 else rgba(shade(EYE, -8))
        return rgba(BONE) if (x + y) % 4 else rgba(shade(BONE, -3))
    if eyes:
        paint_box(img, 0, 0, 8, 8, 8, eyes_only(eye_cols, eye_row))
    else:
        dark = cloth(rng, base=shade(INK, -1), fold=INK, edge=VIOLET)
        paint_box(img, 0, 0, 8, 8, 8, mask_face(rng, eye_cols, eye_row, dark))
        paint_box(img, 16, 16, 8, 12, 4, ragged(rng, dark, rows=4))
        paint_box(img, 40, 16, 3, 14, 3, ragged(rng, dark, rows=3))
        paint_box(img, 0, 16, 4, 12, 4, dark)
    paint_box(img, 0, 40, 14, 2, 1, halo)
    paint_box(img, 32, 40, 2, 12, 1, halo)
    return img


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
    "entity/cannon_shell.png": cannon_shell,
    "blocks/tower_core_archer_side.png": archer_side,
    "blocks/tower_core_archer_top.png": archer_top,
    "blocks/tower_core_cannon_side.png": cannon_side,
    "blocks/tower_core_cannon_top.png": cannon_top,
    "blocks/tower_core_frost_side.png": frost_side,
    "blocks/tower_core_frost_top.png": frost_top,
    "blocks/tower_core_lantern_side.png": lantern_side,
    "blocks/tower_core_lantern_top.png": lantern_top,
    "items/blueprint.png": blueprint_item,
    "entity/hearth_sprite.png": sprite_atlas,
    "items/duskglass.png": duskglass,
    "items/command_brand.png": command_brand,
    "blocks/dusk_stone.png": dusk_stone,
    "entity/gloam_eclipse.png": eclipse_atlas,
    "entity/gloam_eclipse_eyes.png": lambda rng: eclipse_atlas(rng, eyes=True),
    "blocks/spire_core.png": spire_core,
    "entity/ember_guard.png": ember_guard_atlas,
    "blocks/tower_core_guardpost_side.png": guardpost_side,
    "blocks/tower_core_guardpost_top.png": guardpost_top,
    "blocks/tower_core_guardpost_side_t2.png": tiered(guardpost_side, 2),
    "blocks/tower_core_guardpost_side_t3.png": tiered(guardpost_side, 3),
}
TEXTURES.update({
    "blocks/hearth_cold_side.png": hearth_cold_side,
    "blocks/hearth_cold_top.png": hearth_cold_top,
    "blocks/tower_core_flare_side.png": flare_side,
    "blocks/tower_core_flare_top.png": flare_top,
})
for _kind, _fn in (("archer", archer_side), ("cannon", cannon_side), ("frost", frost_side),
                   ("lantern", lantern_side), ("flare", flare_side)):
    for _tier in (2, 3):
        TEXTURES["blocks/tower_core_%s_side_t%d.png" % (_kind, _tier)] = tiered(_fn, _tier)


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
