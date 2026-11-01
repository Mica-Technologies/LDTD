package com.micatechnologies.minecraft.ldtd.village;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.LdtdRegistry;
import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.sim.tower.Blueprint;
import com.micatechnologies.minecraft.ldtd.tower.BlockSpec;
import com.micatechnologies.minecraft.ldtd.tower.BlockTowerCore;
import com.micatechnologies.minecraft.ldtd.tower.TileEntityTower;
import com.micatechnologies.minecraft.ldtd.tower.TowerKind;
import java.util.List;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

/**
 * LDTD's village buildings (design decisions D21/D22, Phase 9).
 *
 * <ul>
 *   <li><b>Hearth Square</b>: an 11x11 plaza with an unowned Hearth at its centre and clear ground
 *       around it, room for every growth stage (the largest is 9x9). Sneak-use claims it.</li>
 *   <li><b>Watch Post</b>: an archer tower built ready, from the archer_tower blueprint. The village's
 *       first defense.</li>
 * </ul>
 */
public final class VillagePieces {

    private VillagePieces() {
        throw new AssertionError("No instances.");
    }

    /** Registers both pieces with village generation. Called in init. */
    public static void register() {
        if (!LdtdConfig.villageBuildings) {
            return;
        }
        MapGenStructureIO.registerStructureComponent(HearthSquare.class, "LDTD_HearthSquare");
        MapGenStructureIO.registerStructureComponent(WatchPost.class, "LDTD_WatchPost");
        VillagerRegistry.instance().registerVillageCreationHandler(new Handler(HearthSquare.class, 20, 1));
        VillagerRegistry.instance().registerVillageCreationHandler(new Handler(WatchPost.class, 15, 2));
    }

    private static final class Handler implements VillagerRegistry.IVillageCreationHandler {

        private final Class<? extends StructureVillagePieces.Village> piece;
        private final int weight;
        private final int max;

        Handler(Class<? extends StructureVillagePieces.Village> piece, int weight, int max) {
            this.piece = piece;
            this.weight = weight;
            this.max = max;
        }

        @Override
        public StructureVillagePieces.PieceWeight getVillagePieceWeight(Random random, int size) {
            return new StructureVillagePieces.PieceWeight(piece, weight, max);
        }

        @Override
        public Class<?> getComponentClass() {
            return piece;
        }

        @Override
        public StructureVillagePieces.Village buildComponent(StructureVillagePieces.PieceWeight weight,
                                                             StructureVillagePieces.Start start,
                                                             List<StructureComponent> pieces, Random random, int x,
                                                             int y, int z, EnumFacing facing, int type) {
            int w = piece == HearthSquare.class ? 11 : 5;
            int h = piece == HearthSquare.class ? 6 : 8;
            StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, w, h, w,
                facing);
            if (box.minY <= 10 || StructureComponent.findIntersecting(pieces, box) != null) {
                return null;
            }
            return piece == HearthSquare.class ? new HearthSquare(start, type, box, facing)
                : new WatchPost(start, type, box, facing);
        }
    }

    /** Settles a piece onto the ground, the way vanilla's village houses do. */
    private abstract static class Grounded extends StructureVillagePieces.Village {

        Grounded() {
        }

        Grounded(StructureVillagePieces.Start start, int type, StructureBoundingBox box, EnumFacing facing) {
            super(start, type);
            setCoordBaseMode(facing);
            boundingBox = box;
        }

        abstract int height();

        /** @return false if the piece can't be placed here yet (ground not found) */
        boolean settle(World world, StructureBoundingBox sbb) {
            if (averageGroundLvl < 0) {
                averageGroundLvl = getAverageGroundLevel(world, sbb);
                if (averageGroundLvl < 0) {
                    return false;
                }
                boundingBox.offset(0, averageGroundLvl - boundingBox.maxY + height() - 1, 0);
            }
            return true;
        }

        /** Fills under the floor so the piece never floats over a dip. */
        void foundation(World world, StructureBoundingBox sbb, int size) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    replaceAirAndLiquidDownwards(world, Blocks.COBBLESTONE.getDefaultState(), x, -1, z, sbb);
                }
            }
        }

        BlockPos worldPos(int x, int y, int z) {
            return new BlockPos(getXWithOffset(x, z), getYWithOffset(y), getZWithOffset(x, z));
        }
    }

    /** The Hearth Square. */
    public static final class HearthSquare extends Grounded {

        public HearthSquare() {
        }

        HearthSquare(StructureVillagePieces.Start start, int type, StructureBoundingBox box, EnumFacing facing) {
            super(start, type, box, facing);
        }

        @Override
        int height() {
            return 6;
        }

        @Override
        public boolean addComponentParts(World world, Random random, StructureBoundingBox sbb) {
            if (!settle(world, sbb)) {
                return true;
            }
            IBlockState air = Blocks.AIR.getDefaultState();
            IBlockState path = getBiomeSpecificBlockState(Blocks.GRASS_PATH.getDefaultState());
            IBlockState paving = getBiomeSpecificBlockState(Blocks.STONEBRICK.getDefaultState());
            fillWithBlocks(world, sbb, 0, 1, 0, 10, 5, 10, air, air, false);
            fillWithBlocks(world, sbb, 0, 0, 0, 10, 0, 10, path, path, false);
            fillWithBlocks(world, sbb, 3, 0, 3, 7, 0, 7, paving, paving, false);
            foundation(world, sbb, 11);
            setBlockState(world, LdtdRegistry.BLOCK_HEARTH.getDefaultState(), 5, 1, 5, sbb);
            return true;
        }
    }

    /** The Watch Post: an archer tower, built ready. */
    public static final class WatchPost extends Grounded {

        public WatchPost() {
        }

        WatchPost(StructureVillagePieces.Start start, int type, StructureBoundingBox box, EnumFacing facing) {
            super(start, type, box, facing);
        }

        @Override
        int height() {
            return 8;
        }

        @Override
        public boolean addComponentParts(World world, Random random, StructureBoundingBox sbb) {
            if (!settle(world, sbb)) {
                return true;
            }
            Blueprint bp = LdtdData.blueprints().get("archer_tower");
            if (bp == null) {
                return true;
            }
            IBlockState air = Blocks.AIR.getDefaultState();
            fillWithBlocks(world, sbb, 0, 1, 0, 4, 7, 4, air, air, false);
            foundation(world, sbb, 5);
            // The archer tower is the same from every side, so rotation 0 matches however the
            // piece is turned, and the tower's own intact check agrees.
            for (Blueprint.Cell c : bp.cells(0)) {
                int x = 2 + c.dx;
                int y = 1 + c.dy;
                int z = 2 + c.dz;
                if (c.key == Blueprint.AIR) {
                    setBlockState(world, air, x, y, z, sbb);
                } else if (c.key == Blueprint.CORE) {
                    setBlockState(world, LdtdRegistry.BLOCK_TOWER_CORE.getDefaultState()
                        .withProperty(BlockTowerCore.KIND, TowerKind.ARCHER), x, y, z, sbb);
                    BlockPos core = worldPos(x, y, z);
                    if (sbb.isVecInside(core)) {
                        TileEntity te = world.getTileEntity(core);
                        if (te instanceof TileEntityTower) {
                            ((TileEntityTower) te).configure("archer_tower", worldPos(2, 1, 2), 0);
                        }
                    }
                } else {
                    IBlockState s = BlockSpec.state(bp.palette.get(String.valueOf(c.key)));
                    if (s != null) {
                        setBlockState(world, s, x, y, z, sbb);
                    }
                }
            }
            return true;
        }
    }
}
