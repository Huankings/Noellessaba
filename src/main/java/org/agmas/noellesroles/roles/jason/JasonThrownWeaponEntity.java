package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 杰森投掷武器实体。
 *
 * <p>它复刻 NoellesRoles 飞斧的“路径扫描贯穿玩家”结构，但把命中结果改为杰森专属：
 * 普通命中造成重伤倒地，达到第三次或倒地期间再次命中才真正死亡；油桶则只在落地时展开汽油/燃烧逻辑。</p>
 */
public class JasonThrownWeaponEntity extends PersistentProjectileEntity {
    private static final TrackedData<Byte> DATA_HIT_DIRECTION = DataTracker.registerData(
            JasonThrownWeaponEntity.class,
            TrackedDataHandlerRegistry.BYTE
    );
    private static final TrackedData<ItemStack> SYNCED_STACK = DataTracker.registerData(
            JasonThrownWeaponEntity.class,
            TrackedDataHandlerRegistry.ITEM_STACK
    );

    private @Nullable BlockPos stuckBlockPos;
    private @Nullable Direction stuckDirection;
    private int ticksAlive;
    private int stuckTicks;
    private boolean stuckInBlock;
    private final Set<Integer> hitEntities = new HashSet<>();

    public JasonThrownWeaponEntity(EntityType<? extends JasonThrownWeaponEntity> entityType, World world) {
        super(entityType, world);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    public void initFromStack(ItemStack stack) {
        ItemStack copy = stack.copy();
        this.setStack(copy);
        this.dataTracker.set(SYNCED_STACK, copy);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DATA_HIT_DIRECTION, (byte) 0);
        builder.add(SYNCED_STACK, ItemStack.EMPTY);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ModItems.THROWING_BLOOD_AXE);
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack synced = this.dataTracker.get(SYNCED_STACK);
        return !synced.isEmpty() ? synced : super.getItemStack();
    }

    @Override
    public void tick() {
        this.ticksAlive++;
        if (!this.stuckInBlock && this.ticksAlive > JasonConstants.DEFAULT_PROJECTILE_LIFETIME_TICKS) {
            cleanupBoundLighterIfJerryCan();
            this.discard();
            return;
        }

        if (this.stuckInBlock) {
            this.stuckTicks++;
            if (this.getItemStack().isOf(ModItems.THROWING_PICKAXE)
                    && this.stuckTicks > JasonConstants.PICKAXE_STUCK_LIFETIME_TICKS) {
                this.discard();
                return;
            }
            if (!this.getItemStack().isOf(ModItems.THROWING_PICKAXE)
                    && this.ticksAlive > JasonConstants.DEFAULT_PROJECTILE_LIFETIME_TICKS) {
                if (this.getItemStack().isOf(ModItems.THROWING_JERRY_CAN)) {
                    JasonFireWorldComponent.KEY.get(this.getWorld()).removeLandedCan(this.getUuid());
                }
                this.discard();
                return;
            }
            if (!this.getWorld().isClient && this.stuckBlockPos != null && this.getWorld().getBlockState(this.stuckBlockPos).isAir()) {
                if (this.getItemStack().isOf(ModItems.THROWING_JERRY_CAN)) {
                    JasonFireWorldComponent.KEY.get(this.getWorld()).removeLandedCan(this.getUuid());
                }
                this.discard();
            }
            return;
        }

        if (!this.getWorld().isClient && isPiercingWeapon()) {
            Vec3d currentPos = this.getPos();
            Vec3d velocity = this.getVelocity();
            Vec3d nextPos = currentPos.add(velocity);
            Box searchBox = this.getBoundingBox().stretch(velocity).expand(JasonConstants.HIT_SCAN_BOX_EXPAND);

            for (Entity entity : this.getWorld().getOtherEntities(this, searchBox)) {
                if (!(entity instanceof ServerPlayerEntity target)) {
                    continue;
                }
                if (!target.canBeHitByProjectile() || this.hitEntities.contains(target.getId())) {
                    continue;
                }

                Box targetBox = target.getBoundingBox().expand(target.getTargetingMargin() + JasonConstants.PLAYER_HITBOX_EXPAND);
                if (targetBox.raycast(currentPos, nextPos).isPresent()) {
                    this.hitEntities.add(target.getId());
                    this.onEntityHit(new EntityHitResult(target));
                }
            }
        }

        super.tick();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        this.stuckBlockPos = blockHitResult.getBlockPos();
        this.stuckDirection = blockHitResult.getSide();
        this.dataTracker.set(DATA_HIT_DIRECTION, (byte) this.stuckDirection.getId());

        Vec3d hitPos = blockHitResult.getPos();
        this.setPosition(hitPos.x, hitPos.y, hitPos.z);
        this.setVelocity(Vec3d.ZERO);

        if (!this.getWorld().isClient && this.getItemStack().isOf(ModItems.THROWING_JERRY_CAN)) {
            UUID ownerUuid = this.getOwner() instanceof PlayerEntity player ? player.getUuid() : null;
            JasonFireWorldComponent.KEY.get(this.getWorld()).addLandedCan(this.getUuid(), hitPos, ownerUuid);
            this.stuckInBlock = true;
            this.playSound(SoundEvents.ITEM_TRIDENT_HIT_GROUND, 1.0F, 1.0F);
            return;
        }

        this.stuckInBlock = true;
        this.playSound(SoundEvents.ITEM_TRIDENT_HIT_GROUND, 1.0F, 1.0F);
    }

    @Nullable
    @Override
    protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
        // 真实贯穿命中由 tick() 扫描整段飞行路径处理，父类单目标碰撞会破坏多目标贯穿。
        return null;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        ServerPlayerEntity thrower = this.getOwner() instanceof ServerPlayerEntity owner ? owner : null;

        if (MagicianServerHooks.stopPlaybackByWeaponTarget(
                entity,
                thrower,
                NoellesDeathReasons.JASON_THROWING_WEAPON_DEATH_REASON,
                MagicianServerHooks.getWeaponName(this.getItemStack())
        )) {
            this.playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.0F, 1.0F);
            this.setVelocity(this.getVelocity().multiply(0.92D, 0.92D, 0.92D));
            return;
        }

        if (!(entity instanceof ServerPlayerEntity target) || thrower == null) {
            return;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(target) || thrower.getUuid().equals(target.getUuid())) {
            return;
        }

        JasonWoundManager.handleThrowingWeaponHit(thrower, target, this.getItemStack());
        this.playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.0F, 1.0F);
        this.setVelocity(this.getVelocity().multiply(0.92D, 0.92D, 0.92D));
    }

    private boolean isPiercingWeapon() {
        ItemStack stack = this.getItemStack();
        return !stack.isOf(ModItems.THROWING_JERRY_CAN);
    }

    private void cleanupBoundLighterIfJerryCan() {
        /*
         * 极端情况下油桶可能一直没有命中方块就达到实体寿命。
         * 它不会进入 landedCans，也就不会触发自动燃烧；这里按实体 UUID 清掉对应打火机，
         * 避免玩家背包里留下永远无法点燃任何油桶的旧打火机。
         */
        if (!this.getWorld().isClient
                && this.getItemStack().isOf(ModItems.THROWING_JERRY_CAN)
                && this.getWorld() instanceof ServerWorld serverWorld) {
            JasonFireWorldComponent.removeOnceLightersForCan(serverWorld.getServer(), this.getUuid());
        }
    }

    @Override
    protected double getGravity() {
        return this.getItemStack().isOf(ModItems.THROWING_JERRY_CAN)
                ? JasonConstants.JERRY_CAN_GRAVITY
                : JasonConstants.THROWING_WEAPON_GRAVITY;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Age", this.ticksAlive);
        nbt.putInt("StuckAge", this.stuckTicks);
        nbt.putBoolean("StuckInBlock", this.stuckInBlock);
        if (this.stuckDirection != null) {
            nbt.putByte("HitDirection", (byte) this.stuckDirection.getId());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.ticksAlive = nbt.getInt("Age");
        this.stuckTicks = nbt.getInt("StuckAge");
        this.stuckInBlock = nbt.getBoolean("StuckInBlock");
        if (nbt.contains("HitDirection")) {
            this.stuckDirection = Direction.byId(nbt.getByte("HitDirection"));
            this.dataTracker.set(DATA_HIT_DIRECTION, nbt.getByte("HitDirection"));
        }
        this.dataTracker.set(SYNCED_STACK, super.getItemStack());
    }

    public int getTicksAlive() {
        return this.ticksAlive;
    }

    public boolean isStuckInBlock() {
        return this.stuckInBlock || this.stuckBlockPos != null;
    }

    public Direction getHitDirection() {
        if (this.stuckDirection != null) {
            return this.stuckDirection;
        }
        return Direction.byId(this.dataTracker.get(DATA_HIT_DIRECTION));
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        return false;
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        // 杰森投掷物不允许捡起，避免飞行/落地状态被玩家回收后破坏倒地和油桶结算。
    }
}
