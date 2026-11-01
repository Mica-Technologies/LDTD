package com.micatechnologies.minecraft.ldtd.spire;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.LdtdRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import com.micatechnologies.minecraft.ldtd.hearth.TileEntityHearth;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * A Dusk Spire's core (Phase 9). Breaking it destroys the spire: the shaft crumbles, Duskglass
 * spills out (more for stronger spires), the spire stops feeding raids, and the nearest Hearth
 * counts it towards calling out the Eclipse. Hard to break, blast-proof, and the Gloam never
 * break it themselves.
 */
public class BlockSpireCore extends Block {

    /** How far around the core the spire crumbles. */
    private static final int CRUMBLE = 3;

    public BlockSpireCore() {
        super(Material.ROCK);
        setRegistryName(LdtdConstants.MOD_NAMESPACE, "spire_core");
        setTranslationKey(LdtdConstants.MOD_NAMESPACE + ".spire_core");
        setCreativeTab(com.micatechnologies.minecraft.ldtd.LdtdTab.TAB);
        setHardness(25.0F);
        setResistance(6000.0F);
        setSoundType(SoundType.GLASS);
        setHarvestLevel("pickaxe", 2);
        setLightLevel(0.4F);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            fall((WorldServer) world, pos);
        }
        super.breakBlock(world, pos, state);
    }

    private static void fall(WorldServer world, BlockPos core) {
        SpireRegistry spires = SpireRegistry.get(world);
        Integer tier = spires.all().get(core);
        if (tier == null) {
            return;
        }
        spires.remove(core);
        // The shaft crumbles to gravel and the needle above it goes with it.
        Random r = world.rand;
        for (BlockPos p : BlockPos.getAllInBoxMutable(core.add(-CRUMBLE, -1, -CRUMBLE), core.add(CRUMBLE, 20, CRUMBLE))) {
            if (world.getBlockState(p).getBlock() == LdtdRegistry.BLOCK_DUSK_STONE) {
                world.setBlockState(p.toImmutable(), r.nextInt(3) == 0 ? Blocks.GRAVEL.getDefaultState()
                    : Blocks.AIR.getDefaultState(), 3);
            }
        }
        InventoryHelper.spawnItemStack(world, core.getX() + 0.5D, core.getY() + 0.5D, core.getZ() + 0.5D,
            new ItemStack(LdtdRegistry.ITEM_DUSKGLASS, 5 * tier));
        world.spawnParticle(EnumParticleTypes.SPELL_WITCH, core.getX() + 0.5D, core.getY() + 2.0D,
            core.getZ() + 0.5D, 120, 1.5D, 3.0D, 1.5D, 0.05D);
        world.playSound(null, core, SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 0.6F, 1.6F);

        // Credit the nearest Hearth: enough spires cleared calls out the Eclipse.
        HearthRecord nearest = HearthRegistry.get(world).nearest(core.getX(), core.getZ());
        String owner = "-";
        if (nearest != null && world.isBlockLoaded(nearest.pos)) {
            TileEntity te = world.getTileEntity(nearest.pos);
            if (te instanceof TileEntityHearth) {
                ((TileEntityHearth) te).addSpireCleared();
                owner = nearest.ownerName;
            }
        }
        for (EntityPlayerMP p : world.getPlayers(EntityPlayerMP.class, p -> p.getDistanceSq(core) < 128 * 128)) {
            p.sendMessage(new TextComponentTranslation("ldtd.spire.fallen", tier, owner));
        }
    }
}
