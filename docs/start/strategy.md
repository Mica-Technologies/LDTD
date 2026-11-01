# Strategy tips

Things that aren't obvious from the rules alone. All numbers are LDTD's defaults; a server can
change them (see [Data files](../reference/data-files.md)).

## Read the announcement

The raid announcement tells you **where** the Gloam will come from and **what** is coming, 30
seconds before they arrive:

> *The Gloam gather to the north-east. Raid 7: 14 Shade, 3 Flicker, 2 Smother, 2 Dimcaster.
> They come in 30 seconds.*

- The **direction** is decided by the nearest standing [Dusk Spire](../offense/dusk-spires.md)
  within 384 blocks. With no spire near, it's random each raid. If a spire sits to the
  north-east of your Hearth, the raids come from the north-east, every time, until you raze it.
  Build your strongest defenses facing your spires.
- From raid 5 on, the Gloam come from **two directions**, the second at least 90° away from the
  first. A Hearth defended on one side only starts to leak here.
- The **roster** says which counters you need. Smothers mean your walls and torches are about to
  be tested, Moth-wraiths ignore walls entirely, and Dimcasters stay back and shoot.

## Place towers where the route goes

The Gloam don't wander. Each raid plans one shared route from the edge of the territory to the
Hearth, preferring to walk around walls rather than break them. Operators can see a route with
`/ldtd route show` (it draws the path in particles for ten seconds).

- **Archer Towers** (range 20) go where they can see the most of the route.
- **Cannon Towers** hit everything within 2.5 blocks of where the shell lands, so put them
  where the Gloam bunch up: at a gap in a wall, or where the route turns.
- **Flare Towers** only shoot fliers. Once Moth-wraiths appear (raid 4), one Flare Tower near the
  Hearth saves a lot of Flame.
- **Frost Towers** slow *everything* in their 10-block reach. Put one where your other towers
  overlap, so slowed Gloam spend longer under fire.
- Towers need **line of sight**. A tower behind a wall it can't see over is wasted.

## Use walls to steer, not to stop

A wall only stops the Gloam until they dig through it, so its real job is to **shape the route**.
Leave a deliberate gap where your towers are strongest, and the Gloam will funnel through it
instead of breaking the walls. A wall with no way around is a siege. That works too, but the
Gloam break blocks, and every block they break must be repaired from the stockpile.

Harder blocks take longer to break *and* make the Gloam more willing to walk the long way round.
Stone bricks are a good default.

## Light is the cheapest defense you have

At **block light 10 or more** the Gloam are 15% slower and take 50% more damage. That's
roughly a free tier of upgrade on every tower that covers the area.

- Torches give light 14 at the torch and 10 four blocks away, so a torch every 8 blocks
  along the approach keeps it lit.
- A **Lantern Post** is light 15 and costs almost nothing. Upgraded, its aura lights every Gloam
  within 8 (tier II) or 12 (tier III) blocks, torches or not, and Smothers can't snuff an aura.
- **Smothers** snuff light sources around them (torches, glowstone and the like). The sprites
  repair them after the raid, but a lit approach can go dark mid-raid. Kill Smothers early, and
  don't rely on torches alone.
- While **the Eclipse** lives, light doesn't weaken the Gloam marching on your Hearth at all.

Light doesn't stack with frost: a Gloam that's already slowed by a Frost Tower doesn't get the
light slowdown as well. It still takes the extra damage.

## Grow the Hearth early

The raid budget grows by **4 per raid survived** and **8 per Hearth level**. Leveling makes the
raids bigger, but it also gives you a wider territory, more sprites, and room for more towers.
The [balance simulator](../contributing/balance-simulator.md) assumes a defender who reaches
roughly one new level every three raids. If you fall behind that pace, raids from about raid 7
on start to leak through.

## Call raids early when you're ready

Sneak-use the Hearth with an empty hand twice within five seconds, at night, to **call the raid
early**. A called raid has a **25% bigger** budget and pays **25% more** Duskglass. Calling uses
up that night's automatic raid, so it's a way to choose *when* you fight, at the cost of a
harder fight.

## Keep the stockpile stocked

The sprites repair siege damage **from the stockpile**, and a fallen Hearth loses a quarter of
every stack. Keep spare stone bricks in it, and keep valuables elsewhere.

## Bring friends

`/hearth trust <player>` lets a friend build in your territory, use your stockpile, upgrade your
Hearth and command its Ember Guards. Raids only ever check that the **owner** is near, but
anyone can fight in them.

## Late game

From around raid 20, raids are over 100 budget points and every Gloam has 50% more health. A
fully grown level-5 Hearth with tier III towers holds well, but the simulator shows that even a
full defense eventually leaks. Razing the [Dusk Spires](../offense/dusk-spires.md) near you takes
3 budget points per spire tier off every raid, and once no spire stands within 384 blocks, the
direction is random again.
