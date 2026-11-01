package com.micatechnologies.minecraft.ldtd.hearth;

import com.micatechnologies.minecraft.ldtd.Ldtd;
import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

/**
 * The Hearth's tile entity: owner, level and stockpile.
 *
 * <p>The server is authoritative (CLAUDE.md rule 2). The server copy keeps the
 * {@link HearthRegistry} in step. The client copy holds only what rendering needs (level, radius,
 * owner name), which arrives in the sync packet. The client never reads the level table itself,
 * because a dedicated server's table can differ from the client's.</p>
 */
public class TileEntityHearth extends TileEntity {

    private final HearthStockpile stockpile = new HearthStockpile(this);
    private final IItemHandler itemHandler = new InvWrapper(stockpile);

    @Nullable
    private UUID owner;
    private String ownerName = "";
    private int level = 1;

    /** The Hearth's Flame (design decision D16). Gloam at the Hearth drain it; 0 and it falls. */
    private double flame = -1.0D;
    /** Fallen and not yet relit (design decision D17). Synced, so the client can show it cold. */
    private boolean dormant;
    /** Raids this Hearth has seen off. Drives raid size. */
    private int raidsSurvived;
    /** The world day of the last raid, so there is at most one night raid per night. */
    private long lastRaidDay = -1L;

    /** Trusted players (id to last known name), mirrored into the registry record. */
    private final Map<UUID, String> trusted = new LinkedHashMap<>();

    /** Client only: the territory radius the server sent. */
    private int syncedRadius;

    public HearthStockpile stockpile() {
        return stockpile;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public int level() {
        return level;
    }

    /** The territory radius: the level table's on the server, the synced value on the client. */
    public int radius() {
        if (world != null && world.isRemote) {
            return syncedRadius;
        }
        return LdtdData.hearthLevels().get(level).radius;
    }

    public double flame() {
        double max = LdtdData.raid().flame.max;
        if (flame < 0.0D || flame > max) {
            flame = max;
        }
        return flame;
    }

    /** Flame as a fraction of full, 0-1. */
    public float flameFraction() {
        return (float) (flame() / LdtdData.raid().flame.max);
    }

    /**
     * Drains Flame. Server only.
     *
     * @return true if this put the Flame out
     */
    public boolean drainFlame(double amount) {
        double before = flame();
        flame = Math.max(0.0D, before - amount);
        markDirty();
        return before > 0.0D && flame <= 0.0D;
    }

    /** Regains Flame between raids. Server only. */
    public void regenFlame(double amount) {
        flame = Math.min(LdtdData.raid().flame.max, flame() + amount);
        markDirty();
    }

    public boolean isDormant() {
        return dormant;
    }

    /** The Hearth falls (design decision D17). Server only. */
    public void fall() {
        dormant = true;
        flame = 0.0D;
        markDirtyAndSync();
    }

    /** The Hearth is relit, at half Flame. Server only. */
    public void relight() {
        dormant = false;
        flame = LdtdData.raid().flame.max * 0.5D;
        markDirtyAndSync();
    }

    public int raidsSurvived() {
        return raidsSurvived;
    }

    public void addRaidSurvived() {
        raidsSurvived++;
        markDirty();
    }

    public long lastRaidDay() {
        return lastRaidDay;
    }

    public void setLastRaidDay(long day) {
        lastRaidDay = day;
        markDirty();
    }

    /** Not saved: who asked to call a raid, and when. */
    @Nullable
    private UUID callRequester;
    private long callRequestTick;

    /**
     * Records a request to call a raid. Returns true if it confirms an earlier request from the
     * same player within {@code window} ticks (and clears it), false if it is a first request.
     */
    boolean confirmCall(UUID player, long now, long window) {
        if (player.equals(callRequester) && now - callRequestTick <= window) {
            callRequester = null;
            return true;
        }
        callRequester = player;
        callRequestTick = now;
        return false;
    }

    /** Trusted players, id to name. Read-only. */
    public Map<UUID, String> trusted() {
        return Collections.unmodifiableMap(trusted);
    }

    /** @return false if they were already trusted (or are the owner) */
    public boolean trust(UUID id, String name) {
        if (trusted.containsKey(id) || id.equals(owner)) {
            return false;
        }
        trusted.put(id, name);
        markDirtyAndSync();
        return true;
    }

    /** @return false if they weren't trusted */
    public boolean untrust(UUID id) {
        if (trusted.remove(id) == null) {
            return false;
        }
        markDirtyAndSync();
        return true;
    }

    /** Whether {@code player} may use the stockpile: the owner or a trusted player. */
    public boolean mayUse(EntityPlayer player) {
        return isOwner(player) || trusted.containsKey(player.getUniqueID());
    }

    public boolean isOwner(EntityPlayer player) {
        return owner == null || owner.equals(player.getUniqueID());
    }

    /** Called once, when a player places the Hearth. */
    void claim(EntityPlayer player) {
        owner = player.getUniqueID();
        ownerName = player.getName();
        level = 1;
        markDirtyAndSync();
    }

    /** Hands the Hearth to another player (an operator's tool). They stop being merely trusted. */
    public void setOwner(UUID id, String name) {
        owner = id;
        ownerName = name;
        trusted.remove(id);
        markDirtyAndSync();
    }

    public void setLevel(int newLevel) {
        level = Math.max(1, Math.min(newLevel, LdtdData.hearthLevels().maxLevel()));
        markDirtyAndSync();
    }

    /** Re-sends this Hearth to clients, for example after {@code /ldtd reload} changed the radius. */
    public void resync() {
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    private void markDirtyAndSync() {
        markDirty();
        if (world != null && !world.isRemote) {
            HearthRegistry.get(world).put(toRecord());
            resync();
        }
    }

    HearthRecord toRecord() {
        return new HearthRecord(pos, owner, ownerName, level, dormant, trusted.keySet());
    }

    @Override
    public void onLoad() {
        if (world.isRemote) {
            Ldtd.proxy.onHearthLoaded(this);
        } else {
            // Re-register a Hearth the registry has lost (for example, a world edited with an
            // external tool) so the territory still exists.
            HearthRegistry registry = HearthRegistry.get(world);
            if (registry.at(pos) == null) {
                registry.put(toRecord());
            }
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (world != null && world.isRemote) {
            Ldtd.proxy.onHearthUnloaded(this);
        }
    }

    @Override
    public void onChunkUnload() {
        if (world.isRemote) {
            Ldtd.proxy.onHearthUnloaded(this);
        }
    }

    // ----------------------------------------------------------------------------------------
    // Save / load

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (owner != null) {
            nbt.setUniqueId("owner", owner);
        }
        nbt.setString("ownerName", ownerName);
        nbt.setInteger("level", level);
        nbt.setDouble("flame", flame);
        nbt.setBoolean("dormant", dormant);
        nbt.setInteger("raidsSurvived", raidsSurvived);
        nbt.setLong("lastRaidDay", lastRaidDay);
        NBTTagList trust = new NBTTagList();
        for (Map.Entry<UUID, String> e : trusted.entrySet()) {
            NBTTagCompound t = new NBTTagCompound();
            t.setUniqueId("id", e.getKey());
            t.setString("name", e.getValue());
            trust.appendTag(t);
        }
        nbt.setTag("trusted", trust);
        nbt.setTag("stockpile", stockpile.write());
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        owner = nbt.hasUniqueId("owner") ? nbt.getUniqueId("owner") : null;
        ownerName = nbt.getString("ownerName");
        level = Math.max(1, nbt.getInteger("level"));
        flame = nbt.hasKey("flame") ? nbt.getDouble("flame") : -1.0D;
        dormant = nbt.getBoolean("dormant");
        raidsSurvived = nbt.getInteger("raidsSurvived");
        lastRaidDay = nbt.hasKey("lastRaidDay") ? nbt.getLong("lastRaidDay") : -1L;
        trusted.clear();
        NBTTagList trust = nbt.getTagList("trusted", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < trust.tagCount(); i++) {
            NBTTagCompound t = trust.getCompoundTagAt(i);
            trusted.put(t.getUniqueId("id"), t.getString("name"));
        }
        stockpile.read(nbt.getTagList("stockpile", Constants.NBT.TAG_COMPOUND));
    }

    // ----------------------------------------------------------------------------------------
    // Client sync: only what rendering needs, never the stockpile.

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound nbt = super.writeToNBT(new NBTTagCompound());
        // The owner's id goes to clients so their dig prediction agrees with the server's
        // owner-only break rule. Player UUIDs are public in vanilla anyway (tab list, skins).
        if (owner != null) {
            nbt.setUniqueId("owner", owner);
        }
        nbt.setString("ownerName", ownerName);
        nbt.setInteger("level", level);
        nbt.setInteger("radius", radius());
        nbt.setBoolean("dormant", dormant);
        return nbt;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound nbt) {
        owner = nbt.hasUniqueId("owner") ? nbt.getUniqueId("owner") : null;
        ownerName = nbt.getString("ownerName");
        level = nbt.getInteger("level");
        syncedRadius = nbt.getInteger("radius");
        dormant = nbt.getBoolean("dormant");
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    // ----------------------------------------------------------------------------------------
    // Automation access to the stockpile (hoppers, pipes).

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandler);
        }
        return super.getCapability(capability, facing);
    }
}
