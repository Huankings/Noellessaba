package org.agmas.noellesroles.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * 魔术师播放阶段的可见“皮套实体”。
 *
 * <p>这里最终没有继续走 {@link net.minecraft.entity.player.PlayerEntity} 继承路线，
 * 而是改成了自定义 {@link LivingEntity}，原因很直接：</p>
 * <p>1. 1.21.1 的 PlayerEntity 构造里会强绑 EntityType.PLAYER，无法安全保留自定义实体类型；</p>
 * <p>2. 我们仍然需要一个真正可注册、可追踪、可自定义渲染的数据实体；</p>
 * <p>3. Wathe 和扩展武器对它的“玩家命中兼容”，改由额外 mixin / 判定桥接来补。</p>
 *
 * <p>这个实体本身只负责三类事情：</p>
 * <p>1. 对客户端同步皮套当前的外观身份与装备；</p>
 * <p>2. 在服务端承载回放轨迹位置、姿态和手持物；</p>
 * <p>3. 被命中时，作为“这次播放的可见替身”交给统一的播放管理器收束。</p>
 */
public class MagicianPlaybackEntity extends LivingEntity {

    private static final TrackedData<Optional<UUID>> MAGICIAN_OWNER =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Optional<UUID>> DISGUISE_PLAYER =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<String> DISGUISE_NAME =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> REPLAY_SITTING =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> REPLAY_USING_ITEM =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> REPLAY_ACTIVE_OFF_HAND =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> REPLAY_ITEM_USE_TIME_LEFT =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> REPLAY_SWING_SEQUENCE =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> REPLAY_SWING_OFF_HAND =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    /**
     * 这里直接复用 LivingEntity 的装备同步链，
     * 让客户端 renderer / 持物特性层可以自然读取主手、副手和护甲槽内容。
     */
    private final ItemStack[] armor = new ItemStack[]{
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY
    };
    private ItemStack mainHand = ItemStack.EMPTY;
    private ItemStack offHand = ItemStack.EMPTY;
    private static final int REPLAY_SWING_DURATION_TICKS = 8;
    private int replaySwingTicks = 0;
    private Hand replaySwingHand = Hand.MAIN_HAND;

    public MagicianPlaybackEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(MAGICIAN_OWNER, Optional.empty());
        builder.add(DISGUISE_PLAYER, Optional.empty());
        builder.add(DISGUISE_NAME, "未知玩家");
        builder.add(REPLAY_SITTING, false);
        builder.add(REPLAY_USING_ITEM, false);
        builder.add(REPLAY_ACTIVE_OFF_HAND, false);
        builder.add(REPLAY_ITEM_USE_TIME_LEFT, 0);
        builder.add(REPLAY_SWING_SEQUENCE, 0);
        builder.add(REPLAY_SWING_OFF_HAND, false);
    }

    /**
     * 统一写入本次播放的“真实拥有者”和“皮套伪装对象”。
     *
     * <p>后续名字渲染、强制结束回放、生成尸体、回放文案补参数，
     * 都统一从这里读取，避免各条逻辑各自维护一份状态。</p>
     */
    public void setPlaybackIdentity(@Nullable UUID ownerUuid, @Nullable UUID disguiseUuid, @Nullable String disguiseName) {
        String resolvedName = disguiseName == null || disguiseName.isBlank() ? "未知玩家" : disguiseName;
        this.dataTracker.set(MAGICIAN_OWNER, Optional.ofNullable(ownerUuid));
        this.dataTracker.set(DISGUISE_PLAYER, Optional.ofNullable(disguiseUuid));
        this.dataTracker.set(DISGUISE_NAME, resolvedName);

        /*
         * 不再启用原版头顶名牌。
         *
         * Wathe 已经整体压掉了玩家头顶名字，魔术师皮套也应该只走
         * noellesroles 自己的 RoleNameRenderer 风格显示，否则靠近时会出现突兀的原版白字名牌。
         */
        this.setCustomName(null);
        this.setCustomNameVisible(false);
    }

    public @Nullable UUID getMagicianOwnerUuid() {
        return this.dataTracker.get(MAGICIAN_OWNER).orElse(null);
    }

    public @Nullable UUID getDisguisePlayerUuid() {
        return this.dataTracker.get(DISGUISE_PLAYER).orElse(null);
    }

    public String getDisguisePlayerName() {
        return this.dataTracker.get(DISGUISE_NAME);
    }

    public void setReplaySitting(boolean sitting) {
        this.dataTracker.set(REPLAY_SITTING, sitting);
    }

    public boolean isReplaySitting() {
        return this.dataTracker.get(REPLAY_SITTING);
    }

    public void setReplayUseState(boolean usingItem, @Nullable Hand activeHand) {
        this.dataTracker.set(REPLAY_USING_ITEM, usingItem && activeHand != null);
        this.dataTracker.set(REPLAY_ACTIVE_OFF_HAND, activeHand == Hand.OFF_HAND);
    }

    public boolean isReplayUsingItem() {
        return this.dataTracker.get(REPLAY_USING_ITEM);
    }

    public Hand getReplayActiveHand() {
        return this.dataTracker.get(REPLAY_ACTIVE_OFF_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    public int getReplayItemUseTimeLeft() {
        return Math.max(0, this.dataTracker.get(REPLAY_ITEM_USE_TIME_LEFT));
    }

    @Override
    public boolean isUsingItem() {
        /*
         * 客户端渲染和部分动作优化模组会直接读取原版 isUsingItem/getActiveItem。
         * 为了让匕首、飞斧、手雷等蓄力姿势恢复，客户端把录制状态伪装成原版使用状态；
         * 服务端绝不能这样做，否则 LivingEntity.tick() 会把皮套当成真实实体完成吃喝/释放物品。
         */
        if (this.getWorld().isClient() && this.isReplayUsingItem()) {
            return true;
        }
        return super.isUsingItem();
    }

    @Override
    public Hand getActiveHand() {
        if (this.getWorld().isClient() && this.isReplayUsingItem()) {
            return this.getReplayActiveHand();
        }
        return super.getActiveHand();
    }

    @Override
    public ItemStack getActiveItem() {
        if (this.getWorld().isClient() && this.isReplayUsingItem()) {
            return this.getStackInHand(this.getReplayActiveHand());
        }
        return super.getActiveItem();
    }

    @Override
    public int getItemUseTimeLeft() {
        if (this.getWorld().isClient() && this.isReplayUsingItem()) {
            return this.getReplayItemUseTimeLeft();
        }
        return super.getItemUseTimeLeft();
    }

    @Override
    public int getItemUseTime() {
        if (this.getWorld().isClient() && this.isReplayUsingItem()) {
            ItemStack activeItem = this.getActiveItem();
            return activeItem.isEmpty() ? 0 : Math.max(0, activeItem.getMaxUseTime(this) - this.getReplayItemUseTimeLeft());
        }
        return super.getItemUseTime();
    }

    public void playReplaySwing(Hand hand) {
        /*
         * 原版 swingHand 会给客户端发 EntityAnimationS2CPacket，但自定义播放体在复杂整合环境里
         * 偶尔会出现客户端没有明显挥手的情况。这里额外发一个数据追踪脉冲，
         * 客户端收到序号变化后会强制从第 0 tick 重新播放挥手动画。
         */
        this.dataTracker.set(REPLAY_SWING_OFF_HAND, hand == Hand.OFF_HAND);
        this.dataTracker.set(REPLAY_SWING_SEQUENCE, this.dataTracker.get(REPLAY_SWING_SEQUENCE) + 1);
        this.handSwinging = false;
        this.handSwingTicks = 0;
        super.swingHand(hand, true);
    }

    /**
     * 播放期间每 tick 都强制维持这些状态，尽量杜绝被环境物理干扰。
     */
    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);
        this.fallDistance = 0.0f;
        if (this.getWorld().isClient() && this.replaySwingTicks > 0) {
            this.replaySwingTicks--;
        }
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (REPLAY_SWING_SEQUENCE.equals(data) && this.getWorld().isClient()) {
            this.forceLocalSwing(this.dataTracker.get(REPLAY_SWING_OFF_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND);
        }
    }

    /**
     * 播放体永远不接受原版伤害。
     *
     * <p>真正能终止它的，是 Wathe / noellesroles 的玩法级命中逻辑额外调用的强制结束入口。
     * 这样可以避免火焰、摔落、窒息之类原版杂项伤害把播放流程提前打断。</p>
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isOf(DamageTypes.GENERIC_KILL) || source.isOf(DamageTypes.OUT_OF_WORLD)) {
            /*
             * 普通原版伤害不应打断播放，但 /kill、虚空清理这类管理员/世界级清理必须放行。
             * 之前这里无条件 return false，会导致命令显示杀死了实体但实体仍然残留。
             */
            this.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return !damageSource.isOf(DamageTypes.GENERIC_KILL) && !damageSource.isOf(DamageTypes.OUT_OF_WORLD);
    }

    @Override
    public boolean canTakeDamage() {
        return false;
    }

    @Override
    public void kill() {
        /*
         * /kill 对 LivingEntity 可能会走 kill() 而不是普通 damage()。
         * 这里直接丢弃，确保管理员命令和对局清理永远能把皮套实体删掉。
         */
        this.discard();
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    protected void pushAway(Entity entity) {
        // 皮套不应被其他实体挤开，保证轨迹复刻稳定。
    }

    @Override
    public void pushAwayFrom(Entity entity) {
        // 同样不对外施加互挤，避免在窄门口把别人推歪后影响回放轨迹。
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return java.util.List.of(this.armor);
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> this.mainHand;
            case OFFHAND -> this.offHand;
            case FEET -> this.armor[0];
            case LEGS -> this.armor[1];
            case CHEST -> this.armor[2];
            case HEAD -> this.armor[3];
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        ItemStack copy = stack.copy();
        switch (slot) {
            case MAINHAND -> this.mainHand = copy;
            case OFFHAND -> this.offHand = copy;
            case FEET -> this.armor[0] = copy;
            case LEGS -> this.armor[1] = copy;
            case CHEST -> this.armor[2] = copy;
            case HEAD -> this.armor[3] = copy;
            default -> {
            }
        }
    }

    public void clearEquipment() {
        this.mainHand = ItemStack.EMPTY;
        this.offHand = ItemStack.EMPTY;
        for (int index = 0; index < this.armor.length; index++) {
            this.armor[index] = ItemStack.EMPTY;
        }
    }

    public void setReplayItemUseTimeLeft(int ticks) {
        /*
         * 皮套实体只负责给客户端看，但举刀、蓄力这类动画会读 LivingEntity 的使用状态。
         * 因此播放帧同步 active hand 后，也要同步剩余使用时间，避免动画每 tick 被重置。
         */
        int safeTicks = Math.max(0, ticks);
        this.itemUseTimeLeft = safeTicks;
        if (!this.dataTracker.get(REPLAY_ITEM_USE_TIME_LEFT).equals(safeTicks)) {
            this.dataTracker.set(REPLAY_ITEM_USE_TIME_LEFT, safeTicks);
        }
    }

    private void forceLocalSwing(Hand hand) {
        this.replaySwingHand = hand;
        this.replaySwingTicks = REPLAY_SWING_DURATION_TICKS;
        this.handSwinging = false;
        this.handSwingTicks = 0;
        super.swingHand(hand, false);
    }

    public Hand getReplaySwingHand() {
        return this.replaySwingHand;
    }

    public float getReplaySwingProgress(float tickDelta) {
        if (this.replaySwingTicks <= 0) {
            return 0.0F;
        }

        /*
         * 额外的“模型级挥手进度”。
         *
         * 原版 handSwingProgress 在自定义实体 + 复杂客户端动画模组下偶尔不够稳定，
         * 所以客户端收到播放脉冲后，会额外保留 8 tick 的本地进度供 renderer 使用。
         */
        float remaining = Math.max(0.0F, this.replaySwingTicks - tickDelta);
        return 1.0F - remaining / (float) REPLAY_SWING_DURATION_TICKS;
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    public boolean shouldRenderName() {
        return false;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 999999.0);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.getMagicianOwnerUuid() != null) {
            nbt.putUuid("MagicianOwner", this.getMagicianOwnerUuid());
        }
        if (this.getDisguisePlayerUuid() != null) {
            nbt.putUuid("DisguisePlayer", this.getDisguisePlayerUuid());
        }
        nbt.putString("DisguisePlayerName", this.getDisguisePlayerName());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setPlaybackIdentity(
                nbt.containsUuid("MagicianOwner") ? nbt.getUuid("MagicianOwner") : null,
                nbt.containsUuid("DisguisePlayer") ? nbt.getUuid("DisguisePlayer") : null,
                nbt.getString("DisguisePlayerName")
        );
    }
}
