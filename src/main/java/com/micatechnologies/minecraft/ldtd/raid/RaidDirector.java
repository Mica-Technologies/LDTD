package com.micatechnologies.minecraft.ldtd.raid;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.gloam.GloamKind;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import com.micatechnologies.minecraft.ldtd.sim.raid.RaidComposer;
import com.micatechnologies.minecraft.ldtd.sim.raid.RaidRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Starts, runs and ends raids, per dimension (Phase 4).
 *
 * <ul>
 *   <li><b>Night raids</b> (design decision D7): once per night, from dusk on, each lit Hearth with
 *       a defender near (D18) is raided.</li>
 *   <li><b>Called raids</b> (D7): the owner can call the next raid early at the Hearth, at night,
 *       for a bigger raid and a bigger reward.</li>
 *   <li>Between raids, every lit Hearth slowly regains Flame (D16).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LdtdConstants.MOD_NAMESPACE)
public final class RaidDirector {

    /** Time of day the Gloam start coming: just after sunset. */
    private static final long DUSK = 12500L;

    /** Time of day they stop: just before sunrise. */
    private static final long DAWN = 23200L;

    private static final Map<Integer, Map<BlockPos, Raid>> RAIDS = new HashMap<>();

    private RaidDirector() {
        throw new AssertionError("No instances.");
    }

    /** Whether it is day: the Gloam don't come, and those out fade. */
    public static boolean isDay(World world) {
        long t = world.getWorldTime() % 24000L;
        return t < DUSK || t >= DAWN;
    }

    /** Which night it is: counts up by one each dusk. */
    static long nightIndex(World world) {
        return Math.floorDiv(world.getWorldTime() - DUSK, 24000L);
    }

    public static boolean isRaided(World world, BlockPos hearth) {
        return raids(world).containsKey(hearth);
    }

    /** Ends every raid. Called when the server stops. */
    public static void clearAll() {
        RAIDS.clear();
    }

    private static Map<BlockPos, Raid> raids(World world) {
        return RAIDS.computeIfAbsent(world.provider.getDimension(), d -> new HashMap<>());
    }

    // ----------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || event.world.getTotalWorldTime() % 10 != 0) {
            return;
        }
        WorldServer world = (WorldServer) event.world;
        boolean fullSecond = world.getTotalWorldTime() % 20 == 0;
        Map<BlockPos, Raid> raids = raids(world);

        Iterator<Raid> it = raids.values().iterator();
        while (it.hasNext()) {
            Raid raid = it.next();
            Raid.Outcome outcome = raid.tick(fullSecond);
            if (outcome != null) {
                raid.end(outcome);
                it.remove();
            }
        }
        if (!fullSecond) {
            return;
        }

        RaidRules rules = LdtdData.raid();
        long night = nightIndex(world);
        for (HearthRecord record : new ArrayList<>(HearthRegistry.get(world).all())) {
            if (record.dormant || raids.containsKey(record.pos) || !world.isBlockLoaded(record.pos)) {
                continue;
            }
            TileEntity te = world.getTileEntity(record.pos);
            if (!(te instanceof TileEntityHearth)) {
                continue;
            }
            TileEntityHearth hearth = (TileEntityHearth) te;
            hearth.regenFlame(rules.flame.regenPerMinute / 60.0D);
            hearth.tickSprites();
            if (LdtdConfig.raidsEnabled && LdtdConfig.nightRaids && !isDay(world)
                && hearth.lastRaidDay() != night && defenderNear(world, record)) {
                start(world, hearth, false);
            }
        }
    }

    /**
     * Calls the next raid at {@code hearth} now (design decision D7): bigger, for a bigger reward.
     *
     * @return null if the raid started, else the lang key of why not
     */
    @Nullable
    public static String call(World world, TileEntityHearth hearth) {
        if (!LdtdConfig.raidsEnabled || !LdtdConfig.callEarly) {
            return "ldtd.raid.call.disabled";
        }
        if (hearth.isDormant()) {
            return "ldtd.raid.call.dormant";
        }
        if (isRaided(world, hearth.getPos())) {
            return "ldtd.raid.call.already";
        }
        if (isDay(world)) {
            return "ldtd.raid.call.day";
        }
        start((WorldServer) world, hearth, true);
        return null;
    }

    /** Starts a raid now, whatever the time and whoever is near. For {@code /ldtd raid start}. */
    public static boolean force(World world, TileEntityHearth hearth, boolean called) {
        if (hearth.isDormant() || isRaided(world, hearth.getPos())) {
            return false;
        }
        start((WorldServer) world, hearth, called);
        return true;
    }

    /** Stops the raid on {@code hearth}, if any, with no result. */
    public static boolean stop(World world, BlockPos hearth) {
        Raid raid = raids(world).remove(hearth);
        if (raid == null) {
            return false;
        }
        raid.end(Raid.Outcome.STOPPED);
        return true;
    }

    private static void start(WorldServer world, TileEntityHearth hearth, boolean called) {
        RaidRules rules = LdtdData.raid();
        int night = hearth.raidsSurvived() + 1;
        Map<String, Integer> costs = new HashMap<>();
        for (GloamKind k : GloamKind.values()) {
            costs.put(k.id(), LdtdData.gloam(k).budgetCost);
        }
        Random random = new Random(world.rand.nextLong());
        int budget = rules.budgetFor(night, hearth.level(), called);
        // Dusk Spires nearby feed the raid (Phase 9).
        com.micatechnologies.minecraft.ldtd.spire.SpireRegistry spires =
            com.micatechnologies.minecraft.ldtd.spire.SpireRegistry.get(world);
        BlockPos hp = hearth.getPos();
        budget = Math.min(rules.budget.max, budget + LdtdConfig.spireBudgetPerTier
            * spires.tierSumWithin(hp.getX(), hp.getZ(), LdtdConfig.spireInfluence));
        List<List<GloamKind>> waves = new ArrayList<>();
        for (List<String> wave : RaidComposer.compose(rules, costs, budget, night, random)) {
            List<GloamKind> kinds = new ArrayList<>();
            for (String id : wave) {
                for (GloamKind k : GloamKind.values()) {
                    if (k.id().equals(id)) {
                        kinds.add(k);
                    }
                }
            }
            waves.add(kinds);
        }
        // One direction for the first few raids, then two (design decision D19).
        // The raid comes from the nearest spire's side, if one is within reach (D19, Phase 9).
        BlockPos source = spires.nearest(hp.getX(), hp.getZ(), LdtdConfig.spireInfluence);
        float first = source != null
            ? (float) Math.atan2(source.getZ() - hp.getZ(), source.getX() - hp.getX())
            : (float) (random.nextDouble() * Math.PI * 2.0D);
        float[] directions = night < 5 ? new float[] {first}
            : new float[] {first, (float) (first + Math.PI * (0.5D + random.nextDouble()))};
        hearth.setLastRaidDay(nightIndex(world));
        Raid raid = new Raid(world, hearth.getPos(), night, called, waves, directions);
        raids(world).put(hearth.getPos(), raid);
        raid.announce();
    }

    /**
     * Whether someone is home to defend (design decision D18): the owner, online, in this
     * dimension, within {@code raids.defenderRange} blocks. An unowned Hearth (placed by command)
     * counts any player in range.
     */
    private static boolean defenderNear(World world, HearthRecord record) {
        double range = LdtdConfig.defenderRange;
        if (record.owner == null) {
            // Unclaimed Hearths (villages' Hearth Squares, Phase 9) aren't raided until claimed.
            return false;
        }
        for (EntityPlayer p : world.playerEntities) {
            if (record.owner.equals(p.getUniqueID())
                && !p.isSpectator() && p.getDistanceSq(record.pos) <= range * range) {
                return true;
            }
        }
        return false;
    }
}
