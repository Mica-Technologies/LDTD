# FAQ and troubleshooting

## Raids

??? question "Night fell and no raid came. Why?"

    A night raid needs **all** of these:

    - Raids are on (`raids.enabled` and `raids.nightRaids` in `config/ldtd.cfg`).
    - The Hearth is **lit**, **claimed**, and its chunk is loaded.
    - The Hearth's **owner** is online, in the same dimension, not in spectator mode, and within
      **96 blocks**. A trusted friend standing at the Hearth doesn't count.
    - The Hearth hasn't already been raided tonight. Calling a raid early uses up the night's raid.
    - It's properly dark: raids start from time 12500, a little after sunset.

    Sneak-use the Hearth to check it's lit and yours. Raids are checked once a second, so one
    starts within a second of everything lining up.

??? question "Can I sleep through a raid?"

    The Gloam are monsters, so as in vanilla you can't sleep while any are near you. If you do
    manage to sleep through one, the raid ends at sunrise as **dawn**: survived, and paid.

??? question "Why did the raid come from that direction again?"

    A Dusk Spire within 384 blocks of your Hearth aims every raid from its side. Raze it (or the
    nearest one, if there are several), and the direction follows the next nearest spire, or turns
    random once none are left in range. See [Dusk Spires](../offense/dusk-spires.md).

??? question "The Gloam spawned inside my base / in a weird spot"

    The Gloam gather 6 blocks outside the territory's edge, on the highest solid block in loaded
    chunks. If the edge is a cliff or a cave roof, they'll appear on top of it. A level-up moves
    the edge outward.

??? question "Gloam are left wandering after a raid"

    Gloam that were part of the raid vanish when it ends. Gloam that weren't (summoned by the
    Eclipse, spawned with `/ldtd spawn` or an egg, or a Dusk Spire's night guards) stay until
    killed. They burn in daylight. A leftover raid can also happen if the server stopped in the
    middle of one: raids aren't saved, but their Gloam are.

## Building

??? question "My blueprint says it can only be built inside a lit Hearth territory"

    Every cell of the structure has to be inside **one** lit territory. Move it further in, or
    level the Hearth up for a wider territory. A fallen Hearth has no territory at all: relight it
    with 8 coal.

??? question "The sprites aren't building"

    Sneak-use the Hearth with an empty hand. The second line says what they're waiting for:

    > *Builders: 2 jobs and 0 repairs to go, waiting for 16 Arrow in the stockpile.*

    Otherwise, check:

    - **Is a raid on, or are Gloam nearby?** Sprites hide in the Hearth during raids and whenever
      any Gloam is within 24 blocks.
    - **Has the Hearth fallen?** A cold Hearth's sprites don't work.
    - **Are there sprites?** They appear one every 10 seconds after the Hearth is placed.

??? question "Part of a structure wasn't built"

    Sprites never break anything. If a cell was already taken by a solid block when they got
    there, they skip it. Clear the space and order the blueprint again: cells that are already
    right are skipped for free.

??? question "Can I build towers outside my territory?"

    No. Towers you already have keep working if the territory moves or the Hearth falls, but new
    ones need a lit territory.

## Towers

??? question "My tower isn't shooting"

    - **Line of sight:** it needs a clear line from just above its core to the target.
    - **Range:** use the core to see its range. Archer 20, Cannon 16, Frost 10 at tier I.
    - **Damage:** if it says *too damaged to work*, fewer than three quarters of its blocks are
      left. The sprites repair siege damage from the stockpile, or you can replace the blocks
      yourself.
    - **Lantern Posts** never shoot. They give off light.

??? question "How do I tell a tower's tier?"

    The core's texture: plain for I, brass bands for II, brass and a cyan glint for III. Or use the
    core. See [Towers](../defenses/towers.md#upgrading).

## The Hearth

??? question "I can't place my Hearth: too close to another one"

    Hearths must be at least 136 blocks apart (centre to centre), so two fully grown territories
    never overlap. The message names the other Hearth's owner and position.

??? question "Is there a way to tell a fallen Hearth from a lit one?"

    The block looks the same. Sneak-use it: a cold Hearth says *This Hearth has fallen and gone
    cold*. A cold Hearth also has no territory border, and its sprites sit still over it.

??? question "Someone else's Hearth is in my way"

    Only its owner (or an operator) can break it. An operator can also give it to someone else
    with `/ldtd hearth setowner <player>`.

## Performance

??? question "Is LDTD heavy on a server?"

    Raids only happen with a defender near, and at most 120 Gloam are alive per Hearth at once.
    Routes are planned once and shared, cached, and capped at 60,000 steps, so a hundred Gloam
    don't each run their own pathfinding. If a server is struggling, lower
    `raids.maxAlivePerHearth` or `raids.routeNodeBudget`.

## Data files

??? question "I edited a JSON file and nothing changed"

    Run `/ldtd reload`. If it reports a problem for that file, the file's previous values are
    still in force: fix what it names and reload again. Unit stats apply to Gloam spawned after
    the reload.

??? question "My JSON file changed by itself after an update"

    That's on purpose. When an update adds a field to the defaults, LDTD adds it to your file
    (with its default value) so nothing reads as zero. Your own values are never overwritten.
