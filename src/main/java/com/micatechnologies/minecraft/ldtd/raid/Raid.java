package com.micatechnologies.minecraft.ldtd.raid;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import com.micatechnologies.minecraft.ldtd.gloam.GloamKind;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import com.micatechnologies.minecraft.ldtd.sim.raid.RaidRules;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.WorldServer;

/**
 * One raid on one Hearth: the countdown, the waves, the Flame drain, and how it ends.
 *
 * <p>Raids are not saved. A server that stops mid-raid loses the raid but keeps its Gloam, which go
 * on marching and fade at dawn. A raid is one night's event, so nothing is lost that matters.</p>
 */
final class Raid {

    enum Outcome { HELD, DAWN, FELL, STOPPED }

    /** How far beyond the territory edge the Gloam appear. */
    private static final int SPAWN_MARGIN = 6;

    /** How close to the Hearth a Gloam must be to drain its Flame. */
    private static final double DRAIN_REACH = 3.5D;

    /** How far beyond the territory players see the raid's boss bar. */
    private static final int BAR_MARGIN = 64;

    final WorldServer world;
    final BlockPos hearthPos;
    final int night;
    final boolean called;
    private final List<List<GloamKind>> waves;
    private final float[] directions;
    private final long startTick;
    private final int waveIntervalTicks;
    private final Deque<GloamKind> queue = new ArrayDeque<>();
    private final Set<UUID> alive = new HashSet<>();
    private final BossInfoServer bar;
    private int nextWave;
    private boolean ended;

    Raid(WorldServer world, BlockPos hearthPos, int night, boolean called, List<List<GloamKind>> waves,
         float[] directions) {
        RaidRules rules = LdtdData.raid();
        this.world = world;
        this.hearthPos = hearthPos;
        this.night = night;
        this.called = called;
        this.waves = waves;
        this.directions = directions;
        this.startTick = world.getTotalWorldTime() + rules.countdownSeconds * 20L;
        this.waveIntervalTicks = rules.waveIntervalSeconds * 20;
        this.bar = new BossInfoServer(new TextComponentString(""), BossInfo.Color.PURPLE, BossInfo.Overlay.NOTCHED_10);
    }

    // ----------------------------------------------------------------------------------------

    /** Tells nearby players what is coming, from where, and when (design decision D19). */
    void announce() {
        broadcast(new TextComponentTranslation("ldtd.raid.announce", directionNames(), night, composition(),
            LdtdData.raid().countdownSeconds));
        updateBar();
    }

    /**
     * Runs once a second (and spawns every half second).
     *
     * @return the outcome once the raid is over, else null
     */
    @Nullable
    Outcome tick(boolean fullSecond) {
        TileEntityHearth hearth = hearth();
        if (hearth == null || hearth.isDormant()) {
            return Outcome.STOPPED;
        }
        long now = world.getTotalWorldTime();
        if (now >= startTick) {
            while (nextWave < waves.size() && now >= startTick + (long) nextWave * waveIntervalTicks) {
                queue.addAll(waves.get(nextWave));
                if (nextWave > 0) {
                    broadcast(new TextComponentTranslation("ldtd.raid.wave", nextWave + 1, waves.size()));
                }
                nextWave++;
            }
            spawnSome();
        }
        if (!fullSecond) {
            return null;
        }
        pruneDead();
        if (now >= startTick && drain(hearth)) {
            return Outcome.FELL;
        }
        if (now >= startTick && nextWave >= waves.size() && queue.isEmpty() && alive.isEmpty()) {
            return Outcome.HELD;
        }
        if (now >= startTick && RaidDirector.isDay(world)) {
            return Outcome.DAWN;
        }
        updateBar();
        return null;
    }

    /** Finishes the raid: rewards, losses, messages, clean-up. */
    void end(Outcome outcome) {
        if (ended) {
            return;
        }
        ended = true;
        TileEntityHearth hearth = hearth();
        switch (outcome) {
            case HELD:
            case DAWN:
                if (hearth != null) {
                    hearth.addRaidSurvived();
                }
                broadcast(new TextComponentTranslation(outcome == Outcome.HELD ? "ldtd.raid.held" : "ldtd.raid.dawn",
                    night));
                break;
            case FELL:
                if (hearth != null) {
                    loseStockpile(hearth);
                    hearth.fall();
                }
                broadcast(new TextComponentTranslation("ldtd.raid.fell", owner()));
                break;
            default:
                broadcast(new TextComponentTranslation("ldtd.raid.stopped"));
                break;
        }
        withdraw();
        for (EntityPlayerMP p : new ArrayList<>(bar.getPlayers())) {
            bar.removePlayer(p);
        }
        bar.setVisible(false);
    }

    // ----------------------------------------------------------------------------------------
    // Spawning

    private void spawnSome() {
        HearthRecord record = HearthRegistry.get(world).at(hearthPos);
        if (record == null) {
            return;
        }
        for (int i = 0; i < 2 && !queue.isEmpty() && alive.size() < LdtdConfig.maxAlivePerHearth; i++) {
            GloamKind kind = queue.peek();
            BlockPos at = spawnPoint(record);
            if (at == null) {
                // Nowhere loaded and dry to stand at the edge right now. Try again shortly.
                return;
            }
            queue.poll();
            Entity e = EntityList.newEntity(EntityList.getClass(
                new ResourceLocation(LdtdConstants.MOD_NAMESPACE, kind.registryPath())), world);
            if (!(e instanceof EntityGloam)) {
                continue;
            }
            EntityGloam mob = (EntityGloam) e;
            double y = kind == GloamKind.MOTH_WRAITH ? at.getY() + 4.0D : at.getY();
            mob.setLocationAndAngles(at.getX() + 0.5D, y, at.getZ() + 0.5D, world.rand.nextFloat() * 360.0F, 0.0F);
            mob.onInitialSpawn(world.getDifficultyForLocation(at), null);
            world.spawnEntity(mob);
            mob.marchOn(record);
            alive.add(mob.getUniqueID());
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, mob.posX, mob.posY + 1.0D, mob.posZ, 20, 0.4D, 0.8D,
                0.4D, 0.01D);
        }
    }

    @Nullable
    private BlockPos spawnPoint(HearthRecord record) {
        double distance = record.radius() + SPAWN_MARGIN;
        for (int attempt = 0; attempt < 6; attempt++) {
            float base = directions[world.rand.nextInt(directions.length)];
            double angle = base + (world.rand.nextDouble() - 0.5D) * Math.toRadians(30.0D);
            int x = hearthPos.getX() + MathHelper.floor(Math.cos(angle) * distance);
            int z = hearthPos.getZ() + MathHelper.floor(Math.sin(angle) * distance);
            BlockPos column = new BlockPos(x, 64, z);
            if (!world.isBlockLoaded(column)) {
                continue;
            }
            BlockPos top = world.getTopSolidOrLiquidBlock(column);
            if (top.getY() > 0 && !world.getBlockState(top.down()).getMaterial().isLiquid()) {
                return top;
            }
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------
    // The Flame (design decision D16)

    /** @return true if the Flame went out */
    private boolean drain(TileEntityHearth hearth) {
        RaidRules rules = LdtdData.raid();
        double total = 0.0D;
        AxisAlignedBB box = new AxisAlignedBB(hearthPos).grow(DRAIN_REACH);
        for (EntityGloam g : world.getEntitiesWithinAABB(EntityGloam.class, box)) {
            if (g.isEntityAlive() && g.isMarchingOn(hearthPos)) {
                RaidRules.Unit unit = rules.units.get(g.kind().id());
                total += unit == null ? 1.0D : unit.drain;
            }
        }
        if (total <= 0.0D) {
            return false;
        }
        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, hearthPos.getX() + 0.5D, hearthPos.getY() + 1.1D,
            hearthPos.getZ() + 0.5D, (int) Math.min(20, total * 2), 0.2D, 0.1D, 0.2D, 0.02D);
        return hearth.drainFlame(total);
    }

    private void loseStockpile(TileEntityHearth hearth) {
        double share = LdtdConfig.fallStockpileLoss;
        for (int i = 0; i < hearth.stockpile().getSizeInventory(); i++) {
            ItemStack stack = hearth.stockpile().getStackInSlot(i);
            if (!stack.isEmpty()) {
                stack.shrink((int) Math.round(stack.getCount() * share));
            }
        }
        hearth.stockpile().markDirty();
    }

    // ----------------------------------------------------------------------------------------

    /** The raid's Gloam leave: they dissolve into smoke. */
    void withdraw() {
        for (UUID id : alive) {
            Entity e = world.getEntityFromUuid(id);
            if (e != null && e.isEntityAlive()) {
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, e.posX, e.posY + 1.0D, e.posZ, 15, 0.3D, 0.6D,
                    0.3D, 0.01D);
                e.setDead();
            }
        }
        alive.clear();
        queue.clear();
    }

    private void pruneDead() {
        Iterator<UUID> it = alive.iterator();
        while (it.hasNext()) {
            Entity e = world.getEntityFromUuid(it.next());
            if (e == null || !e.isEntityAlive()) {
                it.remove();
            }
        }
    }

    int remaining() {
        int pending = queue.size();
        for (int w = nextWave; w < waves.size(); w++) {
            pending += waves.get(w).size();
        }
        return alive.size() + pending;
    }

    @Nullable
    private TileEntityHearth hearth() {
        if (!world.isBlockLoaded(hearthPos)) {
            return null;
        }
        net.minecraft.tileentity.TileEntity te = world.getTileEntity(hearthPos);
        return te instanceof TileEntityHearth ? (TileEntityHearth) te : null;
    }

    private String owner() {
        HearthRecord r = HearthRegistry.get(world).at(hearthPos);
        return r == null || r.ownerName.isEmpty() ? "-" : r.ownerName;
    }

    // ----------------------------------------------------------------------------------------
    // Telling players

    private void updateBar() {
        TileEntityHearth hearth = hearth();
        long now = world.getTotalWorldTime();
        if (now < startTick) {
            bar.setName(new TextComponentTranslation("ldtd.raid.bar.countdown", night, directionNames(),
                (startTick - now + 19) / 20));
        } else {
            bar.setName(new TextComponentTranslation("ldtd.raid.bar.active", night, remaining()));
        }
        bar.setPercent(hearth == null ? 0.0F : hearth.flameFraction());
        HearthRecord record = HearthRegistry.get(world).at(hearthPos);
        double reach = (record == null ? 32 : record.radius()) + BAR_MARGIN;
        for (EntityPlayerMP p : world.getPlayers(EntityPlayerMP.class, p -> true)) {
            boolean near = p.getDistanceSq(hearthPos) <= reach * reach;
            if (near) {
                bar.addPlayer(p);
            } else {
                bar.removePlayer(p);
            }
        }
    }

    private void broadcast(ITextComponent message) {
        HearthRecord record = HearthRegistry.get(world).at(hearthPos);
        double reach = (record == null ? 32 : record.radius()) + BAR_MARGIN;
        for (EntityPlayerMP p : world.getPlayers(EntityPlayerMP.class, p -> p.getDistanceSq(hearthPos) <= reach * reach)) {
            p.sendMessage(message);
        }
    }

    private ITextComponent composition() {
        Map<GloamKind, Integer> counts = new EnumMap<>(GloamKind.class);
        for (List<GloamKind> wave : waves) {
            for (GloamKind k : wave) {
                counts.merge(k, 1, Integer::sum);
            }
        }
        ITextComponent out = new TextComponentString("");
        boolean first = true;
        for (Map.Entry<GloamKind, Integer> e : counts.entrySet()) {
            if (!first) {
                out.appendText(", ");
            }
            first = false;
            out.appendText(e.getValue() + " ");
            out.appendSibling(new TextComponentTranslation(
                "entity." + LdtdConstants.MOD_NAMESPACE + "." + e.getKey().registryPath() + ".name"));
        }
        return out;
    }

    private ITextComponent directionNames() {
        ITextComponent out = new TextComponentString("");
        List<String> seen = new ArrayList<>();
        for (float angle : directions) {
            String key = compass(angle);
            if (seen.contains(key)) {
                continue;
            }
            if (!seen.isEmpty()) {
                out.appendSibling(new TextComponentTranslation("ldtd.raid.and"));
            }
            seen.add(key);
            out.appendSibling(new TextComponentTranslation(key));
        }
        return out;
    }

    /** Compass name for a spawn angle (x = cos, z = sin; Minecraft's north is -z). */
    static String compass(float angle) {
        String[] names = {"east", "south_east", "south", "south_west", "west", "north_west", "north", "north_east"};
        double deg = Math.toDegrees(Math.atan2(Math.sin(angle), Math.cos(angle)));
        int index = (int) Math.round(deg / 45.0D);
        return "ldtd.direction." + names[((index % 8) + 8) % 8];
    }
}
