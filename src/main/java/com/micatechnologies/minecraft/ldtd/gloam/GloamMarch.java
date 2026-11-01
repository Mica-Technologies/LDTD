package com.micatechnologies.minecraft.ldtd.gloam;

import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import com.micatechnologies.minecraft.ldtd.route.RouteService;
import com.micatechnologies.minecraft.ldtd.route.SiegeDamage;
import com.micatechnologies.minecraft.ldtd.sim.route.Route;
import com.micatechnologies.minecraft.ldtd.sim.route.RoutePoint;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

/**
 * Marches a Gloam along its shared route to a Hearth (design decision D10).
 *
 * <p>The route comes from {@link RouteService}. The vanilla navigator is only used for the short
 * hop to a waypoint a few steps ahead, which keeps each mob's own pathfinding small and cheap.
 * When the next waypoint is a breach (a wall block the route goes through), the mob stops in
 * front of it and hits it through {@link SiegeDamage} until it gives way.</p>
 *
 * <p>A mob that makes no progress for a few seconds (a new wall went up, it got knocked off the
 * route) asks for a fresh route from where it stands.</p>
 */
final class GloamMarch extends EntityAIBase {

    /** Waypoints ahead the navigator is pointed at. */
    private static final int LOOKAHEAD = 5;

    /** Waypoints ahead checked for a breach still standing, and for having been skipped past. */
    private static final int WINDOW = 8;

    private static final int DIG_INTERVAL = 20;
    private static final int STUCK_CHECKS = 5;

    private final EntityGloam mob;

    @Nullable
    private Route route;
    private final Set<BlockPos> breaches = new HashSet<>();
    private int index;
    @Nullable
    private RoutePoint navTarget;
    private int replanCooldown;
    private int digCooldown;
    private int stuckChecks;
    private double lastX;
    private double lastZ;
    private boolean digging;

    GloamMarch(EntityGloam mob) {
        this.mob = mob;
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        return !mob.world.isRemote && mob.getAttackTarget() == null && mob.marchHearth() != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return shouldExecute();
    }

    @Override
    public void startExecuting() {
        navTarget = null;
        if (route == null || index >= route.points.size() || distSq(route.points.get(index)) > 36.0D) {
            replan(false);
        }
    }

    @Override
    public void resetTask() {
        mob.getNavigator().clearPath();
        navTarget = null;
    }

    /** Fetches a route from where the mob stands. {@code fresh} bypasses the route cache. */
    void replan(boolean fresh) {
        HearthRecord hearth = mob.marchHearth();
        route = hearth == null ? null : RouteService.get(mob.world).route(mob.world, mob.getPosition(), hearth, fresh);
        index = 0;
        navTarget = null;
        stuckChecks = 0;
        breaches.clear();
        if (route != null) {
            for (RoutePoint p : route.breaches) {
                breaches.add(new BlockPos(p.x, p.y, p.z));
            }
        }
    }

    @Override
    public void updateTask() {
        HearthRecord hearth = mob.marchHearth();
        if (hearth == null) {
            return;
        }
        if (route == null) {
            // No route at all (sealed by unbreakable blocks, or too far to plan). Head straight for
            // the Hearth and try again now and then.
            if (--replanCooldown <= 0) {
                replanCooldown = 60;
                replan(true);
                if (route == null) {
                    mob.getNavigator().tryMoveToXYZ(hearth.pos.getX() + 0.5D, hearth.pos.getY(),
                        hearth.pos.getZ() + 0.5D, 1.0D);
                }
            }
            return;
        }

        advance();
        if (index >= route.points.size()) {
            // Arrived. What a Gloam does at the Hearth is the raid director's business (Phase 4).
            mob.getNavigator().clearPath();
            mob.getLookHelper().setLookPosition(hearth.pos.getX() + 0.5D, hearth.pos.getY() + 0.5D,
                hearth.pos.getZ() + 0.5D, 30.0F, 30.0F);
            return;
        }

        int blocked = firstBlocked();
        digging = false;
        if (blocked == index) {
            RoutePoint p = route.points.get(index);
            if (distSq(p) <= 6.25D) {
                dig(p);
            } else {
                moveTo(index > 0 ? route.points.get(index - 1) : p);
            }
        } else {
            int target = blocked > 0 ? blocked - 1 : Math.min(index + LOOKAHEAD, route.points.size() - 1);
            moveTo(route.points.get(target));
        }
        checkStuck();
    }

    /** Moves the index past every waypoint the mob has reached, or skipped past. */
    private void advance() {
        int end = Math.min(index + WINDOW, route.points.size());
        int reached = -1;
        for (int i = index; i < end; i++) {
            RoutePoint p = route.points.get(i);
            double dx = mob.posX - (p.x + 0.5D);
            double dz = mob.posZ - (p.z + 0.5D);
            if (dx * dx + dz * dz <= 1.0D && Math.abs(mob.posY - p.y) <= 1.5D) {
                reached = i;
            }
        }
        if (reached >= 0) {
            index = reached + 1;
        }
    }

    /** The first waypoint in the window whose breach blocks still stand, or -1. */
    private int firstBlocked() {
        int end = Math.min(index + WINDOW, route.points.size());
        for (int i = index; i < end; i++) {
            RoutePoint p = route.points.get(i);
            BlockPos feet = new BlockPos(p.x, p.y, p.z);
            if (standing(feet) || standing(feet.up())) {
                return i;
            }
        }
        return -1;
    }

    private boolean standing(BlockPos pos) {
        return breaches.contains(pos) && mob.world.getBlockState(pos).getMaterial().blocksMovement();
    }

    private void dig(RoutePoint p) {
        digging = true;
        mob.getNavigator().clearPath();
        navTarget = null;
        BlockPos feet = new BlockPos(p.x, p.y, p.z);
        BlockPos block = standing(feet) ? feet : feet.up();
        mob.getLookHelper().setLookPosition(block.getX() + 0.5D, block.getY() + 0.5D, block.getZ() + 0.5D,
            30.0F, 30.0F);
        if (--digCooldown > 0) {
            return;
        }
        digCooldown = DIG_INTERVAL;
        float power = (float) mob.stats().digPower;
        if (power <= 0.0F) {
            // Some units never dig. They wait for the others to open the way.
            return;
        }
        mob.swingArm(EnumHand.MAIN_HAND);
        if (!SiegeDamage.get(mob.world).hit(mob.world, block, power) && !SiegeDamage.mayBreak(mob.world, block)) {
            // The wall became unbreakable (a Hearth moved, the config changed). Plan around it.
            replan(true);
        }
    }

    private void moveTo(RoutePoint p) {
        if (!p.equals(navTarget) || mob.getNavigator().noPath()) {
            navTarget = p;
            mob.getNavigator().tryMoveToXYZ(p.x + 0.5D, p.y, p.z + 0.5D, 1.0D);
        }
    }

    private void checkStuck() {
        if (mob.ticksExisted % 20 != 0) {
            return;
        }
        double moved = (mob.posX - lastX) * (mob.posX - lastX) + (mob.posZ - lastZ) * (mob.posZ - lastZ);
        lastX = mob.posX;
        lastZ = mob.posZ;
        if (digging || moved > 0.09D) {
            stuckChecks = 0;
            return;
        }
        if (++stuckChecks >= STUCK_CHECKS) {
            replan(true);
        }
    }

    private double distSq(RoutePoint p) {
        return mob.getDistanceSq(p.x + 0.5D, p.y, p.z + 0.5D);
    }
}
