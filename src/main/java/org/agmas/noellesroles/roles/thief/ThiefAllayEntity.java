package org.agmas.noellesroles.roles.thief;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * 小偷召唤的专属悦灵。
 *
 * <p>实体沿用原版悦灵模型、动画和手持物渲染，但关闭原版“捡同类物品、跟随喜欢的玩家、跳舞、复制”AI，
 * 改为服务端权威的偷窃状态机。全部关键状态都会写入 NBT，供区块存档和时停者逐帧回溯使用。</p>
 */
public final class ThiefAllayEntity extends AllayEntity {
    private static final TrackedData<Optional<UUID>> OWNER_UUID = DataTracker.registerData(
            ThiefAllayEntity.class,
            TrackedDataHandlerRegistry.OPTIONAL_UUID
    );
    private static final TrackedData<Optional<UUID>> TARGET_UUID = DataTracker.registerData(
            ThiefAllayEntity.class,
            TrackedDataHandlerRegistry.OPTIONAL_UUID
    );
    private static final TrackedData<Integer> ALLAY_NUMBER = DataTracker.registerData(
            ThiefAllayEntity.class,
            TrackedDataHandlerRegistry.INTEGER
    );
    private static final TrackedData<Integer> STATE = DataTracker.registerData(
            ThiefAllayEntity.class,
            TrackedDataHandlerRegistry.INTEGER
    );

    private int pathRefreshTicks;
    private int progressCheckTicks;
    private int noProgressTicks;
    private int routeFailureTicks;
    private int partialPathFailures;
    private int recoveryWaypointTicks;
    private int movementStallTicks;
    private double lastSampleDistance = Double.MAX_VALUE;
    private @Nullable BlockPos lastPlannedDestination;
    private @Nullable BlockPos recoveryWaypoint;
    private @Nullable BlockPos previousRecoveryWaypoint;
    private @Nullable Vec3d lastMovementSamplePosition;
    private boolean currentRouteReachesDestinationArea;
    private boolean suppressDeathReplay;

    public ThiefAllayEntity(EntityType<? extends AllayEntity> entityType, World world) {
        super(entityType, world);
        this.setPersistent();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OWNER_UUID, Optional.empty());
        builder.add(TARGET_UUID, Optional.empty());
        builder.add(ALLAY_NUMBER, 1);
        builder.add(STATE, ThiefAllayState.SEEKING_TARGET.ordinal());
    }

    public void initialize(@NotNull ServerPlayerEntity owner, @NotNull ServerPlayerEntity target, int number) {
        this.dataTracker.set(OWNER_UUID, Optional.of(owner.getUuid()));
        this.dataTracker.set(TARGET_UUID, Optional.of(target.getUuid()));
        this.dataTracker.set(ALLAY_NUMBER, MathHelper.clamp(
                number,
                ThiefConstants.ALLAY_MIN_COUNT,
                ThiefConstants.ALLAY_MAX_COUNT
        ));
        setState(ThiefAllayState.SEEKING_TARGET);
        resetProgress(target.squaredDistanceTo(this));
        ThiefPlayerComponent.KEY.get(owner).setLivingState(
                getAllayNumber(),
                getUuid(),
                ThiefAllayState.SEEKING_TARGET
        );
    }

    public @NotNull UUID getOwnerUuid() {
        return this.dataTracker.get(OWNER_UUID).orElse(new UUID(0L, 0L));
    }

    public @Nullable UUID getTargetUuid() {
        return this.dataTracker.get(TARGET_UUID).orElse(null);
    }

    public int getAllayNumber() {
        return MathHelper.clamp(
                this.dataTracker.get(ALLAY_NUMBER),
                ThiefConstants.ALLAY_MIN_COUNT,
                ThiefConstants.ALLAY_MAX_COUNT
        );
    }

    public ThiefAllayState getThiefState() {
        return ThiefAllayState.byOrdinal(this.dataTracker.get(STATE));
    }

    public boolean isOperationallyAlive() {
        return !isRemoved() && getThiefState().isLiving();
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new ThiefAllayNavigation(this, world);
    }

    /** 禁用原版悦灵脑行为，避免它擅自捡地面物品或跟随其它玩家。 */
    @Override
    protected void mobTick() {
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean canGather(ItemStack stack) {
        return false;
    }

    @Override
    protected ActionResult interactMob(net.minecraft.entity.player.PlayerEntity player, Hand hand) {
        return ActionResult.PASS;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.getWorld() instanceof ServerWorld world) || this.isRemoved()) {
            return;
        }

        /* 回溯播放期间实体只接受历史快照覆盖，绝不能自行偷窃、换目标或产生新回放。 */
        if (TimekeeperWorldComponent.KEY.get(world).isRewinding()) {
            this.getNavigation().stop();
            return;
        }

        ServerPlayerEntity owner = resolveOwner(world);
        if (!ThiefAllayTargeting.isValidOwner(owner) || owner.getServerWorld() != world) {
            terminateAsDead(null, ItemStack.EMPTY, null, true, true);
            return;
        }

        switch (getThiefState()) {
            case SEEKING_TARGET -> tickSeeking(world, owner);
            case RETURNING_SUCCESS, RETURNING_FAILURE -> tickReturning(owner);
            default -> {
            }
        }
    }

    private void tickSeeking(ServerWorld world, ServerPlayerEntity owner) {
        ServerPlayerEntity target = resolveTarget(world);
        if (!ThiefAllayTargeting.isValidTarget(target, getOwnerUuid()) || target.getServerWorld() != world) {
            UUID previousTarget = getTargetUuid();
            target = ThiefAllayTargeting.findNearestTarget(this, world);
            if (target != null) {
                this.dataTracker.set(TARGET_UUID, Optional.of(target.getUuid()));
                resetProgress(target.squaredDistanceTo(this));
                if (previousTarget == null || !previousTarget.equals(target.getUuid())) {
                    ThiefAllayReplay.recordRetarget(this, target);
                }
            } else {
                this.dataTracker.set(TARGET_UUID, Optional.empty());
                tickNoTargetTimeout();
                return;
            }
        }

        double distanceSquared = squaredDistanceTo(target);
        if (distanceSquared <= MathHelper.square(ThiefConstants.ALLAY_STEAL_DISTANCE)) {
            attemptSteal(owner, target);
            return;
        }

        updateNavigation(target);
        tickProgress(distanceSquared, target);
    }

    private void attemptSteal(ServerPlayerEntity owner, ServerPlayerEntity target) {
        ItemStack stolen = ThiefStealService.stealRandomStack(target, this.getRandom());
        if (stolen == null || stolen.isEmpty()) {
            ThiefAllayReplay.recordStealFail(this, target);
            setState(ThiefAllayState.RETURNING_FAILURE);
        } else {
            this.equipStack(EquipmentSlot.MAINHAND, stolen);
            ThiefAllayReplay.recordStealSuccess(this, target, stolen);
            ThiefItemTracker.refresh(owner.getServerWorld());
            setState(ThiefAllayState.RETURNING_SUCCESS);
        }
        this.dataTracker.set(TARGET_UUID, Optional.empty());
        resetProgress(squaredDistanceTo(owner));
    }

    private void tickReturning(ServerPlayerEntity owner) {
        double distanceSquared = squaredDistanceTo(owner);
        if (distanceSquared <= MathHelper.square(ThiefConstants.ALLAY_DELIVERY_DISTANCE)) {
            if (getThiefState() == ThiefAllayState.RETURNING_SUCCESS && !getMainHandStack().isEmpty()) {
                deliver(owner);
            } else {
                ThiefPlayerComponent.KEY.get(owner).clearState(getAllayNumber());
                discard();
            }
            return;
        }
        updateNavigation(owner);
        tickProgress(distanceSquared, owner);
    }

    private void deliver(ServerPlayerEntity owner) {
        ItemStack delivered = getMainHandStack().copy();
        this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        ItemEntity item = new ItemEntity(
                owner.getServerWorld(),
                this.getX(),
                this.getY() + this.getHeight() * 0.5D,
                this.getZ(),
                delivered
        );
        Vec3d direction = owner.getPos().add(0.0D, 0.8D, 0.0D).subtract(item.getPos()).normalize();
        item.setVelocity(direction.multiply(ThiefConstants.ALLAY_DELIVERY_ITEM_SPEED)
                .add(0.0D, ThiefConstants.ALLAY_DELIVERY_ITEM_UPWARD_SPEED, 0.0D));
        item.setPickupDelay(0);
        item.setOwner(owner.getUuid());
        item.addCommandTag(ThiefConstants.ALLAY_DELIVERY_TAG_PREFIX + owner.getUuid());
        owner.getServerWorld().spawnEntity(item);

        ThiefAllayReplay.recordDelivery(this, owner, delivered);
        ThiefPlayerComponent.KEY.get(owner).setTerminalState(getAllayNumber(), ThiefAllayState.DELIVERY_SUCCESS);
        ThiefItemTracker.refresh(owner.getServerWorld());
        discard();
    }

    /**
     * 推进当前导航计划。
     *
     * <p>完整路径在尚未结束、目标也没有明显移动时会一直保留，不再每10 tick无条件重算。
     * 这点对跨楼层尤其重要：去楼梯的前半段可能暂时远离目标，频繁重算会不断把它改回贴地板的不完整路径。</p>
     */
    private void updateNavigation(@NotNull Entity destination) {
        if (this.recoveryWaypoint != null) {
            tickRecoveryNavigation(destination);
            return;
        }

        BlockPos destinationPos = destination.getBlockPos();
        boolean destinationMoved = this.lastPlannedDestination == null
                || this.lastPlannedDestination.getSquaredDistance(destinationPos)
                >= ThiefConstants.ALLAY_DESTINATION_REPATH_DISTANCE_SQUARED;

        /* 已经拿到可达目标区域的完整路线时，即使目标继续移动也先把这条绕路走完。 */
        if (!this.getNavigation().isIdle() && this.currentRouteReachesDestinationArea) {
            return;
        }
        if (!this.getNavigation().isIdle() && !destinationMoved) {
            return;
        }
        if (this.pathRefreshTicks-- > 0 && !destinationMoved) {
            return;
        }

        this.pathRefreshTicks = ThiefConstants.ALLAY_PATH_REFRESH_INTERVAL_TICKS;
        this.lastPlannedDestination = destinationPos.toImmutable();
        this.currentRouteReachesDestinationArea = false;
        ThiefAllayRoutePlanner.PlannedRoute route = ThiefAllayRoutePlanner.planDestination(this, destination);
        if (route == null) {
            this.partialPathFailures++;
            if (this.partialPathFailures >= ThiefConstants.ALLAY_PARTIAL_PATH_FAILURE_LIMIT) {
                beginRecovery(destination);
            }
            return;
        }

        this.getNavigation().startMovingAlong(route.path(), ThiefConstants.ALLAY_NAVIGATION_SPEED);
        this.currentRouteReachesDestinationArea = route.reachesDestinationArea();
        if (route.reachesDestinationArea()) {
            this.partialPathFailures = 0;
        } else {
            this.partialPathFailures++;
            if (this.partialPathFailures >= ThiefConstants.ALLAY_PARTIAL_PATH_FAILURE_LIMIT
                    && this.routeFailureTicks >= ThiefConstants.ALLAY_ROUTE_RECOVERY_TRIGGER_TICKS / 2) {
                beginRecovery(destination);
            }
        }
    }

    private void tickRecoveryNavigation(@NotNull Entity destination) {
        this.recoveryWaypointTicks++;
        if (this.getBlockPos().getSquaredDistance(this.recoveryWaypoint)
                <= ThiefConstants.ALLAY_RECOVERY_WAYPOINT_REACHED_DISTANCE_SQUARED) {
            finishRecoveryWaypoint();
            return;
        }

        if (this.recoveryWaypointTicks >= ThiefConstants.ALLAY_RECOVERY_WAYPOINT_MAX_TICKS
                || this.movementStallTicks >= ThiefConstants.ALLAY_RECOVERY_MOVEMENT_STALL_TICKS) {
            beginRecovery(destination);
            return;
        }

        if (!this.getNavigation().isIdle()) {
            return;
        }
        var path = ((ThiefAllayNavigation) this.getNavigation()).findExtendedPathTo(
                this.recoveryWaypoint,
                ThiefConstants.ALLAY_RECOVERY_PATH_ACCURACY
        );
        if (path == null || !path.reachesTarget()) {
            beginRecovery(destination);
            return;
        }
        this.getNavigation().startMovingAlong(path, ThiefConstants.ALLAY_NAVIGATION_SPEED);
    }

    private void beginRecovery(@NotNull Entity destination) {
        BlockPos excludedWaypoint = this.recoveryWaypoint != null
                ? this.recoveryWaypoint
                : this.previousRecoveryWaypoint;
        /* 清掉旧的不完整路径，防止 EntityNavigation 复用旧 currentPath 而跳过本轮绕行重算。 */
        this.getNavigation().stop();
        ThiefAllayRoutePlanner.PlannedRoute route = ThiefAllayRoutePlanner.planRecovery(
                this,
                destination,
                excludedWaypoint
        );
        if (route == null) {
            /* 本轮没有找到可达锚点，短暂停止后继续尝试；120秒总兜底仍会最终清理。 */
            this.getNavigation().stop();
            this.previousRecoveryWaypoint = excludedWaypoint;
            this.recoveryWaypoint = null;
            this.recoveryWaypointTicks = 0;
            this.pathRefreshTicks = ThiefConstants.ALLAY_PATH_REFRESH_INTERVAL_TICKS;
            this.routeFailureTicks = 0;
            this.movementStallTicks = 0;
            this.currentRouteReachesDestinationArea = false;
            return;
        }

        this.previousRecoveryWaypoint = this.recoveryWaypoint;
        this.recoveryWaypoint = route.waypoint().toImmutable();
        this.recoveryWaypointTicks = 0;
        this.routeFailureTicks = 0;
        this.partialPathFailures = 0;
        this.movementStallTicks = 0;
        this.getNavigation().startMovingAlong(route.path(), ThiefConstants.ALLAY_NAVIGATION_SPEED);
        this.currentRouteReachesDestinationArea = route.reachesDestinationArea();
    }

    private void finishRecoveryWaypoint() {
        this.previousRecoveryWaypoint = this.recoveryWaypoint;
        this.recoveryWaypoint = null;
        this.recoveryWaypointTicks = 0;
        this.routeFailureTicks = 0;
        this.partialPathFailures = 0;
        this.movementStallTicks = 0;
        this.lastPlannedDestination = null;
        this.pathRefreshTicks = 0;
        this.getNavigation().stop();
        this.currentRouteReachesDestinationArea = false;
    }

    private void tickProgress(double distanceSquared, @NotNull Entity destination) {
        this.progressCheckTicks++;
        if (this.progressCheckTicks < ThiefConstants.ALLAY_PROGRESS_CHECK_INTERVAL_TICKS) {
            return;
        }
        this.progressCheckTicks = 0;

        Vec3d currentPosition = this.getPos();
        if (this.lastMovementSamplePosition != null
                && currentPosition.squaredDistanceTo(this.lastMovementSamplePosition)
                < ThiefConstants.ALLAY_MIN_ACTUAL_MOVEMENT_DISTANCE_SQUARED) {
            this.movementStallTicks += ThiefConstants.ALLAY_PROGRESS_CHECK_INTERVAL_TICKS;
        } else {
            this.movementStallTicks = 0;
        }
        this.lastMovementSamplePosition = currentPosition;

        double distance = Math.sqrt(Math.max(0.0D, distanceSquared));
        if (distance + ThiefConstants.ALLAY_MIN_PROGRESS_DISTANCE < this.lastSampleDistance) {
            this.noProgressTicks = 0;
            if (this.recoveryWaypoint == null) {
                this.routeFailureTicks = 0;
            }
            this.lastSampleDistance = distance;
            return;
        }
        this.noProgressTicks += ThiefConstants.ALLAY_PROGRESS_CHECK_INTERVAL_TICKS;
        if (this.recoveryWaypoint == null) {
            this.routeFailureTicks += ThiefConstants.ALLAY_PROGRESS_CHECK_INTERVAL_TICKS;
        }
        this.lastSampleDistance = distance;
        if (this.noProgressTicks >= ThiefConstants.ALLAY_NO_PROGRESS_TIMEOUT_TICKS) {
            terminateAsDead(null, ItemStack.EMPTY, null, true, true);
            return;
        }

        if (this.recoveryWaypoint != null) {
            if (this.movementStallTicks >= ThiefConstants.ALLAY_RECOVERY_MOVEMENT_STALL_TICKS) {
                beginRecovery(destination);
            }
            return;
        }
        /* 完整路线允许暂时远离最终目标；只有实际原地停滞才推翻它并进入绕行恢复。 */
        if (this.currentRouteReachesDestinationArea) {
            if (this.movementStallTicks >= ThiefConstants.ALLAY_RECOVERY_MOVEMENT_STALL_TICKS
                    || this.routeFailureTicks >= ThiefConstants.ALLAY_COMPLETE_ROUTE_RECOVERY_TICKS) {
                this.currentRouteReachesDestinationArea = false;
                beginRecovery(destination);
            }
            return;
        }
        if (this.routeFailureTicks >= ThiefConstants.ALLAY_ROUTE_RECOVERY_TRIGGER_TICKS) {
            beginRecovery(destination);
        }
    }

    private void tickNoTargetTimeout() {
        this.noProgressTicks++;
        if (this.noProgressTicks >= ThiefConstants.ALLAY_NO_PROGRESS_TIMEOUT_TICKS) {
            terminateAsDead(null, ItemStack.EMPTY, null, true, true);
        }
    }

    private void resetProgress(double distanceSquared) {
        this.progressCheckTicks = 0;
        this.noProgressTicks = 0;
        this.routeFailureTicks = 0;
        this.partialPathFailures = 0;
        this.recoveryWaypointTicks = 0;
        this.movementStallTicks = 0;
        this.lastSampleDistance = Math.sqrt(Math.max(0.0D, distanceSquared));
        this.lastMovementSamplePosition = this.getPos();
        this.lastPlannedDestination = null;
        this.recoveryWaypoint = null;
        this.previousRecoveryWaypoint = null;
        this.currentRouteReachesDestinationArea = false;
        this.pathRefreshTicks = 0;
        this.getNavigation().stop();
    }

    private void setState(ThiefAllayState state) {
        this.dataTracker.set(STATE, state.ordinal());
        if (this.getWorld() instanceof ServerWorld world) {
            ServerPlayerEntity owner = resolveOwner(world);
            if (owner != null && state.isLiving()) {
                ThiefPlayerComponent.KEY.get(owner).setLivingState(getAllayNumber(), getUuid(), state);
            }
        }
    }

    /** 玩法武器命中入口；伤害值不参与计算，命中即死亡。 */
    public void killByWeapon(
            @Nullable ServerPlayerEntity killer,
            @Nullable ItemStack weapon,
            @Nullable net.minecraft.util.Identifier deathReason
    ) {
        if (this.getWorld() instanceof ServerWorld world
                && TimekeeperWorldComponent.KEY.get(world).isRewinding()) {
            return;
        }
        terminateAsDead(killer, weapon, deathReason, true, true);
    }

    /** G键主动清理使用单独的汇总回放，因此逐只死亡时不重复记录击杀行。 */
    public void killByOwnerCommand() {
        this.suppressDeathReplay = true;
        terminateAsDead(null, ItemStack.EMPTY, null, true, true);
    }

    /** 对局结束和快照删除未来实体时静默移除，不掉物品、不写 HUD、不污染回放。 */
    public void discardSilently() {
        this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        discard();
    }

    private void terminateAsDead(
            @Nullable ServerPlayerEntity killer,
            @Nullable ItemStack weapon,
            @Nullable net.minecraft.util.Identifier deathReason,
            boolean dropHeldItem,
            boolean updateHud
    ) {
        if (isRemoved()) {
            return;
        }
        if (dropHeldItem && !getMainHandStack().isEmpty() && this.getWorld() instanceof ServerWorld world) {
            ItemStack dropped = getMainHandStack().copy();
            this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            ItemEntity item = new ItemEntity(world, getX(), getY(), getZ(), dropped);
            world.spawnEntity(item);
            ThiefItemTracker.refresh(world);
        }
        if (!this.suppressDeathReplay) {
            ThiefAllayReplay.recordKilled(this, killer, weapon, deathReason);
        }
        if (updateHud && this.getWorld() instanceof ServerWorld world) {
            ServerPlayerEntity owner = resolveOwner(world);
            if (owner != null) {
                ThiefPlayerComponent.KEY.get(owner).setTerminalState(getAllayNumber(), ThiefAllayState.DEAD);
            }
        }
        this.playSound(
                SoundEvents.ENTITY_ALLAY_DEATH,
                ThiefConstants.ALLAY_DEATH_SOUND_VOLUME,
                ThiefConstants.ALLAY_DEATH_SOUND_PITCH
        );
        discard();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient()) {
            return false;
        }
        if (this.getWorld() instanceof ServerWorld world
                && TimekeeperWorldComponent.KEY.get(world).isRewinding()) {
            return false;
        }
        if (source.isOf(DamageTypes.GENERIC_KILL) || source.isOf(DamageTypes.OUT_OF_WORLD)) {
            terminateAsDead(
                    source.getAttacker() instanceof ServerPlayerEntity player ? player : null,
                    source.getWeaponStack(),
                    null,
                    true,
                    true
            );
            return true;
        }
        ItemStack weapon = source.getWeaponStack();
        if (weapon != null && ThiefAllayCombatHandler.isSupportedDirectDamageWeapon(weapon)) {
            killByWeapon(
                    source.getAttacker() instanceof ServerPlayerEntity player ? player : null,
                    weapon,
                    ThiefAllayCombatHandler.resolveDeathReason(weapon)
            );
            return true;
        }
        return false;
    }

    @Override
    public void kill() {
        /* 管理员 /kill 等直接生命终止入口同样要掉落手持物并走最差兜底回放。 */
        terminateAsDead(null, ItemStack.EMPTY, null, true, true);
    }

    /**
     * 只有当本次扫过的真实碰撞全部来自允许穿越的门类方块时，才临时 noClip。
     * 这样悦灵能穿门，但门框旁的墙体仍然会正常阻挡，避免永久 noClip 带来的穿墙问题。
     */
    @Override
    public void move(MovementType movementType, Vec3d movement) {
        boolean passDoor = shouldIgnoreDoorCollision(movement);
        boolean previousNoClip = this.noClip;
        if (passDoor) {
            this.noClip = true;
        }
        try {
            super.move(movementType, movement);
        } finally {
            this.noClip = previousNoClip;
        }
    }

    private boolean shouldIgnoreDoorCollision(Vec3d movement) {
        if (movement.lengthSquared() <= ThiefConstants.ALLAY_MOVEMENT_EPSILON_SQUARED) {
            return false;
        }
        Box swept = this.getBoundingBox().stretch(movement).expand(ThiefConstants.ALLAY_DOOR_COLLISION_EXPANSION);
        boolean foundDoorCollision = false;
        for (BlockPos pos : BlockPos.iterate(
                MathHelper.floor(swept.minX), MathHelper.floor(swept.minY), MathHelper.floor(swept.minZ),
                MathHelper.floor(swept.maxX), MathHelper.floor(swept.maxY), MathHelper.floor(swept.maxZ)
        )) {
            BlockState state = this.getWorld().getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(this.getWorld(), pos);
            if (shape.isEmpty() || !shape.getBoundingBox().offset(pos).intersects(swept)) {
                continue;
            }
            if (!ThiefAllayDoorRules.canPassThrough(state)) {
                return false;
            }
            foundDoorCollision = true;
        }
        return foundDoorCollision;
    }

    private @Nullable ServerPlayerEntity resolveOwner(ServerWorld world) {
        return world.getServer().getPlayerManager().getPlayer(getOwnerUuid());
    }

    private @Nullable ServerPlayerEntity resolveTarget(ServerWorld world) {
        UUID targetUuid = getTargetUuid();
        if (targetUuid == null) {
            return null;
        }
        ServerPlayerEntity target = world.getServer().getPlayerManager().getPlayer(targetUuid);
        return target != null && target.getServerWorld() == world ? target : null;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.dataTracker.get(OWNER_UUID).isPresent()) {
            nbt.putUuid("ThiefOwner", getOwnerUuid());
        }
        if (getTargetUuid() != null) {
            nbt.putUuid("ThiefTarget", getTargetUuid());
        }
        nbt.putInt("ThiefAllayNumber", getAllayNumber());
        nbt.putInt("ThiefState", getThiefState().ordinal());
        nbt.putInt("PathRefreshTicks", this.pathRefreshTicks);
        nbt.putInt("ProgressCheckTicks", this.progressCheckTicks);
        nbt.putInt("NoProgressTicks", this.noProgressTicks);
        nbt.putInt("RouteFailureTicks", this.routeFailureTicks);
        nbt.putInt("PartialPathFailures", this.partialPathFailures);
        nbt.putInt("RecoveryWaypointTicks", this.recoveryWaypointTicks);
        nbt.putInt("MovementStallTicks", this.movementStallTicks);
        nbt.putBoolean("CurrentRouteReachesDestinationArea", this.currentRouteReachesDestinationArea);
        nbt.putDouble("LastSampleDistance", this.lastSampleDistance);
        if (this.lastPlannedDestination != null) {
            nbt.putLong("LastPlannedDestination", this.lastPlannedDestination.asLong());
        }
        if (this.recoveryWaypoint != null) {
            nbt.putLong("RecoveryWaypoint", this.recoveryWaypoint.asLong());
        }
        if (this.previousRecoveryWaypoint != null) {
            nbt.putLong("PreviousRecoveryWaypoint", this.previousRecoveryWaypoint.asLong());
        }
        if (this.lastMovementSamplePosition != null) {
            nbt.putDouble("LastMovementSampleX", this.lastMovementSamplePosition.x);
            nbt.putDouble("LastMovementSampleY", this.lastMovementSamplePosition.y);
            nbt.putDouble("LastMovementSampleZ", this.lastMovementSamplePosition.z);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(OWNER_UUID, nbt.containsUuid("ThiefOwner")
                ? Optional.of(nbt.getUuid("ThiefOwner"))
                : Optional.empty());
        this.dataTracker.set(TARGET_UUID, nbt.containsUuid("ThiefTarget")
                ? Optional.of(nbt.getUuid("ThiefTarget"))
                : Optional.empty());
        this.dataTracker.set(ALLAY_NUMBER, MathHelper.clamp(
                nbt.getInt("ThiefAllayNumber"),
                ThiefConstants.ALLAY_MIN_COUNT,
                ThiefConstants.ALLAY_MAX_COUNT
        ));
        this.dataTracker.set(STATE, ThiefAllayState.byOrdinal(nbt.getInt("ThiefState")).ordinal());
        this.pathRefreshTicks = Math.max(0, nbt.getInt("PathRefreshTicks"));
        this.progressCheckTicks = Math.max(0, nbt.getInt("ProgressCheckTicks"));
        this.noProgressTicks = Math.max(0, nbt.getInt("NoProgressTicks"));
        this.routeFailureTicks = Math.max(0, nbt.getInt("RouteFailureTicks"));
        this.partialPathFailures = Math.max(0, nbt.getInt("PartialPathFailures"));
        this.recoveryWaypointTicks = Math.max(0, nbt.getInt("RecoveryWaypointTicks"));
        this.movementStallTicks = Math.max(0, nbt.getInt("MovementStallTicks"));
        this.currentRouteReachesDestinationArea = nbt.getBoolean("CurrentRouteReachesDestinationArea");
        this.lastSampleDistance = nbt.contains("LastSampleDistance")
                ? Math.max(0.0D, nbt.getDouble("LastSampleDistance"))
                : Double.MAX_VALUE;
        this.lastPlannedDestination = nbt.contains("LastPlannedDestination")
                ? BlockPos.fromLong(nbt.getLong("LastPlannedDestination"))
                : null;
        this.recoveryWaypoint = nbt.contains("RecoveryWaypoint")
                ? BlockPos.fromLong(nbt.getLong("RecoveryWaypoint"))
                : null;
        this.previousRecoveryWaypoint = nbt.contains("PreviousRecoveryWaypoint")
                ? BlockPos.fromLong(nbt.getLong("PreviousRecoveryWaypoint"))
                : null;
        this.lastMovementSamplePosition = nbt.contains("LastMovementSampleX")
                && nbt.contains("LastMovementSampleY")
                && nbt.contains("LastMovementSampleZ")
                ? new Vec3d(
                nbt.getDouble("LastMovementSampleX"),
                nbt.getDouble("LastMovementSampleY"),
                nbt.getDouble("LastMovementSampleZ")
        )
                : this.getPos();
    }
}
