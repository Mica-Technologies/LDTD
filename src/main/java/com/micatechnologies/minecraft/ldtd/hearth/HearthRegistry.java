package com.micatechnologies.minecraft.ldtd.hearth;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.sim.TerritoryMath;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

/**
 * Every Hearth in one dimension, saved with the world.
 *
 * <p>Territory questions ("is this block inside a territory?", "which Hearth is nearest?") have to
 * be answerable when the Hearth's own chunk is not loaded: an enemy at the territory edge, or a
 * block-damage check, can't wait for a chunk load. So the facts those questions need (position,
 * owner, level) live here, and {@link TileEntityHearth} keeps this registry up to date.</p>
 *
 * <p>Server side only. Clients learn about Hearths from the tile entity's sync packet.</p>
 */
public final class HearthRegistry extends WorldSavedData {

    private static final String NAME = LdtdConstants.MOD_NAMESPACE + "_hearths";

    private final Map<BlockPos, HearthRecord> hearths = new LinkedHashMap<>();

    /** Required by {@link MapStorage}, which constructs saved data reflectively. */
    public HearthRegistry(String name) {
        super(name);
    }

    /** The registry for {@code world}'s dimension. Server worlds only. */
    public static HearthRegistry get(World world) {
        if (world.isRemote) {
            throw new IllegalStateException("HearthRegistry is server-side only");
        }
        MapStorage storage = world.getPerWorldStorage();
        HearthRegistry registry = (HearthRegistry) storage.getOrLoadData(HearthRegistry.class, NAME);
        if (registry == null) {
            registry = new HearthRegistry(NAME);
            storage.setData(NAME, registry);
        }
        return registry;
    }

    public Collection<HearthRecord> all() {
        return Collections.unmodifiableCollection(hearths.values());
    }

    @Nullable
    public HearthRecord at(BlockPos pos) {
        return hearths.get(pos);
    }

    public void put(HearthRecord record) {
        hearths.put(record.pos, record);
        markDirty();
    }

    public void remove(BlockPos pos) {
        if (hearths.remove(pos) != null) {
            markDirty();
        }
    }

    /**
     * The Hearth whose territory contains block column {@code (x, z)}, or null. A dormant Hearth
     * claims no territory (design decision D17).
     */
    @Nullable
    public HearthRecord territoryAt(int x, int z) {
        for (HearthRecord r : hearths.values()) {
            if (!r.dormant && TerritoryMath.contains(r.pos.getX(), r.pos.getZ(), r.radius(), x, z)) {
                return r;
            }
        }
        return null;
    }

    /**
     * The Hearth whose claim covers block column {@code (x, z)}, lit or fallen. Ownership doesn't
     * lapse while a Hearth is cold, so this is what "whose is this?" questions ask (tower cores).
     */
    @Nullable
    public HearthRecord claimAt(int x, int z) {
        for (HearthRecord r : hearths.values()) {
            if (TerritoryMath.contains(r.pos.getX(), r.pos.getZ(), r.radius(), x, z)) {
                return r;
            }
        }
        return null;
    }

    /** The Hearth nearest to block column {@code (x, z)}, or null if there are none. */
    @Nullable
    public HearthRecord nearest(int x, int z) {
        HearthRecord best = null;
        long bestDist = Long.MAX_VALUE;
        for (HearthRecord r : hearths.values()) {
            long d = TerritoryMath.distanceSq(r.pos.getX(), r.pos.getZ(), x, z);
            if (d < bestDist) {
                bestDist = d;
                best = r;
            }
        }
        return best;
    }

    /**
     * The existing Hearth that a new level-1 Hearth at {@code pos} would crowd, or null if it can be
     * placed. "Crowd" means the two territories, each grown to the largest level in the table plus
     * the configured spacing, would overlap. Checking against the largest level means a Hearth never
     * becomes impossible to level up because of a neighbour placed later.
     */
    @Nullable
    public HearthRecord conflictFor(BlockPos pos) {
        int reach = LdtdData.hearthLevels().maxRadius() + LdtdConfig.territorySpacing;
        for (HearthRecord r : hearths.values()) {
            if (TerritoryMath.overlaps(r.pos.getX(), r.pos.getZ(), reach, pos.getX(), pos.getZ(), reach)) {
                return r;
            }
        }
        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        hearths.clear();
        NBTTagList list = nbt.getTagList("hearths", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            HearthRecord r = HearthRecord.read(list.getCompoundTagAt(i));
            hearths.put(r.pos, r);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (HearthRecord r : hearths.values()) {
            list.appendTag(r.write(new NBTTagCompound()));
        }
        nbt.setTag("hearths", list);
        return nbt;
    }

    /** What the registry knows about one Hearth. */
    public static final class HearthRecord {

        public final BlockPos pos;
        @Nullable
        public final UUID owner;
        public final String ownerName;
        public final int level;
        /** A fallen Hearth that hasn't been relit: no territory, no raids (design decision D17). */
        public final boolean dormant;
        /** Players the owner trusts to build in the territory (Phase 4b). */
        public final Set<UUID> trusted;

        public HearthRecord(BlockPos pos, @Nullable UUID owner, String ownerName, int level, boolean dormant,
                            Set<UUID> trusted) {
            this.pos = pos.toImmutable();
            this.owner = owner;
            this.ownerName = ownerName == null ? "" : ownerName;
            this.level = level;
            this.dormant = dormant;
            this.trusted = Collections.unmodifiableSet(new HashSet<>(trusted));
        }

        /**
         * Whether {@code player} may build in this territory: the owner, or someone the owner trusts.
         * An unclaimed Hearth protects nothing.
         */
        public boolean mayBuild(UUID player) {
            return owner == null || owner.equals(player) || trusted.contains(player);
        }

        /** The territory radius for this Hearth's level, from the current level table. */
        public int radius() {
            return LdtdData.hearthLevels().get(level).radius;
        }

        NBTTagCompound write(NBTTagCompound nbt) {
            nbt.setLong("pos", pos.toLong());
            if (owner != null) {
                nbt.setUniqueId("owner", owner);
            }
            nbt.setString("ownerName", ownerName);
            nbt.setInteger("level", level);
            nbt.setBoolean("dormant", dormant);
            NBTTagList list = new NBTTagList();
            for (UUID id : trusted) {
                NBTTagCompound t = new NBTTagCompound();
                t.setUniqueId("id", id);
                list.appendTag(t);
            }
            nbt.setTag("trusted", list);
            return nbt;
        }

        static HearthRecord read(NBTTagCompound nbt) {
            UUID owner = nbt.hasUniqueId("owner") ? nbt.getUniqueId("owner") : null;
            Set<UUID> trusted = new HashSet<>();
            NBTTagList list = nbt.getTagList("trusted", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                trusted.add(list.getCompoundTagAt(i).getUniqueId("id"));
            }
            return new HearthRecord(BlockPos.fromLong(nbt.getLong("pos")), owner,
                nbt.getString("ownerName"), Math.max(1, nbt.getInteger("level")), nbt.getBoolean("dormant"), trusted);
        }
    }
}
