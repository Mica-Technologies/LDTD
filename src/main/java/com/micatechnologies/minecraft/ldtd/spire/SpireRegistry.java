package com.micatechnologies.minecraft.ldtd.spire;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.sim.TerritoryMath;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

/**
 * Every standing Dusk Spire in one dimension, saved with the world (Phase 9). Raids look here for
 * the spires that feed them, so it must answer without the spires' chunks being loaded.
 */
public final class SpireRegistry extends WorldSavedData {

    private static final String NAME = LdtdConstants.MOD_NAMESPACE + "_spires";

    /** Core position to tier (1-3). */
    private final Map<BlockPos, Integer> spires = new LinkedHashMap<>();

    public SpireRegistry(String name) {
        super(name);
    }

    public static SpireRegistry get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        SpireRegistry r = (SpireRegistry) storage.getOrLoadData(SpireRegistry.class, NAME);
        if (r == null) {
            r = new SpireRegistry(NAME);
            storage.setData(NAME, r);
        }
        return r;
    }

    public Map<BlockPos, Integer> all() {
        return Collections.unmodifiableMap(spires);
    }

    public Collection<BlockPos> positions() {
        return Collections.unmodifiableCollection(spires.keySet());
    }

    public void add(BlockPos core, int tier) {
        spires.put(core.toImmutable(), tier);
        markDirty();
    }

    public void remove(BlockPos core) {
        if (spires.remove(core) != null) {
            markDirty();
        }
    }

    /** The standing spire nearest to column {@code (x, z)} within {@code range}, or null. */
    @Nullable
    public BlockPos nearest(int x, int z, int range) {
        BlockPos best = null;
        long bestD = (long) range * range;
        for (BlockPos p : spires.keySet()) {
            long d = TerritoryMath.distanceSq(p.getX(), p.getZ(), x, z);
            if (d <= bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** The sum of the tiers of standing spires within {@code range} of {@code (x, z)}. */
    public int tierSumWithin(int x, int z, int range) {
        int sum = 0;
        long r2 = (long) range * range;
        for (Map.Entry<BlockPos, Integer> e : spires.entrySet()) {
            if (TerritoryMath.distanceSq(e.getKey().getX(), e.getKey().getZ(), x, z) <= r2) {
                sum += e.getValue();
            }
        }
        return sum;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        spires.clear();
        NBTTagList list = nbt.getTagList("spires", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound t = list.getCompoundTagAt(i);
            spires.put(BlockPos.fromLong(t.getLong("pos")), Math.max(1, t.getInteger("tier")));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<BlockPos, Integer> e : spires.entrySet()) {
            NBTTagCompound t = new NBTTagCompound();
            t.setLong("pos", e.getKey().toLong());
            t.setInteger("tier", e.getValue());
            list.appendTag(t);
        }
        nbt.setTag("spires", list);
        return nbt;
    }
}
