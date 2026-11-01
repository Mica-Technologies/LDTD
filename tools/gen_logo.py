#!/usr/bin/env python3
"""Generates LDTD's logo: original pixel art, drawn entirely by this script.

A stone watchtower with a Hearth's flame on top, the letters LDTD in ember, and Gloam eyes in the
dark around it. Colours come from the art direction palette in tools/gen_textures.py.

Writes:
  src/main/resources/ldtd_logo.png  (mcmod.info logoFile, shown in the Mods screen)
  docs/images/logo.png              (README)

Run: py tools/gen_logo.py
"""

import os
import random

from gen_textures import BONE, BRASS, EMBER, EMBER_HI, EYE, INK, MASONRY, SMOKE, SOOT, VIOLET, rgba, shade, write_png

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
W, H = 128, 64

# A 5x7 pixel font, just the letters the logo needs.
FONT = {
    "L": ["X....", "X....", "X....", "X....", "X....", "X....", "XXXXX"],
    "D": ["XXX..", "X..X.", "X...X", "X...X", "X...X", "X..X.", "XXX.."],
    "T": ["XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X.."],
}


def logo():
    rng = random.Random("ldtd-logo")
    img = [[None] * W for _ in range(H)]
    # Night sky: ink at the top, smoke-violet towards the ground.
    for y in range(H):
        t = y / (H - 1)
        c = tuple(int(INK[i] + (SMOKE[i] - INK[i]) * t) for i in range(3))
        for x in range(W):
            img[y][x] = rgba(c)
    # Stars.
    for _ in range(40):
        x, y = rng.randrange(W), rng.randrange(H // 2)
        img[y][x] = rgba(shade(BONE, -rng.randrange(4, 10)))
    # Rolling ground.
    for x in range(W):
        top = 54 + int(2 * ((x * 7) % 13) / 13)
        for y in range(top, H):
            img[y][x] = rgba(shade(SOOT, -1 if (x + y) % 5 else 0))
    # The watchtower, left of centre: masonry with mortar lines and a crenellated top.
    tx0, tx1, ttop = 14, 26, 22
    for y in range(ttop, 56):
        for x in range(tx0, tx1):
            mortar = (y - ttop) % 4 == 3 or (x - tx0 + (4 if ((y - ttop) // 4) % 2 else 0)) % 8 == 7
            img[y][x] = rgba(shade(MASONRY, -3 if mortar else rng.choice((-1, 0, 0, 1))))
    for x in range(tx0 - 1, tx1 + 1):
        for y in (ttop - 1, ttop - 2):
            if y == ttop - 2 and (x - tx0) % 3 == 1:
                continue
            img[y][x] = rgba(shade(MASONRY, 1))
    # A lit window.
    for y in range(34, 38):
        for x in range(19, 21):
            img[y][x] = rgba(EMBER)
    # The Hearth's flame on top of the tower, with a soft glow.
    cx, cy = 20, ttop - 7
    for y in range(cy - 10, cy + 8):
        for x in range(cx - 10, cx + 10):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if 0 <= y < H and d < 10:
                base = img[y][x]
                k = (1 - d / 10) * 0.45
                img[y][x] = (int(base[0] + (EMBER[0] - base[0]) * k), int(base[1] + (EMBER[1] - base[1]) * k),
                             int(base[2] + (EMBER[2] - base[2]) * k), 255)
    flame = ["...X...", "..XX...", "..XXX..", ".XXHX..", ".XHHXX.", "XXHHHXX", "XHHHHHX", ".XHHHX."]
    for j, row in enumerate(flame):
        for i, ch in enumerate(row):
            if ch != ".":
                img[cy - 4 + j][cx - 3 + i] = rgba(EMBER_HI if ch == "H" else EMBER)
    # LDTD, 3x scale, ember with a brass outline and a soot shadow.
    text, scale, gap = "LDTD", 3, 2
    tw = len(text) * 5 * scale + (len(text) - 1) * gap * scale
    x0, y0 = 36 + (W - 36 - tw) // 2, 14
    lit = set()
    for n, ch in enumerate(text):
        for j, row in enumerate(FONT[ch]):
            for i, px in enumerate(row):
                if px == "X":
                    for dy in range(scale):
                        for dx in range(scale):
                            lit.add((x0 + n * (5 + gap) * scale + i * scale + dx, y0 + j * scale + dy))
    for (x, y) in lit:
        img[y + 2][x + 2] = rgba(SOOT)
    for (x, y) in lit:
        for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + ox, y + oy)
            if p not in lit:
                img[p[1]][p[0]] = rgba(shade(BRASS, -4))
    for (x, y) in lit:
        img[y][x] = rgba(EMBER_HI if (y - y0) < scale * 2 else EMBER)
    # A rule and the full name's initials, small: a line of brass under the letters.
    for x in range(x0, x0 + tw):
        img[y0 + 7 * scale + 4][x] = rgba(shade(BRASS, -2))
    # Gloam eyes in the dark, in pairs, low on the ground line.
    for ex, ey in ((44, 50), (60, 52), (79, 49), (97, 51), (112, 50), (6, 51)):
        img[ey][ex] = rgba(EYE)
        img[ey][ex + 2] = rgba(EYE)
        for ox in (-1, 3):
            img[ey][ex + ox] = rgba(shade(VIOLET, 2))
    return img


def main():
    pixels = logo()
    write_png(os.path.join(ROOT, "src", "main", "resources", "ldtd_logo.png"), pixels, scale=4)
    write_png(os.path.join(ROOT, "docs", "images", "logo.png"), pixels, scale=4)
    print("wrote ldtd_logo.png and docs/images/logo.png")


if __name__ == "__main__":
    main()
