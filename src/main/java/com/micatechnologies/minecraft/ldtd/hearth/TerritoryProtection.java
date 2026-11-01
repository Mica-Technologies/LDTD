package com.micatechnologies.minecraft.ldtd.hearth;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Light territory protection and border messages (Phase 4b, design decision D23).
 *
 * <p>Inside a claimed, lit territory, a player who isn't the owner or trusted can't break or place
 * blocks, empty buckets or light fires. Explosions break no blocks there. Doors, chests, levers,
 * PvP and animals stay vanilla. Each rule has a config switch.</p>
 *
 * <p>This only ever watches players and explosions. The Gloam break blocks through
 * {@code SiegeDamage}, which fires none of these events, so the siege rules (D4) are untouched.</p>
 */
@Mod.EventBusSubscriber(modid = LdtdConstants.MOD_NAMESPACE)
public final class TerritoryProtection {

    /** The territory each player was last seen in, for the entering/leaving messages. */
    private static final Map<UUID, BlockPos> LAST_TERRITORY = new HashMap<>();

    private TerritoryProtection() {
        throw new AssertionError("No instances.");
    }

    /** The territory protecting {@code pos} against {@code player}, or null if they may build there. */
    @Nullable
    public static HearthRecord barring(World world, BlockPos pos, EntityPlayer player) {
        if (world.isRemote || !LdtdConfig.protectionEnabled || !LdtdConfig.protectBlocks) {
            return null;
        }
        if (LdtdConfig.protectionOpsBypass && player.canUseCommand(2, "")) {
            return null;
        }
        HearthRecord r = HearthRegistry.get(world).territoryAt(pos.getX(), pos.getZ());
        return r == null || r.mayBuild(player.getUniqueID()) ? null : r;
    }

    private static void refuse(EntityPlayer player, HearthRecord r) {
        player.sendStatusMessage(new TextComponentTranslation("ldtd.protect.blocks",
            r.ownerName.isEmpty() ? "-" : r.ownerName), true);
        // The client already predicted the placement and used up the item. Send the real inventory
        // back so its counts don't drift until the next sync.
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent event) {
        HearthRecord r = barring(event.getWorld(), event.getPos(), event.getPlayer());
        if (r != null) {
            event.setCanceled(true);
            refuse(event.getPlayer(), r);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.PlaceEvent event) {
        HearthRecord r = barring(event.getWorld(), event.getPos(), event.getPlayer());
        if (r != null) {
            event.setCanceled(true);
            refuse(event.getPlayer(), r);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBucket(FillBucketEvent event) {
        RayTraceResult hit = event.getTarget();
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        HearthRecord r = barring(event.getWorld(), hit.getBlockPos().offset(hit.sideHit), event.getEntityPlayer());
        if (r == null) {
            r = barring(event.getWorld(), hit.getBlockPos(), event.getEntityPlayer());
        }
        if (r != null) {
            event.setCanceled(true);
            refuse(event.getEntityPlayer(), r);
        }
    }

    /** Flint and steel and fire charges set fire without a place event. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Item item = event.getItemStack().getItem();
        if (item != Items.FLINT_AND_STEEL && item != Items.FIRE_CHARGE) {
            return;
        }
        HearthRecord r = barring(event.getWorld(), event.getPos(), event.getEntityPlayer());
        if (r != null) {
            event.setCanceled(true);
            refuse(event.getEntityPlayer(), r);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        World world = event.getWorld();
        if (world.isRemote || !LdtdConfig.protectionEnabled || !LdtdConfig.protectExplosions) {
            return;
        }
        HearthRegistry registry = HearthRegistry.get(world);
        event.getAffectedBlocks().removeIf(pos -> {
            HearthRecord r = registry.territoryAt(pos.getX(), pos.getZ());
            return r != null && r.owner != null;
        });
    }

    // ----------------------------------------------------------------------------------------
    // Entering and leaving

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.END || player.world.isRemote || player.ticksExisted % 10 != 0) {
            return;
        }
        HearthRecord now = HearthRegistry.get(player.world).territoryAt((int) Math.floor(player.posX),
            (int) Math.floor(player.posZ));
        BlockPos was = LAST_TERRITORY.get(player.getUniqueID());
        BlockPos is = now == null ? null : now.pos;
        if (is == null ? was == null : is.equals(was)) {
            return;
        }
        if (is == null) {
            LAST_TERRITORY.remove(player.getUniqueID());
            player.sendStatusMessage(new TextComponentTranslation("ldtd.territory.leave"), true);
        } else {
            LAST_TERRITORY.put(player.getUniqueID(), is);
            player.sendStatusMessage(new TextComponentTranslation(now.owner == null ? "ldtd.territory.enter_unclaimed"
                : "ldtd.territory.enter", now.ownerName.isEmpty() ? "-" : now.ownerName), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_TERRITORY.remove(event.player.getUniqueID());
    }

    public static void clearAll() {
        LAST_TERRITORY.clear();
    }
}
