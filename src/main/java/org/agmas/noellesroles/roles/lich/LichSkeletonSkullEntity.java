package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * 巫妖法杖发射的骷髅头实体。
 *
 * <p>外观继承原版凋零骷髅头，但碰撞行为完全重写：
 * 1. 命中方块只播放凋零骷髅爆炸感的粒子和音效，不创建 Explosion；
 * 2. 命中玩家只走 Wathe 的玩家死亡链，不给非玩家实体造成伤害；
 * 3. 到达配置距离后自动清理，避免高速实体在地图外残留。</p>
 */
public class LichSkeletonSkullEntity extends WitherSkullEntity {
    private @NotNull LichSkeletonKind kind = LichSkeletonKind.SPELL;
    private double maxRangeBlocks = LichConstants.ONCE_STAFF_RANGE_BLOCKS;
    private Vec3d spawnPos = Vec3d.ZERO;
    private int ticksAlive;
    private final Set<Integer> hitEntities = new HashSet<>();

    public LichSkeletonSkullEntity(EntityType<? extends WitherSkullEntity> entityType, World world) {
        super(entityType, world);
        /*
         * 原版 ExplosiveProjectileEntity 会用 accelerationPower 持续推进。
         * 巫妖骷髅已经直接写入固定速度，关闭额外加速度能让“飞行速度常量”更直观。
         */
        this.accelerationPower = LichConstants.SKELETON_ACCELERATION_POWER;
    }

    public static void spawnFan(@NotNull ServerPlayerEntity owner,
                                @NotNull EntityType<? extends WitherSkullEntity> entityType,
                                @NotNull LichSkeletonKind kind,
                                int skullCount,
                                float fanDegrees,
                                double maxRangeBlocks,
                                float speedBlocksPerTick) {
        if (skullCount <= 0) {
            return;
        }

        Vec3d origin = new Vec3d(owner.getX(), owner.getEyeY() + LichConstants.SKELETON_SPAWN_EYE_Y_OFFSET, owner.getZ());
        for (int index = 0; index < skullCount; index++) {
            float yawOffset = skullCount == 1
                    ? 0.0F
                    : -fanDegrees / 2.0F + fanDegrees * index / (float) (skullCount - 1);
            Vec3d direction = directionWithYawOffset(owner, yawOffset).normalize();

            LichSkeletonSkullEntity skull = new LichSkeletonSkullEntity(entityType, owner.getWorld());
            skull.setOwner(owner);
            skull.init(kind, origin, maxRangeBlocks);
            skull.setPosition(origin.x, origin.y, origin.z);
            skull.setVelocity(direction.multiply(speedBlocksPerTick));
            owner.getWorld().spawnEntity(skull);
        }
    }

    private static Vec3d directionWithYawOffset(@NotNull PlayerEntity player, float yawOffsetDegrees) {
        /*
         * 只在水平 yaw 上均匀散开，pitch 完全沿用玩家视角。
         * 这样“上下角度由自己视角角度而定”的需求不会被扇形分散破坏。
         */
        float pitch = player.getPitch();
        float yaw = player.getYaw() + yawOffsetDegrees;
        return Vec3d.fromPolar(pitch, yaw);
    }

    private void init(@NotNull LichSkeletonKind kind, @NotNull Vec3d spawnPos, double maxRangeBlocks) {
        this.kind = kind;
        this.spawnPos = spawnPos;
        this.maxRangeBlocks = maxRangeBlocks;
    }

    @Override
    public void tick() {
        this.ticksAlive++;
        Vec3d velocityBeforeTick = this.getVelocity();
        if (!this.getWorld().isClient) {
            Vec3d currentPos = this.getPos();
            if (this.shouldDisappearBeforeMovement(currentPos)) {
                /*
                 * 距离上限不是“撞击”，只是投射物自然到达最大射程。
                 * 用户反馈希望它到点直接消失，所以这里不播放爆炸粒子/音效，也不等兜底寿命清理。
                 */
                this.discard();
                return;
            }

            Vec3d velocity = velocityBeforeTick;
            Vec3d nextPos = currentPos.add(velocity);
            Box searchBox = this.getBoundingBox().stretch(velocity).expand(LichConstants.SKELETON_HIT_SCAN_BOX_EXPAND);
            for (Entity entity : this.getWorld().getOtherEntities(this, searchBox)) {
                if (!(entity instanceof ServerPlayerEntity target)) {
                    continue;
                }
                if (this.hitEntities.contains(target.getId()) || !target.canBeHitByProjectile()) {
                    continue;
                }
                Box targetBox = target.getBoundingBox().expand(target.getTargetingMargin() + LichConstants.SKELETON_PLAYER_HITBOX_EXPAND);
                if (targetBox.raycast(currentPos, nextPos).isPresent()) {
                    this.hitEntities.add(target.getId());
                    this.onEntityHit(new EntityHitResult(target));
                    if (this.isRemoved()) {
                        return;
                    }
                }
            }
        }

        super.tick();
        if (!this.isRemoved()) {
            /*
             * WitherSkullEntity 的父类会给速度套阻力/加速度。
             * 巫妖骷髅的速度已经是配置常量，tick 后写回本 tick 开始时的速度，避免飞一段后慢慢停住。
             */
            this.setVelocity(velocityBeforeTick);
            if (!this.getWorld().isClient && this.hasReachedMaxRange(this.getPos())) {
                // 到达最大射程只安静消失；只有命中方块/玩家才播放凋零式爆炸效果。
                this.discard();
            }
        }
    }

    private boolean shouldDisappearBeforeMovement(@NotNull Vec3d currentPos) {
        if (this.ticksAlive > LichConstants.SKELETON_MAX_LIFETIME_TICKS) {
            return true;
        }
        return this.hasReachedMaxRange(currentPos);
    }

    private boolean hasReachedMaxRange(@NotNull Vec3d pos) {
        double maxRangeSquared = this.maxRangeBlocks * this.maxRangeBlocks;
        return this.spawnPos.squaredDistanceTo(pos) >= maxRangeSquared;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        /*
         * WitherSkullEntity#onCollision 会创建真实爆炸并附带凋零效果。
         * 巫妖只需要它的外观，不需要破坏方块或伤害非玩家实体，所以这里彻底接管碰撞分发。
         */
        if (this.getWorld().isClient || hitResult.getType() == HitResult.Type.MISS) {
            return;
        }
        if (hitResult instanceof EntityHitResult entityHitResult) {
            this.onEntityHit(entityHitResult);
        } else if (hitResult instanceof BlockHitResult blockHitResult) {
            this.onBlockHit(blockHitResult);
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (this.getWorld().isClient) {
            return;
        }
        this.setPosition(blockHitResult.getPos());
        this.playImpactEffects();
        this.discard();
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.getWorld().isClient) {
            return;
        }

        Entity entity = entityHitResult.getEntity();
        ServerPlayerEntity caster = this.getOwner() instanceof ServerPlayerEntity owner ? owner : null;

        if (MagicianServerHooks.stopPlaybackByWeaponTarget(
                entity,
                caster,
                NoellesDeathReasons.SKELETON_DEATH_REASON,
                this.kind.replayNameKey()
        )) {
            this.playImpactEffects();
            this.discard();
            return;
        }

        if (!(entity instanceof ServerPlayerEntity target)) {
            /*
             * 需求明确不伤害盔甲架、画等非玩家实体。
             * 非玩家实体被原版碰撞捕捉到时，直接按“撞到东西”播放效果并清理。
             */
            this.playImpactEffects();
            this.discard();
            return;
        }
        if (caster == null || caster.getUuid().equals(target.getUuid()) || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return;
        }

        NbtCompound replayData = this.kind.createReplayData(target.getServerWorld(), org.agmas.noellesroles.ModItems.ONCE_STAFF.getDefaultStack());
        GameRecordManager.recordGlobalEvent(target.getServerWorld(), NoellesEventIds.LICH_SKELETON_HIT_EVENT, caster, replayHitData(target));
        GameFunctions.killPlayer(target, true, caster, NoellesDeathReasons.SKELETON_DEATH_REASON, replayData);
        this.playImpactEffects();
        this.discard();
    }

    private NbtCompound replayHitData(@NotNull ServerPlayerEntity target) {
        NbtCompound data = new NbtCompound();
        data.putUuid("target", target.getUuid());
        data.putString(LichSkeletonKind.REPLAY_NAME_KEY, this.kind.replayNameKey());
        return data;
    }

    private void playImpactEffects() {
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        world.playSound(
                null,
                this.getBlockPos(),
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                SoundCategory.PLAYERS,
                LichConstants.SKELETON_IMPACT_SOUND_VOLUME,
                LichConstants.SKELETON_IMPACT_SOUND_PITCH
        );
        world.spawnParticles(
                ParticleTypes.EXPLOSION,
                this.getX(),
                this.getY(),
                this.getZ(),
                LichConstants.SKELETON_IMPACT_PARTICLE_COUNT,
                LichConstants.SKELETON_IMPACT_PARTICLE_SPREAD,
                LichConstants.SKELETON_IMPACT_PARTICLE_SPREAD,
                LichConstants.SKELETON_IMPACT_PARTICLE_SPREAD,
                LichConstants.SKELETON_IMPACT_PARTICLE_SPEED
        );
    }

    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        /*
         * 玩家不需要通过攻击反弹巫妖骷髅；保持不可被打掉，避免调试和实战中产生额外分支。
         */
        return false;
    }

    @Override
    protected boolean canHit(Entity entity) {
        /*
         * 父类碰撞只允许玩家目标参与，非玩家实体不进入“伤害目标”分支。
         * 方块碰撞仍由 ProjectileEntity 的 block raycast 处理。
         */
        return entity instanceof PlayerEntity && super.canHit(entity);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("LichSkeletonKind", this.kind.name());
        nbt.putDouble("LichMaxRange", this.maxRangeBlocks);
        nbt.putDouble("LichSpawnX", this.spawnPos.x);
        nbt.putDouble("LichSpawnY", this.spawnPos.y);
        nbt.putDouble("LichSpawnZ", this.spawnPos.z);
        nbt.putInt("LichAge", this.ticksAlive);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        try {
            this.kind = LichSkeletonKind.valueOf(nbt.getString("LichSkeletonKind"));
        } catch (IllegalArgumentException ignored) {
            this.kind = LichSkeletonKind.SPELL;
        }
        this.maxRangeBlocks = nbt.getDouble("LichMaxRange");
        if (this.maxRangeBlocks <= 0.0D) {
            this.maxRangeBlocks = LichConstants.ONCE_STAFF_RANGE_BLOCKS;
        }
        this.spawnPos = new Vec3d(nbt.getDouble("LichSpawnX"), nbt.getDouble("LichSpawnY"), nbt.getDouble("LichSpawnZ"));
        this.ticksAlive = nbt.getInt("LichAge");
    }

    @Nullable
    public LichSkeletonKind getKind() {
        return this.kind;
    }
}
