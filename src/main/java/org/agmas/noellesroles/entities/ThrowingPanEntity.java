package org.agmas.noellesroles.entities;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.roles.cook.CookConstants;
import org.agmas.noellesroles.roles.engineer.StunnedPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * 飞锅投掷实体。
 *
 * <p>结构复刻飞斧的“整段路径扫描 + 命中后继续飞行”，但命中结果不是击杀，
 * 而是复用平底锅的 5 秒眩晕、命中音效和眩晕结束回放。</p>
 */
public class ThrowingPanEntity extends PersistentProjectileEntity {
    private static final TrackedData<Byte> DATA_HIT_DIRECTION = DataTracker.registerData(
            ThrowingPanEntity.class,
            TrackedDataHandlerRegistry.BYTE
    );
    private static final TrackedData<ItemStack> SYNCED_STACK = DataTracker.registerData(
            ThrowingPanEntity.class,
            TrackedDataHandlerRegistry.ITEM_STACK
    );

    @Nullable
    private BlockPos stuckBlockPos = null;
    @Nullable
    private Direction stuckDirection = null;
    private int ticksAlive = 0;
    private int stuckTicks = 0;
    private boolean stuckInBlock = false;
    private final Set<Integer> hitEntities = new HashSet<>();

    public ThrowingPanEntity(EntityType<? extends ThrowingPanEntity> entityType, World world) {
        super(entityType, world);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    /**
     * 把原始飞锅物品栈同步给客户端渲染器。
     * 疯魔飞锅和普通飞锅共用实体类型，渲染端必须通过这份栈来显示正确贴图。
     */
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
        return new ItemStack(ModItems.THROWING_PAN);
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack synced = this.dataTracker.get(SYNCED_STACK);
        return !synced.isEmpty() ? synced : super.getItemStack();
    }

    @Override
    public void tick() {
        this.ticksAlive++;

        if (this.ticksAlive > CookConstants.THROWING_PAN_MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }

        if (this.stuckInBlock) {
            this.stuckTicks++;
            if (this.getItemStack().isOf(ModItems.PSYCHO_THROWING_PAN)
                    && this.stuckTicks > CookConstants.PSYCHO_THROWING_PAN_STUCK_LIFETIME_TICKS) {
                this.discard();
                return;
            }

            // 插着的方块消失时同步清理实体，避免半空留下不可交互的飞锅。
            if (!this.getWorld().isClient
                    && this.stuckBlockPos != null
                    && this.getWorld().getBlockState(this.stuckBlockPos).isAir()) {
                this.discard();
            }
            return;
        }

        if (!this.getWorld().isClient) {
            Vec3d currentPos = this.getPos();
            Vec3d velocity = this.getVelocity();
            Vec3d nextPos = currentPos.add(velocity);

            /*
             * 不使用父类单目标碰撞，而是扫描整段飞行轨迹上的所有玩家。
             * 这样飞锅可以像飞斧一样穿过多个玩家，并逐个造成眩晕。
             */
            Box searchBox = this.getBoundingBox().stretch(velocity).expand(CookConstants.THROWING_PAN_HIT_SCAN_BOX_EXPAND);
            for (Entity entity : this.getWorld().getOtherEntities(this, searchBox)) {
                if (!(entity instanceof ServerPlayerEntity target)) {
                    continue;
                }
                if (!target.canBeHitByProjectile() || this.hitEntities.contains(target.getId())) {
                    continue;
                }

                Box targetBox = target.getBoundingBox().expand(target.getTargetingMargin() + CookConstants.THROWING_PAN_PLAYER_HITBOX_EXPAND);
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
        this.stuckInBlock = true;
        this.playSound(
                SoundEvents.ITEM_TRIDENT_HIT_GROUND,
                CookConstants.THROWING_PAN_GROUND_HIT_SOUND_VOLUME,
                CookConstants.THROWING_PAN_GROUND_HIT_SOUND_PITCH
        );
    }

    @Nullable
    @Override
    protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
        // 真实贯穿命中由 tick() 扫描整段路径处理，父类单目标碰撞会让飞锅命中一人后停下。
        return null;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        if (!(entity instanceof ServerPlayerEntity target)) {
            return;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
            return;
        }

        Entity owner = this.getOwner();
        if (!(owner instanceof ServerPlayerEntity thrower) || owner.getUuid().equals(target.getUuid())) {
            return;
        }

        StunnedPlayerComponent.KEY.get(target).stun(CookConstants.PAN_STUN_TICKS, NoellesEventIds.PAN_STUN_END_EVENT);
        GameRecordManager.recordItemHit(thrower, ModItems.THROWING_PAN.getDefaultStack(), target, null);

        target.getWorld().playSound(
                null,
                target.getBlockPos(),
                SoundEvents.BLOCK_ANVIL_LAND,
                SoundCategory.PLAYERS,
                CookConstants.THROWING_PAN_PLAYER_HIT_SOUND_VOLUME,
                CookConstants.THROWING_PAN_PLAYER_HIT_SOUND_PITCH
        );

        // 命中后只轻微衰减速度，让它继续沿路径贯穿后续玩家。
        this.setVelocity(this.getVelocity().multiply(CookConstants.THROWING_PAN_HIT_VELOCITY_MULTIPLIER));
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

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
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
        // 飞锅和飞斧一样不允许被拾取，避免投掷武器落地后被回收破坏商店消耗规则。
    }
}
