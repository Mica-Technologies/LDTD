package com.micatechnologies.minecraft.ldtd.spire;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.LdtdConstants;
import com.micatechnologies.minecraft.ldtd.LdtdRegistry;
import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import com.micatechnologies.minecraft.ldtd.gloam.GloamKind;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import com.micatechnologies.minecraft.ldtd.raid.RaidDirector;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Dusk Spires in the world (Phase 9): where they generate, and the Gloam that guard them at night.
 */
@Mod.EventBusSubscriber(modid = LdtdConstants.MOD_NAMESPACE)
public final class SpireEvents {

    /** How close a player must come before a spire's guards rise. */
    private static final int GUARD_TRIGGER = 32;

    private SpireEvents() {
        throw new AssertionError("No instances.");
    }

    /** Dusk stone: the spires' dark masonry. */
    public static Block duskStone() {
        return new Block(Material.ROCK) {
            {
                setRegistryName(LdtdConstants.MOD_NAMESPACE, "dusk_stone");
                setTranslationKey(LdtdConstants.MOD_NAMESPACE + ".dusk_stone");
                setCreativeTab(com.micatechnologies.minecraft.ldtd.LdtdTab.TAB);
                setHardness(3.0F);
                setResistance(30.0F);
                setSoundType(SoundType.STONE);
                setHarvestLevel("pickaxe", 1);
            }
        };
    }

    /**
     * Places spires as chunks populate: a few in a thousand chunks, in the overworld only, beyond
     * {@code spires.minSpawnDistance} from spawn, {@code spires.spacing} apart, and never inside a
     * Hearth's territory. Built at the chunk's centre, so it never reaches into an ungenerated
     * neighbour.
     */
    public static final class Generator implements IWorldGenerator {

        @Override
        public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator gen,
                             IChunkProvider provider) {
            if (!LdtdConfig.spiresEnabled || world.provider.getDimension() != 0
                || random.nextInt(LdtdConfig.spireRarity) != 0) {
                return;
            }
            int x = chunkX * 16 + 8;
            int z = chunkZ * 16 + 8;
            BlockPos spawn = world.getSpawnPoint();
            double dx = x - spawn.getX();
            double dz = z - spawn.getZ();
            if (dx * dx + dz * dz < (double) LdtdConfig.spireMinSpawnDistance * LdtdConfig.spireMinSpawnDistance) {
                return;
            }
            if (SpireRegistry.get(world).nearest(x, z, LdtdConfig.spireSpacing) != null
                || HearthRegistry.get(world).territoryAt(x, z) != null) {
                return;
            }
            BlockPos ground = world.getHeight(new BlockPos(x, 0, z));
            if (ground.getY() < 4 || ground.getY() > 220
                || world.getBlockState(ground.down()).getMaterial().isLiquid()) {
                return;
            }
            SpireBuilder.build(world, ground, SpireBuilder.tierAt(world, ground));
        }
    }

    /** Once a second at night: rouse the guards of spires that players are near. */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || event.world.getTotalWorldTime() % 20 != 0
            || RaidDirector.isDay(event.world)) {
            return;
        }
        WorldServer world = (WorldServer) event.world;
        for (Map.Entry<BlockPos, Integer> e : SpireRegistry.get(world).all().entrySet()) {
            BlockPos core = e.getKey();
            if (!world.isBlockLoaded(core)) {
                continue;
            }
            EntityPlayer near = world.getClosestPlayer(core.getX() + 0.5D, core.getY(), core.getZ() + 0.5D,
                GUARD_TRIGGER, false);
            if (near == null || near.isSpectator() || near.isCreative()) {
                continue;
            }
            int want = 1 + e.getValue();
            int have = world.getEntitiesWithinAABB(EntityGloam.class, new AxisAlignedBB(core).grow(16)).size();
            if (have < want && world.rand.nextInt(4) == 0) {
                rouse(world, core, e.getValue());
            }
        }
    }

    private static void rouse(WorldServer world, BlockPos core, int tier) {
        GloamKind kind = tier >= 2 && world.rand.nextInt(3) == 0 ? GloamKind.DIMCASTER : GloamKind.SHADE;
        Entity e = EntityList.newEntity(EntityList.getClass(
            new ResourceLocation(LdtdConstants.MOD_NAMESPACE, kind.registryPath())), world);
        if (!(e instanceof EntityGloam)) {
            return;
        }
        double angle = world.rand.nextDouble() * Math.PI * 2.0D;
        BlockPos spot = world.getHeight(core.add((int) (Math.cos(angle) * 4.0D), 0, (int) (Math.sin(angle) * 4.0D)));
        e.setLocationAndAngles(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, world.rand.nextFloat() * 360.0F,
            0.0F);
        ((EntityGloam) e).onInitialSpawn(world.getDifficultyForLocation(spot), null);
        world.spawnEntity(e);
        world.spawnParticle(net.minecraft.util.EnumParticleTypes.SMOKE_LARGE, e.posX, e.posY + 1.0D, e.posZ, 15,
            0.3D, 0.6D, 0.3D, 0.01D);
    }
}
