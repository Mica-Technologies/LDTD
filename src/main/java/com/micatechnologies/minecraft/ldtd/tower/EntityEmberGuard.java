package com.micatechnologies.minecraft.ldtd.tower;

import com.micatechnologies.minecraft.ldtd.gloam.EntityGloam;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * An Ember Guard (Phase 8): a stone-and-ember warrior kept by a guard post. It fights the Gloam
 * near the spot it holds, and takes simple orders from a Command Brand: hold here, attack that,
 * follow me, back to your post. Deliberately simple (research lesson: 1.12 mob AI is weak, so
 * nothing clever is asked of it).
 *
 * <p>A Guard whose post is gone fades away. Guards never despawn, and towers never hurt them.</p>
 */
public class EntityEmberGuard extends EntityCreature {

    /**
     * How far from the spot it holds a Guard will chase a Gloam, until its post tells it otherwise.
     * The post sets it from its tier-scaled {@code range} in towers.json.
     */
    private static final double DEFAULT_LEASH = 12.0D;

    /** Melee reach, squared (2.5 blocks). */
    private static final double REACH_SQ = 6.25D;

    @Nullable
    private BlockPos post;
    @Nullable
    private BlockPos hold;
    @Nullable
    private UUID follow;
    private float blow = 6.0F;
    private double leash = DEFAULT_LEASH;

    public EntityEmberGuard(World world) {
        super(world);
        setSize(0.7F, 1.95F);
    }

    /** Called by the guard post that brings it out. */
    void assign(BlockPos postPos, float damage, double leashRange) {
        post = postPos.toImmutable();
        hold = postPos.toImmutable();
        setStats(damage, leashRange);
        enablePersistence();
    }

    /** The post's current tier-scaled blow and reach. Re-sent by the post, so upgrades reach guards already out. */
    void setStats(float damage, double leashRange) {
        blow = damage;
        leash = Math.max(1.0D, leashRange);
    }

    // ----------------------------------------------------------------------------------------
    // Orders (from the Command Brand)

    public void orderHold(BlockPos pos) {
        hold = pos.toImmutable();
        follow = null;
        setAttackTarget(null);
        getNavigator().clearPath();
    }

    public void orderFollow(EntityPlayer player) {
        follow = player.getUniqueID();
        setAttackTarget(null);
    }

    public void orderReturn() {
        orderHold(post == null ? getPosition() : post);
    }

    public void orderAttack(EntityLivingBase target) {
        setAttackTarget(target);
    }

    @Nullable
    public BlockPos post() {
        return post;
    }

    /** Where the Guard stands guard: the player it follows, or the spot it holds. */
    @Nullable
    private BlockPos anchor() {
        if (follow != null) {
            EntityPlayer p = world.getPlayerEntityByUUID(follow);
            if (p != null && p.dimension == dimension) {
                return p.getPosition();
            }
            follow = null;
        }
        return hold;
    }

    // ----------------------------------------------------------------------------------------

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(6.0D);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackMelee(this, 1.2D, true) {
            // Vanilla melee reach is tiny for a 0.7-wide mob, and in a crowd of Gloam the Guard
            // rarely got close enough to swing. A big stone warrior reaches 2.5 blocks.
            @Override
            protected double getAttackReachSqr(EntityLivingBase target) {
                return REACH_SQ + target.width;
            }
        });
        tasks.addTask(3, new KeepPost(this));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityGloam.class, 10, true, false,
            g -> g != null && withinLeash(g)));
    }

    private boolean withinLeash(Entity e) {
        BlockPos a = anchor();
        return a == null || e.getDistanceSq(a) <= leash * leash;
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), blow);
        if (hit) {
            swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
        }
        return hit;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        // Players can't hurt their own guards by accident (a stray sword swing in a fight).
        if (source.getTrueSource() instanceof EntityPlayer && !source.canHarmInCreative()) {
            return false;
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote) {
            return;
        }
        if (ticksExisted % 40 == 0) {
            if (getHealth() < getMaxHealth()) {
                heal(1.0F);
            }
            // No post, no guard.
            if (post != null && world.isBlockLoaded(post)) {
                TileEntity te = world.getTileEntity(post);
                if (!(te instanceof TileEntityTower) || !((TileEntityTower) te).keeps(getUniqueID())) {
                    setDead();
                }
            }
        }
        if (getAttackTarget() != null && !withinLeash(getAttackTarget())) {
            setAttackTarget(null);
        }
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_IRONGOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_IRONGOLEM_DEATH;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        if (post != null) {
            nbt.setLong("ldtdPost", post.toLong());
        }
        if (hold != null) {
            nbt.setLong("ldtdHold", hold.toLong());
        }
        nbt.setFloat("ldtdBlow", blow);
        nbt.setDouble("ldtdLeash", leash);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        post = nbt.hasKey("ldtdPost") ? BlockPos.fromLong(nbt.getLong("ldtdPost")) : null;
        hold = nbt.hasKey("ldtdHold") ? BlockPos.fromLong(nbt.getLong("ldtdHold")) : post;
        blow = nbt.hasKey("ldtdBlow") ? nbt.getFloat("ldtdBlow") : 6.0F;
        leash = nbt.hasKey("ldtdLeash") ? nbt.getDouble("ldtdLeash") : DEFAULT_LEASH;
    }

    /** Walk back to the anchor (held spot or followed player) when there's nothing to fight. */
    private static final class KeepPost extends EntityAIBase {

        private final EntityEmberGuard guard;
        private int repath;

        KeepPost(EntityEmberGuard guard) {
            this.guard = guard;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            BlockPos a = guard.anchor();
            double near = guard.follow != null ? 9.0D : 4.0D;
            return guard.getAttackTarget() == null && a != null && guard.getDistanceSq(a) > near;
        }

        @Override
        public void updateTask() {
            BlockPos a = guard.anchor();
            if (a != null && --repath <= 0) {
                repath = 20;
                guard.getNavigator().tryMoveToXYZ(a.getX() + 0.5D, a.getY(), a.getZ() + 0.5D, 1.0D);
            }
        }
    }
}
