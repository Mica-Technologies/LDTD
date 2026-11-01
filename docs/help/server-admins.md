# Server admins

LDTD is built for multiplayer: the server runs everything, and every player needs the mod on
their client too.

## Before you open the server

- **Install on both sides.** Server and every client need the same LDTD version.
- **Pre-generate, or don't.** Dusk Spires and village Hearth Squares appear only in **new**
  chunks. If you pre-generate the world after installing LDTD, they'll be there. If the world was
  generated before LDTD, they'll only appear in new ground.
- **Decide on griefing.** By default the Gloam break blocks inside territories (only ever
  there), and drop them. Turn that off with `griefing.gloamDamageBlocks=false` if your players
  would rather just fight.

## Recommended settings by server type

=== "Survival (default)"

    The defaults are what LDTD is balanced around. Nothing to change.

=== "Peaceful building server"

    ```ini
    raids {
        B:enabled=false
    }
    spires {
        B:enabled=false
    }
    ```

    Hearths, territories, protection and sprite-built blueprints, with no Gloam.

=== "No griefing"

    ```ini
    griefing {
        B:gloamDamageBlocks=false
    }
    ```

    The Gloam march and fight, but never break a block. A Hearth walled in completely can't be
    reached, so they gather at the walls.

=== "Hardcore"

    ```ini
    hearth {
        D:fallStockpileLoss=0.5
        I:relightFuelCount=32
    }
    raids {
        I:defenderRange=192
    }
    gloam {
        I:lightThreshold=12
    }
    ```

    And in `config/ldtd/raid.json`, raise `budget.perNight` and `toughness.perNight`.

## Protection

Territory protection keeps out untrusted players: no building, breaking, buckets, fire or
stockpile access, and no explosion damage. Operators bypass it by default
(`protection.opsBypass`). It's deliberately light: it isn't a full claims plugin, and doesn't stop
PvP, doors or other containers. If you already run a claims mod, you can turn LDTD's off with
`protection.enabled=false`.

## Raids and offline players

A night raid only comes when the Hearth's **owner** is online and within 96 blocks of it. Players
who log off before dark never come back to a ruined base. Trusted players can defend, but don't
trigger raids. Raise or lower `raids.defenderRange` to change how near "near" is.

## Load

- At most `raids.maxAlivePerHearth` (120) Gloam per Hearth are alive at once.
- Routes are planned once per raid and shared, capped by `raids.routeNodeBudget` (60,000 steps),
  and only over loaded chunks.
- Towers look for a target every quarter second once they are ready to fire.
- Raid Gloam are cleaned up when a raid ends. Gloam from other sources aren't, but they burn in
  daylight.

## Testing on a dedicated server

The server console has no position. Run position commands through an entity:

```text
summon minecraft:armor_stand 0 100 0 {NoGravity:1b}
execute @e[type=armor_stand] ~ ~ ~ ldtd spawn shade 4
```

Vanilla stops ticking entities 15 seconds after the last player leaves, so for console-only
testing, run `/ldtd debug keepticking true` first.

## Modpacks

- Every balance number is in `config/ldtd/*.json`. Ship your tuned files in the pack's `config/`
  folder, and LDTD will keep them up to date with new fields as it updates.
- Blueprints can use blocks from any mod (`modid:name@meta`), so you can restyle every tower
  and the Hearth's hall to fit your pack. See [Data files](../reference/data-files.md#blueprintsjson).
- Stockpiles accept hoppers and pipes from any side.
- `/randomspawn` is handy for spreading players out so their Hearths have room.
