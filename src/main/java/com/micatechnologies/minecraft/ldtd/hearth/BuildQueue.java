package com.micatechnologies.minecraft.ldtd.hearth;

import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.sim.tower.Blueprint;
import com.micatechnologies.minecraft.ldtd.tower.BlockSpec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

/**
 * The work a Hearth's sprites have to do (Phase 6), saved with the Hearth.
 *
 * <ul>
 *   <li><b>Repairs</b>: blocks the Gloam broke or snuffed in the territory, to put back. Done first.</li>
 *   <li><b>Jobs</b>: blueprints to build block by block, oldest first: player orders and the
 *       Hearth's own growth stages (design decision D20).</li>
 * </ul>
 *
 * <p>Everything is paid for as it is handed out: one item from the stockpile per block, and a
 * job's extra cost before its first block. When something is missing, the queue waits and
 * {@link #waitingFor()} says what for. A cell that is no longer clear when its turn comes (a player
 * built there in the meantime) is skipped, so builders never break anything.</p>
 */
public final class BuildQueue {

    /** One block for a sprite to place. */
    public static final class Step {
        public final BlockPos pos;
        /** The state to place, or null to clear the spot (a blueprint's '.' cell). */
        @Nullable
        public final IBlockState state;
        /** For a tower core: the blueprint and placement, so the tower can be configured. */
        @Nullable
        public final String towerBlueprint;
        public final BlockPos anchor;
        public final int rotation;

        Step(BlockPos pos, @Nullable IBlockState state, @Nullable String towerBlueprint, BlockPos anchor,
             int rotation) {
            this.pos = pos;
            this.state = state;
            this.towerBlueprint = towerBlueprint;
            this.anchor = anchor;
            this.rotation = rotation;
        }
    }

    private static final class Job {
        final String blueprint;
        final BlockPos anchor;
        final int rotation;
        int next;
        boolean extraPaid;

        Job(String blueprint, BlockPos anchor, int rotation) {
            this.blueprint = blueprint;
            this.anchor = anchor;
            this.rotation = rotation;
        }
    }

    private static final class Repair {
        final BlockPos pos;
        final IBlockState state;

        Repair(BlockPos pos, IBlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }

    private final Deque<Repair> repairs = new ArrayDeque<>();
    private final List<Job> jobs = new ArrayList<>();
    private String waitingFor = "";

    public void addJob(String blueprint, BlockPos anchor, int rotation) {
        jobs.add(new Job(blueprint, anchor.toImmutable(), rotation));
    }

    public void addRepair(BlockPos pos, IBlockState state) {
        for (Repair r : repairs) {
            if (r.pos.equals(pos)) {
                return;
            }
        }
        repairs.add(new Repair(pos.toImmutable(), state));
    }

    public boolean isEmpty() {
        return repairs.isEmpty() && jobs.isEmpty();
    }

    public int repairsLeft() {
        return repairs.size();
    }

    public int jobsLeft() {
        return jobs.size();
    }

    /** What the queue is stuck on ("8 Stone Bricks"), or "" if it isn't stuck. */
    public String waitingFor() {
        return waitingFor;
    }

    /**
     * Hands out up to {@code max} steps, paying for each from {@code stockpile} (unless
     * {@code free}). Stops early at the first thing it can't pay for.
     */
    public List<Step> take(World world, HearthStockpile stockpile, int max, boolean free) {
        List<Step> out = new ArrayList<>();
        waitingFor = "";
        Iterator<Repair> rit = repairs.iterator();
        while (rit.hasNext() && out.size() < max) {
            Repair r = rit.next();
            if (!world.getBlockState(r.pos).getBlock().isReplaceable(world, r.pos)) {
                rit.remove();
                continue;
            }
            if (!free && !pay(stockpile, costOf(r.state))) {
                // Can't pay for this repair yet: leave the rest for later and get on with the jobs.
                break;
            }
            rit.remove();
            out.add(new Step(r.pos, r.state, null, r.pos, 0));
        }
        // Jobs go oldest first, but one waiting for materials doesn't hold up the rest: a later job
        // that can be paid for goes ahead. waitingFor reports the first thing missing.
        String firstMissing = waitingFor;
        int i = 0;
        while (i < jobs.size() && out.size() < max) {
            Job job = jobs.get(i);
            Blueprint bp = LdtdData.blueprints().get(job.blueprint);
            if (bp == null) {
                jobs.remove(i);
                continue;
            }
            if (!job.extraPaid) {
                if (!free && !payExtra(stockpile, bp)) {
                    firstMissing = firstMissing.isEmpty() ? waitingFor : firstMissing;
                    i++;
                    continue;
                }
                job.extraPaid = true;
            }
            List<Blueprint.Cell> cells = bp.cells(job.rotation);
            boolean stuck = false;
            while (job.next < cells.size() && out.size() < max) {
                Blueprint.Cell c = cells.get(job.next);
                BlockPos p = job.anchor.add(c.dx, c.dy, c.dz);
                IBlockState state = null;
                String tower = null;
                if (c.key == Blueprint.CORE) {
                    tower = job.blueprint;
                } else if (c.key != Blueprint.AIR) {
                    state = BlockSpec.state(bp.palette.get(String.valueOf(c.key)));
                    if (state == null) {
                        job.next++;
                        continue;
                    }
                }
                IBlockState there = world.getBlockState(p);
                boolean alreadyDone = state != null && there.getBlock() == state.getBlock();
                boolean clear = there.getBlock().isReplaceable(world, p);
                if (alreadyDone || !clear || c.key == Blueprint.AIR && there.getBlock().isAir(there, world, p)) {
                    job.next++;
                    continue;
                }
                if (!free && state != null && !pay(stockpile, costOf(state))) {
                    firstMissing = firstMissing.isEmpty() ? waitingFor : firstMissing;
                    stuck = true;
                    break;
                }
                job.next++;
                out.add(new Step(p, state, tower, job.anchor, job.rotation));
            }
            if (job.next >= cells.size()) {
                jobs.remove(i);
            } else if (stuck || out.size() >= max) {
                i++;
            }
        }
        waitingFor = firstMissing;
        return out;
    }

    private static ItemStack costOf(IBlockState state) {
        String name = String.valueOf(Block.REGISTRY.getNameForObject(state.getBlock()));
        return BlockSpec.costOfBlock(name + "@" + state.getBlock().getMetaFromState(state));
    }

    private boolean payExtra(HearthStockpile stockpile, Blueprint bp) {
        for (Map.Entry<String, Integer> e : bp.extraCost.entrySet()) {
            ItemStack need = BlockSpec.item(e.getKey());
            if (!need.isEmpty() && count(stockpile, need) < e.getValue()) {
                waitingFor = (e.getValue() - count(stockpile, need)) + " " + need.getDisplayName();
                return false;
            }
        }
        for (Map.Entry<String, Integer> e : bp.extraCost.entrySet()) {
            ItemStack need = BlockSpec.item(e.getKey());
            if (!need.isEmpty()) {
                remove(stockpile, need, e.getValue());
            }
        }
        return true;
    }

    private boolean pay(HearthStockpile stockpile, ItemStack one) {
        if (one.isEmpty()) {
            return true;
        }
        if (count(stockpile, one) < 1) {
            waitingFor = "1 " + one.getDisplayName();
            return false;
        }
        remove(stockpile, one, 1);
        return true;
    }

    private static int count(HearthStockpile stockpile, ItemStack like) {
        int n = 0;
        for (int i = 0; i < stockpile.getSizeInventory(); i++) {
            ItemStack s = stockpile.getStackInSlot(i);
            if (ItemStack.areItemsEqual(s, like)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void remove(HearthStockpile stockpile, ItemStack like, int amount) {
        for (int i = 0; i < stockpile.getSizeInventory() && amount > 0; i++) {
            ItemStack s = stockpile.getStackInSlot(i);
            if (ItemStack.areItemsEqual(s, like)) {
                int n = Math.min(amount, s.getCount());
                stockpile.decrStackSize(i, n);
                amount -= n;
            }
        }
    }

    // ----------------------------------------------------------------------------------------

    NBTTagCompound write() {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList r = new NBTTagList();
        for (Repair repair : repairs) {
            NBTTagCompound t = new NBTTagCompound();
            t.setLong("pos", repair.pos.toLong());
            t.setInteger("state", Block.getStateId(repair.state));
            r.appendTag(t);
        }
        nbt.setTag("repairs", r);
        NBTTagList j = new NBTTagList();
        for (Job job : jobs) {
            NBTTagCompound t = new NBTTagCompound();
            t.setString("blueprint", job.blueprint);
            t.setLong("anchor", job.anchor.toLong());
            t.setInteger("rotation", job.rotation);
            t.setInteger("next", job.next);
            t.setBoolean("extraPaid", job.extraPaid);
            j.appendTag(t);
        }
        nbt.setTag("jobs", j);
        return nbt;
    }

    void read(NBTTagCompound nbt) {
        repairs.clear();
        jobs.clear();
        NBTTagList r = nbt.getTagList("repairs", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < r.tagCount(); i++) {
            NBTTagCompound t = r.getCompoundTagAt(i);
            repairs.add(new Repair(BlockPos.fromLong(t.getLong("pos")), Block.getStateById(t.getInteger("state"))));
        }
        NBTTagList j = nbt.getTagList("jobs", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < j.tagCount(); i++) {
            NBTTagCompound t = j.getCompoundTagAt(i);
            Job job = new Job(t.getString("blueprint"), BlockPos.fromLong(t.getLong("anchor")), t.getInteger("rotation"));
            job.next = t.getInteger("next");
            job.extraPaid = t.getBoolean("extraPaid");
            jobs.add(job);
        }
    }
}
