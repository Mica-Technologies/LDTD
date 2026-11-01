# Light as a defense

The Gloam are shadow given shape, and **light hurts them**. It's the faction's defining weakness,
and the cheapest defense you have.

## The rule

Twice a second, every Gloam checks the **block light** where its eyes are: light from torches,
lanterns, glowstone and fire, not from the sky. At **10 or more** it's **lit**:

| | In darkness | Lit (block light ≥ 10) |
| --- | :---: | :---: |
| Speed | Full | **15% slower** |
| Damage taken | Normal | **×1.5** |

You can see it: a lit Gloam trails smoke, and the glow in its eyes dims.

So every tower covering a lit area hits half again as hard, and the Gloam spend longer in range.
For the price of some torches, that's better than an upgrade.

## How much light is 10?

Block light falls by one for every block of distance:

| Source | Light at the source | Lit area (light ≥ 10) |
| --- | :---: | --- |
| Torch | 14 | About 4 blocks around it |
| Glowstone, Sea Lantern, Jack o'Lantern | 15 | About 5 blocks around it |
| **Lantern Post** (LDTD) | 15 | About 5 blocks around it, plus an 8 / 12-block aura at tiers II / III |
| Frost Tower core | 6 | Never reaches 10 on its own |
| The Hearth | 15 | About 5 blocks around it |

A torch every 8 blocks along the approach keeps the whole route lit. Press ++f3++ and read
`Light: ... (... block)` to check a spot.

## Light you can carry

Two defenses light the Gloam directly, whatever the block light around them:

- A **[Lantern Post](towers.md#lantern-post)** at tier II or III lights every Gloam within its aura
  (8 or 12 blocks).
- A **[Flare Tower](towers.md#flare-tower)** lights each flier it hits for 4 seconds.

## The Gloam fight back

- **Smothers snuff lights.** Every two seconds a Smother puts out **one** light source within
  about 3 blocks of it: torches, glowstone, sea lanterns, lit redstone lamps and other soft,
  bright blocks. It only works inside a territory, and the sprites rebuild what it snuffed after
  the raid. Tougher light blocks, like a Jack o'Lantern, a Lantern Post's core or the Hearth,
  can't be snuffed.
- **Moth-wraiths are drawn to light.** They steer towards the brightest spot nearby. A ring of
  bright light away from the Hearth, covered by towers, can lure them in.
- **The Eclipse smothers all of it.** While the Eclipse lives, no Gloam marching on your Hearth
  counts as lit, however bright it is.
- **Darkness spreads from the Dusk Spires.** No Gloam within 32 blocks of a standing spire counts
  as lit either: not by torches, auras or flares. Spire assaults are fought in the dark.

## Daylight

The Gloam are creatures of the night. A Gloam under the **open sky in daytime** smoulders and
takes a heart of damage every second (the Eclipse doesn't). Gloam killed by daylight drop no
Duskglass.

The light level that weakens the Gloam, how much it slows and weakens them, and whether they burn
by day can all be changed in [`config/ldtd.cfg`](../reference/configuration.md#gloam).
