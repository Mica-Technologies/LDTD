package com.micatechnologies.minecraft.ldtd.hearth;

import com.micatechnologies.minecraft.ldtd.Ldtd;
import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
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
        return new HearthRecord(pos, owner, ownerName, level);
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
        nbt.setTag("stockpile", stockpile.write());
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        owner = nbt.hasUniqueId("owner") ? nbt.getUniqueId("owner") : null;
        ownerName = nbt.getString("ownerName");
        level = Math.max(1, nbt.getInteger("level"));
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
        return nbt;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound nbt) {
        owner = nbt.hasUniqueId("owner") ? nbt.getUniqueId("owner") : null;
        ownerName = nbt.getString("ownerName");
        level = nbt.getInteger("level");
        syncedRadius = nbt.getInteger("radius");
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
