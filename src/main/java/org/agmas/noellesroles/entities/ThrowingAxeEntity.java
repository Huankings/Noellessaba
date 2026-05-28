package org.agmas.noellesroles.entities;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
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
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * 飞斧实体。
 * 重点实现：
 * 1. 允许沿飞行路径贯穿多个玩家。
 * 2. 命中玩家后继续飞行，而不是像普通箭矢那样立刻停下。
 * 3. 撞墙后停留在方块表面，直到回合结束清理或生命周期结束。
 */
public class ThrowingAxeEntity extends PersistentProjectileEntity {

    /**
     * 飞斧每多成功贯穿击杀 1 人，额外奖励增加 x 金币。
     * 因此第 1 人奖励 x，第 2 人奖励 2x，第 3 人奖励 3x，以此类推。
     */
    private static final int BONUS_REWARD_PER_PENETRATION = 10;

    /**
     * 飞斧每 tick 检查贯穿命中时，对“整段飞行路径包围盒”追加的搜索扩张量。
     * 这个值越大，越容易把飞行轨迹附近的玩家纳入候选检测范围。
     *
     * 它影响的是“先搜到谁”，并不代表搜到后一定命中；
     * 真正是否判定命中，还要继续经过下面的命中盒射线检测。
     */
    private static final double HIT_SCAN_BOX_EXPAND = 1.6D;

    /**
     * 飞斧对玩家命中盒额外增加的判定半径。
     * 原版只会使用玩家自身的 targeting margin，这里再额外补一层扩张，
     * 让高速飞行时的擦边命中更宽松，整体手感更稳定。
     *
     * 后续如果你想继续放大或缩小“碰撞体积”，优先调整这个值即可。
     */
    private static final double PLAYER_HITBOX_EXPAND = 0.3D;

    private static final TrackedData<Byte> DATA_HIT_DIRECTION = DataTracker.registerData(
            ThrowingAxeEntity.class,
            TrackedDataHandlerRegistry.BYTE
    );
    private static final TrackedData<ItemStack> SYNCED_STACK = DataTracker.registerData(
            ThrowingAxeEntity.class,
            TrackedDataHandlerRegistry.ITEM_STACK
    );
    private static final int MAX_LIFETIME = 20 * 120;

    @Nullable
    private BlockPos stuckBlockPos = null;
    @Nullable
    private Direction stuckDirection = null;
    private int ticksAlive = 0;
    private boolean stuckInBlock = false;
    private final Set<Integer> hitEntities = new HashSet<>();

    /**
     * 记录这把飞斧已经“成功完成击杀”的贯穿人数。
     * 只有目标真的死亡后才会增加，护盾挡住或其他保命逻辑不会计入奖励层数。
     */
    private int successfulPenetrationKills = 0;

    public ThrowingAxeEntity(EntityType<? extends ThrowingAxeEntity> entityType, World world) {
        super(entityType, world);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    /**
     * 把原始 ItemStack 同步给实体渲染器。
     * 这样以后如果贴图或组件有扩展，渲染端也能直接复用。
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
        return new ItemStack(ModItems.THROWING_AXE);
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack synced = this.dataTracker.get(SYNCED_STACK);
        return !synced.isEmpty() ? synced : super.getItemStack();
    }

    @Override
    public void tick() {
        this.ticksAlive++;

        // 生命周期到达上限后自动清理，避免意外残留。
        if (this.ticksAlive > MAX_LIFETIME) {
            this.discard();
            return;
        }

        if (this.stuckInBlock) {
            // 如果飞斧插着的方块已经消失，就一并清理飞斧实体。
            if (!this.getWorld().isClient && this.stuckBlockPos != null
                    && this.getWorld().getBlockState(this.stuckBlockPos).isAir()) {
                this.discard();
            }
            return;
        }

        if (!this.getWorld().isClient) {
            Vec3d currentPos = this.getPos();
            Vec3d velocity = this.getVelocity();
            Vec3d nextPos = currentPos.add(velocity);

            // 这里不只检查最近命中的一个实体，而是扫描整个飞行段上的全部玩家。
            Box searchBox = this.getBoundingBox().stretch(velocity).expand(HIT_SCAN_BOX_EXPAND);
            for (Entity entity : this.getWorld().getOtherEntities(this, searchBox)) {
                if (!(entity instanceof ServerPlayerEntity serverPlayer)) {
                    continue;
                }
                if (!serverPlayer.canBeHitByProjectile()) {
                    continue;
                }
                if (this.hitEntities.contains(serverPlayer.getId())) {
                    continue;
                }

                // 命中盒使用“原版目标边距 + 飞斧额外判定扩张”的方式放大，
                // 方便后续单独调大或调小飞斧的实际命中宽度。
                Box targetBox = serverPlayer.getBoundingBox().expand(serverPlayer.getTargetingMargin() + PLAYER_HITBOX_EXPAND);
                if (targetBox.raycast(currentPos, nextPos).isPresent()) {
                    this.hitEntities.add(serverPlayer.getId());
                    this.onEntityHit(new EntityHitResult(serverPlayer));
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
        this.playSound(SoundEvents.ITEM_TRIDENT_HIT_GROUND, 1.0F, 1.0F);
    }

    /**
     * 关闭父类默认的“只处理一个实体碰撞”逻辑。
     * 真实的贯穿判定已经在 tick() 里自行完成。
     */
    @Nullable
    @Override
    protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
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
        if (owner != null && owner.getUuid().equals(target.getUuid())) {
            return;
        }

        ServerPlayerEntity killer = owner instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
        /*
         * 飞斧属于“投掷实体造成伤害”的武器。
         *
         * 如果这里只传 death reason，不把真实物品一起带进 killPlayer，
         * 那么后续像 wathe 疯魔护盾、梦之印记这类统一走 wathe 挡伤解析链的护盾，
         * 就只能拿到“被飞斧贯穿”这个死因，而拿不到真正的伤害物品 [飞斧]。
         *
         * 因此这里显式把飞斧自身的 ItemStack 写进额外回放数据，
         * 让所有护盾挡伤和真正死亡回放都能统一识别成飞斧物品来源。
         */
        ItemStack replayStack = this.getItemStack();
        if (replayStack.isEmpty()) {
            replayStack = new ItemStack(ModItems.THROWING_AXE);
        }
        NbtCompound replayDeathData = GameFunctions.createReplayItemData(target.getServerWorld(), replayStack);
        GameFunctions.killPlayer(target, true, killer, Noellesroles.DEATH_REASON_THROWING_AXE, replayDeathData);

        // 飞斧是一击必杀武器，但仍可能被护盾、免死等机制挡下。
        // 因此这里只在 killPlayer 执行后再次确认目标是否真的死亡，
        // 只有真正击杀成功时才累计贯穿层数并发放额外金币。
        if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
            handlePenetrationBonus(killer);
        }

        // 命中后只稍微衰减速度，保留贯穿的感觉。
        this.playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.0F, 1.0F);
        this.setVelocity(this.getVelocity().multiply(0.9D, 0.9D, 0.9D));
    }

    /**
     * 处理飞斧贯穿的额外金币奖励。
     *
     * 结算方式：
     * 1. 第一次成功击杀奖励 x。
     * 2. 第二次成功击杀奖励 2x。
     * 3. 第三次成功击杀奖励 3x。
     * 4. 累加后正好形成 x、3x、6x、10x…… 的总奖励曲线。
     *
     * 只对仍然存活、并且当前身份确实是“强盗”的投掷者发奖，
     * 这样即便物品被指令刷给别人，也不会误套用强盗的专属收益。
     */
    private void handlePenetrationBonus(@Nullable ServerPlayerEntity killer) {
        if (killer == null || !GameFunctions.isPlayerAliveAndSurvival(killer)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(killer.getWorld());
        if (!gameWorld.isRole(killer, Noellesroles.ROBBER)) {
            return;
        }

        this.successfulPenetrationKills++;
        int reward = BONUS_REWARD_PER_PENETRATION * this.successfulPenetrationKills;
        PlayerShopComponent.KEY.get(killer).addToBalance(reward);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Age", this.ticksAlive);
        nbt.putBoolean("StuckInBlock", this.stuckInBlock);
        nbt.putInt("SuccessfulPenetrationKills", this.successfulPenetrationKills);
        if (this.stuckDirection != null) {
            nbt.putByte("HitDirection", (byte) this.stuckDirection.getId());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.ticksAlive = nbt.getInt("Age");
        this.stuckInBlock = nbt.getBoolean("StuckInBlock");
        this.successfulPenetrationKills = nbt.getInt("SuccessfulPenetrationKills");
        if (nbt.contains("HitDirection")) {
            this.stuckDirection = Direction.byId(nbt.getByte("HitDirection"));
            this.dataTracker.set(DATA_HIT_DIRECTION, nbt.getByte("HitDirection"));
        }

        // 重新加载后把服务端保存的物品栈重新推给客户端渲染。
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

    @Nullable
    public BlockPos getStuckBlockPos() {
        return this.stuckBlockPos;
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        return false;
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        // 飞斧不允许被捡起，防止中途被回收破坏贯穿逻辑和残局结算。
    }
}
