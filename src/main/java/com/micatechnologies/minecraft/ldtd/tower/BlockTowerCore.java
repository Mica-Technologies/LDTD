package com.micatechnologies.minecraft.ldtd.tower;

import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A tower's core: the block that aims and fires. Only placed by a blueprint, never by hand, and it
 * drops nothing when broken. The rest of a tower is ordinary blocks.
 *
 * <p>The lantern's core gives off full light. That's its whole job: light weakens the Gloam
 * (design decision D15) and draws Moth-wraiths.</p>
 */
public class BlockTowerCore extends Block {

    public static final PropertyEnum<TowerKind> KIND = PropertyEnum.create("kind", TowerKind.class);
    /** The tower's tier (Phase 7). Not stored in metadata: read from the tile entity for rendering. */
    public static final net.minecraft.block.properties.PropertyInteger TIER =
        net.minecraft.block.properties.PropertyInteger.create("tier", 1, 3);

    public BlockTowerCore() {
        super(Material.ROCK);
        setRegistryName(LdtdConstants.MOD_NAMESPACE, "tower_core");
        setTranslationKey(LdtdConstants.MOD_NAMESPACE + ".tower_core");
        setHardness(3.5F);
        setResistance(15.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 1);
        setDefaultState(blockState.getBaseState().withProperty(KIND, TowerKind.ARCHER).withProperty(TIER, 1));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, KIND, TIER);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        TowerKind[] kinds = TowerKind.values();
        return getDefaultState().withProperty(KIND, kinds[Math.max(0, Math.min(meta, kinds.length - 1))]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(KIND).ordinal();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world instanceof net.minecraft.world.ChunkCache
            ? ((net.minecraft.world.ChunkCache) world).getTileEntity(pos, net.minecraft.world.chunk.Chunk.EnumCreateEntityType.CHECK)
            : world.getTileEntity(pos);
        int tier = te instanceof TileEntityTower ? ((TileEntityTower) te).tier() : 1;
        return state.withProperty(TIER, Math.max(1, Math.min(tier, 3)));
    }

    /** Taking your own tower down refunds half of what it cost (Phase 7). */
    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityTower) {
                ((TileEntityTower) te).salvage(player);
            }
        }
        super.onBlockHarvested(world, pos, state, player);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        switch (state.getValue(KIND)) {
            case LANTERN:
                return 15;
            case FROST:
                return 6;
            default:
                return 0;
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTower();
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
                                  EntityPlayer player) {
        return ItemStack.EMPTY;
    }

    /** Right-click: what the tower is doing. Sneak-right-click: change what it aims at first. */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote || hand != EnumHand.MAIN_HAND) {
            return true;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityTower) {
            ((TileEntityTower) te).interact(player);
        }
        return true;
    }
}
