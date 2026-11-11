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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
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
    /** 同步目标玩家是否开启披风部位；目标离线时客户端使用该快照保持其显示设置。 */
    private static final TrackedData<Boolean> DISGUISE_CAPE_VISIBLE =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> REPLAY_SITTING =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> REPLAY_USING_ITEM =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> REPLAY_ACTIVE_OFF_HAND =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> REPLAY_ITEM_USE_TIME_LEFT =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** 录制玩家计算出的物品总使用时长；避免用非玩家皮套重新调用扩展物品的时长逻辑。 */
    private static final TrackedData<Integer> REPLAY_ITEM_USE_MAX_TIME =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** 录制位置当时是否处于水中，用来区分游泳和一格高匍匐。 */
    private static final TrackedData<Boolean> REPLAY_TOUCHING_WATER =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    /** 录制帧是否处于真正的鞘翅滑翔，而不只是使用 FALL_FLYING 姿态。 */
    private static final TrackedData<Boolean> REPLAY_FALL_FLYING =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    /** 录制玩家已经持续滑翔的 tick 数，保证播放从中途开始时立刻进入完整滑翔骨骼。 */
    private static final TrackedData<Integer> REPLAY_FALL_FLYING_TICKS =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** 原版玩家游泳/匍匐由 0 到 1 的身体前倾进度。 */
    private static final TrackedData<Float> REPLAY_LEANING_PITCH =
            DataTracker.registerData(MagicianPlaybackEntity.class, TrackedDataHandlerRegistry.FLOAT);
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
    /** 一次普通玩家挥手动画持续的 tick 数。 */
    private static final int REPLAY_SWING_DURATION_TICKS = 8;
    /** 原版开始周期性播放吃喝效果所需的已使用时长比例。 */
    private static final float CONSUMPTION_EFFECT_START_FRACTION = 0.21875F;
    /** 原版吃喝过程中两次声音/粒子效果之间的 tick 间隔。 */
    private static final int CONSUMPTION_EFFECT_INTERVAL_TICKS = 4;
    /** 原版吃喝过程每次产生的普通粒子数量。 */
    private static final int CONSUMPTION_PARTICLE_COUNT = 5;
    /** 原版完成食用时产生的加强粒子数量。 */
    private static final int CONSUMPTION_FINAL_PARTICLE_COUNT = 16;
    private int replaySwingTicks = 0;
    private Hand replaySwingHand = Hand.MAIN_HAND;
    /** 服务端最近一次已发出的挥手所在世界 tick，用于合并语义动作与挥手包。 */
    private long lastReplaySwingWorldTime = Long.MIN_VALUE;
    /** 与最近一次服务端挥手对应的手；只有同 tick 同手才视为重复。 */
    private Hand lastReplaySwingServerHand = Hand.MAIN_HAND;
    /** 客户端最近一次已经播放吃喝效果的剩余时长，避免网络帧重复时重复发声。 */
    private int lastConsumptionEffectTimeLeft = Integer.MIN_VALUE;
    /** 客户端最近一次吃喝效果对应的手。 */
    private Hand lastConsumptionEffectHand = Hand.MAIN_HAND;
    /** 客户端最近一次吃喝效果对应的物品副本。 */
    private ItemStack lastConsumptionEffectStack = ItemStack.EMPTY;
    /** 客户端上一 tick 的平滑前倾进度，供渲染帧插值。 */
    private float previousReplayLeaningPitch;
    /** 客户端当前平滑前倾进度；不会直接跳到服务端数据追踪目标。 */
    private float replayLeaningPitch;
    /** 原版身体俯冲动画约在前 10 tick 内完成，因此客户端只追踪这段有效滑翔时间。 */
    private static final float FALL_FLYING_TRANSITION_TICKS = 10.0F;
    /** 客户端上一 tick 的鞘翅过渡时间，供渲染帧插值。 */
    private float previousReplayFallFlyingTransitionTicks;
    /** 客户端当前鞘翅过渡时间；进入与退出都以每 tick 1.0 的原版时间速度变化。 */
    private float replayFallFlyingTransitionTicks;
    /** 蹲下或站起时每游戏 tick 推进的过渡比例；五 tick 完成一次完整动作。 */
    private static final float SNEAKING_TRANSITION_STEP = 0.2F;
    /** 客户端上一 tick 的蹲姿混合进度，供渲染帧插值。 */
    private float previousReplaySneakingTransition;
    /** 客户端当前蹲姿混合进度，0 为站立，1 为完整蹲姿。 */
    private float replaySneakingTransition;
    /** 披风跟随皮套位置的比例，复刻 1.21.1 PlayerEntity.updateCapeAngles 的四分之一平滑。 */
    private static final double CAPE_POSITION_FOLLOW = 0.25D;
    /** 披风与皮套相差超过此距离时重置历史位置，单位为方块。 */
    private static final double CAPE_POSITION_RESET_DISTANCE = 10.0D;
    /** 原版玩家用于披风步态振幅的最大水平速度。 */
    private static final float CAPE_STRIDE_SPEED_LIMIT = 0.1F;
    /** 原版玩家步幅振幅向当前水平速度收敛的比例。 */
    private static final float CAPE_STRIDE_FOLLOW = 0.4F;
    /** 上一 tick 的披风世界坐标，供渲染器做帧间插值。 */
    private double prevCapeX;
    private double prevCapeY;
    private double prevCapeZ;
    /** 当前经过平滑的披风世界坐标，用于产生跟随滞后。 */
    private double capeX;
    private double capeY;
    private double capeZ;
    /** 避免首次更新时从世界原点开始插值。 */
    private boolean capePositionInitialized;
    /** 原版 PlayerEntity 的前后步幅振幅；LivingEntity 没有这两个玩家专属字段，因此在皮套侧复刻。 */
    private float prevCapeStrideDistance;
    private float capeStrideDistance;

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
        builder.add(DISGUISE_CAPE_VISIBLE, true);
        builder.add(REPLAY_SITTING, false);
        builder.add(REPLAY_USING_ITEM, false);
        builder.add(REPLAY_ACTIVE_OFF_HAND, false);
        builder.add(REPLAY_ITEM_USE_TIME_LEFT, 0);
        builder.add(REPLAY_ITEM_USE_MAX_TIME, 0);
        builder.add(REPLAY_TOUCHING_WATER, false);
        builder.add(REPLAY_FALL_FLYING, false);
        builder.add(REPLAY_FALL_FLYING_TICKS, 0);
        builder.add(REPLAY_LEANING_PITCH, 0.0F);
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

    /** 设置目标玩家的披风部位显示标记，并通过实体数据追踪同步给观察者。 */
    public void setDisguiseCapeVisible(boolean visible) {
        this.dataTracker.set(DISGUISE_CAPE_VISIBLE, visible);
    }

    /** 读取目标离线时保留的披风显示标记。 */
    public boolean isDisguiseCapeVisible() {
        return this.dataTracker.get(DISGUISE_CAPE_VISIBLE);
    }

    /** 读取插值后的披风 X 坐标。 */
    public double getCapeX(float tickDelta) {
        return net.minecraft.util.math.MathHelper.lerp(tickDelta, this.prevCapeX, this.capeX);
    }

    /** 读取插值后的披风 Y 坐标。 */
    public double getCapeY(float tickDelta) {
        return net.minecraft.util.math.MathHelper.lerp(tickDelta, this.prevCapeY, this.capeY);
    }

    /** 读取插值后的披风 Z 坐标。 */
    public double getCapeZ(float tickDelta) {
        return net.minecraft.util.math.MathHelper.lerp(tickDelta, this.prevCapeZ, this.capeZ);
    }

    /** 读取插值后的原版等价步幅振幅。 */
    public float getCapeStrideDistance(float tickDelta) {
        return net.minecraft.util.math.MathHelper.lerp(tickDelta, this.prevCapeStrideDistance, this.capeStrideDistance);
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

    public int getReplayItemUseMaxTime() {
        return Math.max(0, this.dataTracker.get(REPLAY_ITEM_USE_MAX_TIME));
    }

    /**
     * 同步游泳、匍匐和鞘翅渲染所需的玩家专属状态。
     */
    public void setReplayMovementState(
            boolean swimming,
            boolean touchingWater,
            boolean fallFlying,
            int fallFlyingTicks,
            float leaningPitch
    ) {
        this.setSwimming(swimming);
        this.dataTracker.set(REPLAY_TOUCHING_WATER, touchingWater);
        this.dataTracker.set(REPLAY_FALL_FLYING, fallFlying);
        this.dataTracker.set(REPLAY_FALL_FLYING_TICKS, Math.max(0, fallFlyingTicks));
        this.dataTracker.set(REPLAY_LEANING_PITCH, Math.max(0.0F, Math.min(1.0F, leaningPitch)));
    }

    public boolean isReplayTouchingWater() {
        return this.dataTracker.get(REPLAY_TOUCHING_WATER);
    }

    @Override
    public boolean isFallFlying() {
        if (!this.getWorld().isClient()) {
            return this.dataTracker.get(REPLAY_FALL_FLYING);
        }
        /*
         * 退出滑翔后继续保持到本地过渡归零，让身体和翅膀有时间收回；
         * 开始滑翔的第一个同步帧则由追踪值立即打开动画分支。
         */
        return this.dataTracker.get(REPLAY_FALL_FLYING) || this.replayFallFlyingTransitionTicks > 0.0F;
    }

    @Override
    public int getFallFlyingTicks() {
        if (!this.getWorld().isClient()) {
            return this.isFallFlying() ? this.dataTracker.get(REPLAY_FALL_FLYING_TICKS) : 0;
        }
        return Math.round(this.replayFallFlyingTransitionTicks);
    }

    /**
     * 返回 0 到 1 的鞘翅过渡进度。
     *
     * <p>先在上一/当前 tick 间线性插值，再使用原版 PlayerEntityRenderer 的
     * {@code ticks * ticks / 100} 二次曲线，因此起飞初段较缓、随后自然加速。</p>
     */
    public float getReplayFallFlyingTransition(float tickDelta) {
        float transitionTicks = net.minecraft.util.math.MathHelper.lerp(
                tickDelta,
                this.previousReplayFallFlyingTransitionTicks,
                this.replayFallFlyingTransitionTicks
        );
        return net.minecraft.util.math.MathHelper.clamp(
                transitionTicks * transitionTicks
                        / (FALL_FLYING_TRANSITION_TICKS * FALL_FLYING_TRANSITION_TICKS),
                0.0F,
                1.0F
        );
    }

    /**
     * 返回经过客户端 tick 追踪和渲染帧插值后的蹲姿进度。
     */
    public float getReplaySneakingTransition(float tickDelta) {
        return net.minecraft.util.math.MathHelper.lerp(
                tickDelta,
                this.previousReplaySneakingTransition,
                this.replaySneakingTransition
        );
    }

    /**
     * 返回与 FA+Player {@code var.sneak} 相同的双余弦蹲姿缓动。
     *
     * <p>第一层余弦负责慢入慢出，第二层对进度开平方后再做余弦，形成资源包使用的
     * 前段蓄力、后段自然收束效果。未启用 EMF 时，Noelles 的兜底模型也使用相同节奏。</p>
     */
    public float getReplaySneakingEasedTransition(float tickDelta) {
        float progress = net.minecraft.util.math.MathHelper.clamp(
                this.getReplaySneakingTransition(tickDelta),
                0.0F,
                1.0F
        );
        float cosineEase = 0.5F - 0.5F * net.minecraft.util.math.MathHelper.cos(progress * (float) Math.PI);
        float squareRootCosineEase = 0.5F - 0.5F * net.minecraft.util.math.MathHelper.cos(
                net.minecraft.util.math.MathHelper.sqrt(progress) * (float) Math.PI
        );
        return cosineEase * squareRootCosineEase;
    }

    @Override
    public float getLeaningPitch(float tickDelta) {
        if (!this.getWorld().isClient()) {
            return this.dataTracker.get(REPLAY_LEANING_PITCH);
        }
        return net.minecraft.util.math.MathHelper.lerp(
                tickDelta,
                this.previousReplayLeaningPitch,
                this.replayLeaningPitch
        );
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
            return Math.max(0, this.getReplayItemUseMaxTime() - this.getReplayItemUseTimeLeft());
        }
        return super.getItemUseTime();
    }

    /**
     * 让 LivingEntityRenderer、原版玩家模型和动作兼容模组都能读到可靠的回放挥手进度。
     * 之前只有自定义模型内部读取本地计时，EMF 等在更早阶段读取实体进度时会得到 0。
     */
    @Override
    public float getHandSwingProgress(float tickDelta) {
        return Math.max(super.getHandSwingProgress(tickDelta), this.getReplaySwingProgress(tickDelta));
    }

    public void playReplaySwing(Hand hand) {
        if (!this.getWorld().isClient()) {
            long worldTime = this.getWorld().getTime();
            if (this.lastReplaySwingWorldTime == worldTime && this.lastReplaySwingServerHand == hand) {
                return;
            }
            this.lastReplaySwingWorldTime = worldTime;
            this.lastReplaySwingServerHand = hand;
        }
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
        if (this.getWorld().isClient()) {
            this.tickReplayLeaningPitch();
            this.tickReplayFallFlyingTransition();
            this.tickReplaySneakingTransition();
            this.tickCapePosition();
            this.tickCapeStrideDistance();
            this.tickReplayConsumptionEffects();
        }
        if (this.getWorld().isClient() && this.replaySwingTicks > 0) {
            this.replaySwingTicks--;
        }
    }

    /**
     * 按原版 LivingEntity 每 tick 0.09 的速度追赶录制目标，并保留上一 tick 数值供帧间插值。
     * 即使网络一次合并了多个数据追踪更新，也不会让身体突然跳到水平或站立姿态。
     */
    private void tickReplayLeaningPitch() {
        this.previousReplayLeaningPitch = this.replayLeaningPitch;
        float target = this.dataTracker.get(REPLAY_LEANING_PITCH);
        if (this.replayLeaningPitch < target) {
            this.replayLeaningPitch = Math.min(target, this.replayLeaningPitch + 0.09F);
        } else if (this.replayLeaningPitch > target) {
            this.replayLeaningPitch = Math.max(target, this.replayLeaningPitch - 0.09F);
        }
    }

    /**
     * 以原版游戏 tick 速度追踪鞘翅展开/收拢，并保留上一 tick 状态用于渲染帧插值。
     * 服务端偶尔合并数据包时也只会每客户端 tick 前进一步，不会突然跳过整段过渡。
     */
    private void tickReplayFallFlyingTransition() {
        this.previousReplayFallFlyingTransitionTicks = this.replayFallFlyingTransitionTicks;
        float targetTicks = this.dataTracker.get(REPLAY_FALL_FLYING)
                ? Math.min(FALL_FLYING_TRANSITION_TICKS, this.dataTracker.get(REPLAY_FALL_FLYING_TICKS))
                : 0.0F;
        if (this.replayFallFlyingTransitionTicks < targetTicks) {
            this.replayFallFlyingTransitionTicks = Math.min(
                    targetTicks,
                    this.replayFallFlyingTransitionTicks + 1.0F
            );
        } else if (this.replayFallFlyingTransitionTicks > targetTicks) {
            this.replayFallFlyingTransitionTicks = Math.max(
                    targetTicks,
                    this.replayFallFlyingTransitionTicks - 1.0F
            );
        }
    }

    /**
     * 在站立和蹲姿之间按固定游戏 tick 速度追踪，并保存上一 tick 供高帧率渲染插值。
     * 不直接使用布尔姿态作为模型终值，避免 EMF 动作资源包看到身体和鞘翅瞬间跳变。
     */
    private void tickReplaySneakingTransition() {
        this.previousReplaySneakingTransition = this.replaySneakingTransition;
        float target = this.isInSneakingPose() ? 1.0F : 0.0F;
        if (this.replaySneakingTransition < target) {
            this.replaySneakingTransition = Math.min(
                    target,
                    this.replaySneakingTransition + SNEAKING_TRANSITION_STEP
            );
        } else if (this.replaySneakingTransition > target) {
            this.replaySneakingTransition = Math.max(
                    target,
                    this.replaySneakingTransition - SNEAKING_TRANSITION_STEP
            );
        }
    }

    /**
     * 只在客户端复刻原版吃喝声音与粒子，绝不调用 ItemStack.finishUsing。
     *
     * <p>使用时长上限来自真实录制玩家，而不是让扩展物品用非玩家皮套重新计算，
     * 从而同时规避被动触发和不安全类型转换。</p>
     */
    private void tickReplayConsumptionEffects() {
        if (!this.isReplayUsingItem()) {
            this.resetReplayConsumptionEffectState();
            return;
        }

        Hand activeHand = this.getReplayActiveHand();
        ItemStack activeStack = this.getStackInHand(activeHand);
        UseAction useAction = activeStack.getUseAction();
        if (activeStack.isEmpty() || (useAction != UseAction.EAT && useAction != UseAction.DRINK)) {
            this.resetReplayConsumptionEffectState();
            return;
        }

        if (this.lastConsumptionEffectHand != activeHand
                || !ItemStack.areItemsAndComponentsEqual(this.lastConsumptionEffectStack, activeStack)) {
            this.lastConsumptionEffectTimeLeft = Integer.MIN_VALUE;
            this.lastConsumptionEffectHand = activeHand;
            this.lastConsumptionEffectStack = activeStack.copy();
        }

        int maxUseTime = this.getReplayItemUseMaxTime();
        int timeLeft = this.getReplayItemUseTimeLeft();
        int usedTicks = Math.max(0, maxUseTime - timeLeft);
        int effectStartTick = (int) (maxUseTime * CONSUMPTION_EFFECT_START_FRACTION);
        if (maxUseTime <= 0
                || usedTicks <= effectStartTick
                || timeLeft == this.lastConsumptionEffectTimeLeft
                || timeLeft % CONSUMPTION_EFFECT_INTERVAL_TICKS != 0) {
            return;
        }

        this.lastConsumptionEffectTimeLeft = timeLeft;
        int particleCount = timeLeft <= CONSUMPTION_EFFECT_INTERVAL_TICKS
                ? CONSUMPTION_FINAL_PARTICLE_COUNT
                : CONSUMPTION_PARTICLE_COUNT;
        this.spawnConsumptionEffects(activeStack, particleCount);
        this.playReplayConsumptionSound(activeStack, useAction);
    }

    /**
     * 从皮套实体位置直接播放客户端跟随声音。
     *
     * <p>{@link #spawnConsumptionEffects(ItemStack, int)} 内部调用的 Entity.playSound 会进入
     * {@code ClientWorld.playSound(except, ...)}；其 except 为 null，不等于本地玩家，因此客户端会
     * 丢弃声音但仍生成粒子。这里改走 ClientWorld 覆写的实体跟随声音入口，吃与喝都能正确听见。</p>
     */
    private void playReplayConsumptionSound(ItemStack stack, UseAction useAction) {
        SoundEvent sound;
        float pitch;
        if (useAction == UseAction.DRINK) {
            sound = stack.getDrinkSound();
            pitch = 0.9F + this.random.nextFloat() * 0.1F;
        } else {
            sound = stack.getEatSound();
            pitch = 0.5F + 0.5F * this.random.nextInt(2)
                    + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F;
        }
        this.getWorld().playSoundFromEntity(this, sound, SoundCategory.PLAYERS, 0.5F, pitch);
    }

    private void resetReplayConsumptionEffectState() {
        this.lastConsumptionEffectTimeLeft = Integer.MIN_VALUE;
        this.lastConsumptionEffectStack = ItemStack.EMPTY;
    }

    /**
     * 用 1.21.1 原版玩家的披风位置平滑规则维护动画历史；播放轨迹发生大幅跳变时重置，
     * 避免传送或实体校正把披风拖过场景。
     */
    private void tickCapePosition() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        if (!this.capePositionInitialized) {
            this.prevCapeX = this.capeX = x;
            this.prevCapeY = this.capeY = y;
            this.prevCapeZ = this.capeZ = z;
            this.capePositionInitialized = true;
            return;
        }

        this.prevCapeX = this.capeX;
        this.prevCapeY = this.capeY;
        this.prevCapeZ = this.capeZ;

        double deltaX = x - this.capeX;
        double deltaY = y - this.capeY;
        double deltaZ = z - this.capeZ;
        if (Math.abs(deltaX) > CAPE_POSITION_RESET_DISTANCE) {
            this.capeX = this.prevCapeX = x;
        }
        if (Math.abs(deltaY) > CAPE_POSITION_RESET_DISTANCE) {
            this.capeY = this.prevCapeY = y;
        }
        if (Math.abs(deltaZ) > CAPE_POSITION_RESET_DISTANCE) {
            this.capeZ = this.prevCapeZ = z;
        }

        this.capeX += deltaX * CAPE_POSITION_FOLLOW;
        this.capeY += deltaY * CAPE_POSITION_FOLLOW;
        this.capeZ += deltaZ * CAPE_POSITION_FOLLOW;
    }

    /**
     * 复刻 1.21.1 PlayerEntity.tick 中的步幅振幅更新，保证披风随步速及步态自然摆动。
     */
    private void tickCapeStrideDistance() {
        this.prevCapeStrideDistance = this.capeStrideDistance;
        // 皮套位置来自服务端轨迹插值，速度向量不一定随位置包更新，因此用实际水平位移还原回放步速。
        double deltaX = this.getX() - this.prevX;
        double deltaZ = this.getZ() - this.prevZ;
        float horizontalStep = (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float targetStrideDistance = !this.isOnGround() || this.isDead() || this.isSwimming()
                ? 0.0F
                : Math.min(CAPE_STRIDE_SPEED_LIMIT, horizontalStep);
        this.capeStrideDistance += (targetStrideDistance - this.capeStrideDistance) * CAPE_STRIDE_FOLLOW;
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

    public void setReplayItemUseTime(int ticksLeft, int maxTicks) {
        /*
         * 皮套实体只负责给客户端看，但举刀、蓄力这类动画会读 LivingEntity 的使用状态。
         * 因此播放帧同步 active hand 后，也要同步剩余使用时间，避免动画每 tick 被重置。
         */
        int safeTicksLeft = Math.max(0, ticksLeft);
        int safeMaxTicks = Math.max(0, maxTicks);
        this.itemUseTimeLeft = safeTicksLeft;
        if (!this.dataTracker.get(REPLAY_ITEM_USE_TIME_LEFT).equals(safeTicksLeft)) {
            this.dataTracker.set(REPLAY_ITEM_USE_TIME_LEFT, safeTicksLeft);
        }
        if (!this.dataTracker.get(REPLAY_ITEM_USE_MAX_TIME).equals(safeMaxTicks)) {
            this.dataTracker.set(REPLAY_ITEM_USE_MAX_TIME, safeMaxTicks);
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
