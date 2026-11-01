package com.micatechnologies.minecraft.ldtd.tower;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.LdtdTab;
import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * The Command Brand (Phase 8): a smouldering baton for giving Ember Guards their orders. It reaches
 * every Guard within {@value #REACH} blocks whose post stands in a territory the player may build in.
 *
 * <ul>
 *   <li>Right-click a block: hold that spot.</li>
 *   <li>Right-click a Gloam: attack it.</li>
 *   <li>Sneak-right-click: follow me.</li>
 *   <li>Right-click the air: back to your posts.</li>
 * </ul>
 */
public class ItemCommandBrand extends Item {

    private static final double REACH = 32.0D;

    public ItemCommandBrand() {
        setRegistryName(LdtdConstants.MOD_NAMESPACE, "command_brand");
        setTranslationKey(LdtdConstants.MOD_NAMESPACE + ".command_brand");
        setCreativeTab(LdtdTab.TAB);
        setMaxStackSize(1);
    }

    private static List<EntityEmberGuard> guards(World world, EntityPlayer player) {
        HearthRegistry hearths = HearthRegistry.get(world);
        return world.getEntitiesWithinAABB(EntityEmberGuard.class, new AxisAlignedBB(player.getPosition()).grow(REACH),
            g -> {
                if (g == null || g.post() == null) {
                    return false;
                }
                HearthRecord r = hearths.territoryAt(g.post().getX(), g.post().getZ());
                return r != null && (r.mayBuild(player.getUniqueID()) || player.canUseCommand(2, ""));
            });
    }

    private static void tell(EntityPlayer player, String key, int count) {
        player.sendStatusMessage(new TextComponentTranslation(count == 0 ? "ldtd.brand.none" : key, count), true);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side,
                                      float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            List<EntityEmberGuard> squad = guards(world, player);
            if (player.isSneaking()) {
                squad.forEach(g -> g.orderFollow(player));
                tell(player, "ldtd.brand.follow", squad.size());
            } else {
                BlockPos spot = pos.offset(side);
                squad.forEach(g -> g.orderHold(spot));
                tell(player, "ldtd.brand.hold", squad.size());
            }
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote) {
            List<EntityEmberGuard> squad = guards(world, player);
            if (player.isSneaking()) {
                squad.forEach(g -> g.orderFollow(player));
                tell(player, "ldtd.brand.follow", squad.size());
            } else {
                squad.forEach(EntityEmberGuard::orderReturn);
                tell(player, "ldtd.brand.return", squad.size());
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target,
                                            EnumHand hand) {
        if (!(target instanceof EntityGloam)) {
            return false;
        }
        if (!player.world.isRemote) {
            List<EntityEmberGuard> squad = guards(player.world, player);
            squad.forEach(g -> g.orderAttack(target));
            tell(player, "ldtd.brand.attack", squad.size());
        }
        return true;
    }
}
