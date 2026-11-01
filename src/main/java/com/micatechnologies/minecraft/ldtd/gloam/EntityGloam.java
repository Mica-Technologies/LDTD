package com.micatechnologies.minecraft.ldtd.gloam;

import com.micatechnologies.minecraft.ldtd.LdtdConfig;
import com.micatechnologies.minecraft.ldtd.data.LdtdData;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry;
import com.micatechnologies.minecraft.ldtd.hearth.HearthRegistry.HearthRecord;
import com.micatechnologies.minecraft.ldtd.sim.GloamStats;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

/**
 * Base class for every Gloam unit: stats from {@code gloam.json}, the light weakness (design
 * decision D15), and marching on a Hearth along a shared route.
 *
 * <p>The Gloam never spawn naturally. Raids (and operators, through {@code /ldtd spawn} or spawn
 * eggs) bring them in.</p>
 */
public abstract class EntityGloam extends EntityMob {

    /** Daylight damage. Bypasses armor: it's the light itself, not a blade. */
    public static final DamageSource DAYLIGHT = new DamageSource("ldtd_daylight").setDamageBypassesArmor();

    private static final UUID LIT_SLOWDOWN_ID = UUID.fromString("6f0b7a52-4d0e-4f4b-9b3c-2c7e0a3b1d11");
    private static final DataParameter<Boolean> LIT = EntityDataManager.createKey(EntityGloam.class,
        DataSerializers.BOOLEAN);

    /** How close a player must come before a marching Gloam turns to fight them. */
    protected static final double AGGRO_RANGE = 10.0D;

    @Nullable
    private BlockPos marchTarget;

    /**
     * Created in {@link #initEntityAI()}, which vanilla calls from the superclass constructor,
     * before field initializers run. So it must have no initializer here: one would replace the
     * instance the task list already holds.
     */
    GloamMarch march;

    protected EntityGloam(World world) {
        super(world);
    }

    /** Which unit this is. Must return a constant: it is called while the entity is constructed. */
    public abstract GloamKind kind();

    public GloamStats stats() {
        return LdtdData.gloam(kind());
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(LIT, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        GloamStats s = LdtdData.gloam(kind());
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(s.health);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(s.speed);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(s.attack);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(s.armor);
        // Far enough for the vanilla navigator to reach the next few route waypoints.
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(48.0D);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackMelee(this, 1.1D, false));
        march = new GloamMarch(this);
        tasks.addTask(3, march);
        tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        targetTasks.addTask(2, new NearbyPlayerTarget(this));
    }

    // ----------------------------------------------------------------------------------------
    // Marching

    /** Sends this Gloam marching on {@code hearth}. */
    public void marchOn(HearthRecord hearth) {
        marchTarget = hearth.pos;
        enablePersistence();
        if (march != null) {
            // Flyers have no march: they find their own way (EntityMothWraith).
            march.replan(true);
        }
    }

    /** The Hearth this Gloam is marching on, if it still exists. */
    @Nullable
    HearthRecord marchHearth() {
        if (marchTarget == null || world.isRemote) {
            return null;
        }
        HearthRecord r = HearthRegistry.get(world).at(marchTarget);
        // A fallen Hearth is no target: the march is over (design decision D17).
        return r == null || r.dormant ? null : r;
    }

    /** The Hearth this Gloam was sent against, or null. */
    @Nullable
    public BlockPos marchTargetPos() {
        return marchTarget;
    }

    /** Whether this Gloam was sent against the Hearth at {@code hearth}. */
    public boolean isMarchingOn(BlockPos hearth) {
        return hearth.equals(marchTarget);
    }

    // ----------------------------------------------------------------------------------------
    // Light (design decision D15)

    /** Whether this Gloam is standing in enough light to be weakened. Synced to clients. */
    public boolean isLit() {
        return dataManager.get(LIT);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) {
            if (isLit() && rand.nextInt(3) == 0) {
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX + (rand.nextDouble() - 0.5D) * width,
                    posY + rand.nextDouble() * height, posZ + (rand.nextDouble() - 0.5D) * width, 0.0D, 0.02D, 0.0D);
            }
            return;
        }
        if (ticksExisted % 10 == 0) {
            updateLight();
        }
        if (LdtdConfig.gloamBurnInDaylight && ticksExisted % 20 == 0 && world.isDaytime()
            && world.canSeeSky(new BlockPos(posX, posY + getEyeHeight(), posZ))) {
            attackEntityFrom(DAYLIGHT, 2.0F);
        }
    }

    private void updateLight() {
        BlockPos eyes = new BlockPos(posX, posY + getEyeHeight(), posZ);
        boolean lit = world.getLightFor(EnumSkyBlock.BLOCK, eyes) >= LdtdConfig.gloamLightThreshold;
        if (lit != isLit()) {
            dataManager.set(LIT, lit);
        }
        IAttributeInstance speed = getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        boolean slowed = speed.getModifier(LIT_SLOWDOWN_ID) != null;
        if (lit && !slowed) {
            speed.applyModifier(new AttributeModifier(LIT_SLOWDOWN_ID, "ldtd light slowdown",
                -LdtdConfig.gloamLitSlowdown, 2).setSaved(false));
        } else if (!lit && slowed) {
            speed.removeModifier(LIT_SLOWDOWN_ID);
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (isLit() && source != DAYLIGHT) {
            amount *= (float) LdtdConfig.gloamLitDamageMultiplier;
        }
        return super.attackEntityFrom(source, amount);
    }

    // ----------------------------------------------------------------------------------------
    // Spawning and persistence

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        if (marchTarget != null) {
            nbt.setLong("ldtdMarchTarget", marchTarget.toLong());
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        if (nbt.hasKey("ldtdMarchTarget")) {
            marchTarget = BlockPos.fromLong(nbt.getLong("ldtdMarchTarget"));
        }
    }

    // ----------------------------------------------------------------------------------------
    // Sounds: vanilla sound events, played at runtime (nothing of Mojang's is redistributed), and
    // pitched down so the Gloam don't sound like the mob they borrow from.

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VEX_DEATH;
    }

    @Override
    protected float getSoundPitch() {
        return 0.55F + rand.nextFloat() * 0.1F;
    }

    /** Targets players only when they come close, so a march isn't pulled across the map. */
    protected static final class NearbyPlayerTarget extends EntityAINearestAttackableTarget<EntityPlayer> {

        NearbyPlayerTarget(EntityCreature creature) {
            super(creature, EntityPlayer.class, true);
        }

        @Override
        protected double getTargetDistance() {
            return AGGRO_RANGE;
        }
    }
}
