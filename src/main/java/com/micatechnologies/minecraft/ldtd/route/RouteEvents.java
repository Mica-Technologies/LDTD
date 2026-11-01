package com.micatechnologies.minecraft.ldtd.route;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.sim.route.Route;
import com.micatechnologies.minecraft.ldtd.sim.route.RoutePoint;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Server tick work for routing and sieges: healing old cracks, and drawing routes shown with
 * {@code /ldtd route show}.
 */
@Mod.EventBusSubscriber(modid = LdtdConstants.MOD_NAMESPACE)
public final class RouteEvents {

    /** How long {@code /ldtd route show} keeps drawing a route. */
    private static final int SHOW_TICKS = 200;

    private static final List<Shown> SHOWN = new ArrayList<>();

    /**
     * Debug only ({@code /ldtd debug keepticking}): keep entities ticking with no player online.
     * Vanilla stops ticking entities 15 seconds after the last player leaves a dimension, which
     * makes headless dedicated-server tests of marches and sieges impossible. Never saved.
     */
    public static boolean keepTicking;

    private RouteEvents() {
        throw new AssertionError("No instances.");
    }

    /** Draws {@code route} with particles for {@value #SHOW_TICKS} ticks. */
    public static void show(WorldServer world, Route route) {
        SHOWN.add(new Shown(world, route, world.getTotalWorldTime() + SHOW_TICKS));
    }

    public static void clearAll() {
        SHOWN.clear();
        keepTicking = false;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        WorldServer world = (WorldServer) event.world;
        if (keepTicking) {
            world.resetUpdateEntityTick();
        }
        long now = world.getTotalWorldTime();
        if (now % 20 == 0) {
            SiegeDamage.get(world).tick(world);
        }
        if (now % 10 == 0 && !SHOWN.isEmpty()) {
            Iterator<Shown> it = SHOWN.iterator();
            while (it.hasNext()) {
                Shown s = it.next();
                if (s.world != world) {
                    continue;
                }
                if (now >= s.until) {
                    it.remove();
                    continue;
                }
                for (RoutePoint p : s.route.points) {
                    world.spawnParticle(EnumParticleTypes.END_ROD, p.x + 0.5D, p.y + 0.3D, p.z + 0.5D, 1,
                        0.0D, 0.0D, 0.0D, 0.0D);
                }
                for (RoutePoint p : s.route.breaches) {
                    world.spawnParticle(EnumParticleTypes.SPELL_WITCH, p.x + 0.5D, p.y + 0.5D, p.z + 0.5D, 4,
                        0.3D, 0.3D, 0.3D, 0.0D);
                }
            }
        }
    }

    private static final class Shown {
        final WorldServer world;
        final Route route;
        final long until;

        Shown(WorldServer world, Route route, long until) {
            this.world = world;
            this.route = route;
            this.until = until;
        }
    }
}
