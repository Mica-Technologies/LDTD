# Balance simulator

LDTD's raid balance isn't tuned by feel alone. An **offline simulator** in the test suite plays
raids built from the shipped JSON against a "reasonable" defense at each Hearth level, and the
build fails if the difficulty curve drifts out of shape.

- Simulator: `sim/balance/RaidSim.java`
- Test: `src/test/java/.../sim/balance/BalanceTest.java`

Run it with:

```bash
./gradlew test --tests "*BalanceTest*" -i
```

## What it models

It's Minecraft-free, so it runs in milliseconds on a plain JVM:

- A straight **48-block route** ending at the Hearth. Gloam enter 10 ticks apart and walk at
  their real speed (the vanilla movement formula, checked against a zombie's 2.33 blocks a
  second).
- **Towers** stand 4 blocks off the route, with their real tier-scaled range, damage and fire
  rate. Archers and Cannons aim at the Gloam furthest along. Cannon splash is measured along the
  route. Frost slows everything in reach. A Lantern lights 5 blocks of route either side, or its
  aura's reach if wider (tiers II and III). A Flare shoots only fliers and leaves them lit.
- **Light**: lit Gloam take ×1.5 damage and lose 15% speed, and only the stronger of light and
  frost slows apply, as in the game.
- **Armour** uses vanilla's formula (checked: 8 damage into 10 armour is 6.08).
- **Raids** are composed by the real `RaidComposer` from the shipped `raid.json`, with real
  toughness. Each data point averages 40 raids.

It leaves out players, Ember Guards, walls and sieges. It measures what **towers alone** hold
back, so it's a floor: a player fighting alongside does better.

## The defense it assumes

The Hearth level is assumed to keep pace at one level every three raids (`1 + (raid − 1) / 3`,
up to 5), with this defense at each level:

| Level | Defense |
| :---: | --- |
| 1 | One Archer |
| 2 | Two Archers |
| 3 | Archer II, Archer, Frost, Lantern |
| 4 | Two Archer II, Cannon, Frost II, Lantern |
| 5 | Two Archer III, Cannon II, Cannon, Frost III, two Lanterns |

## What the build insists on

| Rule | Why |
| --- | --- |
| An undefended Hearth leaks **100%** on raid 1 | Raids have teeth from the start |
| Raids 1–3 leak **≤ 50%** against the on-pace defense | The first nights need you fighting beside your one tower, but are never hopeless |
| Raids 4–30 leak **≤ 25%** on pace | A player keeping pace holds the line on towers alone |
| From raid 7, a defense **one level behind** never leaks less, and on average leaks **10+ points more** | Falling behind has to cost something |
| Raid 60 leaks **> 5%** even against the full level-5 defense | There's no perfect fortress |

## The current curve

The share of Gloam that reach the Hearth (towers only), from the latest run with the shipped
defaults:

| Raid | Level | Budget | On pace | One level behind |
| :---: | :---: | :---: | :---: | :---: |
| 1 | 1 | 6 | 0% | 0% |
| 2 | 1 | 10 | 40% | 40% |
| 3 | 1 | 14 | 34% | 34% |
| 4 | 2 | 26 | 11% | 49% |
| 5 | 2 | 30 | 16% | 54% |
| 6 | 2 | 34 | 22% | 59% |
| 7 | 3 | 46 | 0.3% | 34% |
| 8 | 3 | 50 | 0.5% | 39% |
| 9 | 3 | 54 | 1.4% | 44% |
| 10 | 4 | 66 | 0% | 16% |
| 12 | 4 | 74 | 0% | 23% |
| 15 | 5 | 94 | 0% | 0.1% |
| 20 | 5 | 114 | 0% | 12% |
| 25 | 5 | 134 | 0% | 25% |
| 30 | 5 | 154 | 0% | 39% |

Read it as: **early raids are a fight**, each level-up brings a comfortable stretch, and a player
who stops growing falls further behind with every raid.

## Changing the balance

1. Edit the defaults in `src/main/resources/ldtd/defaults/*.json`.
2. Run the balance test and read the table it prints.
3. If an assertion fails, either the change went too far, or the rule it breaks needs a
   deliberate rethink. Change the test only with a reason.
