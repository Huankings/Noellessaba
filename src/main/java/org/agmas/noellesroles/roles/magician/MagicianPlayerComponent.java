package org.agmas.noellesroles.roles.magician;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 魔术师个人状态组件。
 *
 * <p>这里维护三类数据：</p>
 * <p>1. 客户端 HUD / 选人界面需要同步的轻量状态：当前选中玩家、阶段、剩余时间；</p>
 * <p>2. 服务端录制期需要保存的轨迹帧与动作列表；</p>
 * <p>3. 播放期需要锁定的起点、终点、快照背包与外观资料。</p>
 *
 * <p>其中录制得到的大量数据只在服务端内存中保存，不写入 NBT。
 * 这样可以避免把几百帧轨迹同步到客户端，减小组件同步与存档负担。</p>
 */
public class MagicianPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<MagicianPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "magician"), MagicianPlayerComponent.class);

    private final PlayerEntity player;

    /**
     * 当前在背包界面里选中的目标。
     *
     * <p>和风灵师一样，默认永远先选自己。</p>
     */
    public UUID selectedTarget;

    /**
     * 记录最后一次成功选择时的目标名称。
     *
     * <p>这样即使目标在之后掉线，播放开始时仍然能继续使用稳定名字。</p>
     */
    public String selectedTargetName = "";

    /**
     * 记录最后一次成功选择时的 GameProfile 快照。
     *
     * <p>这里主要是为了保住皮肤属性：
     * 即使目标后来掉线，只要当初选中时他在线，这里仍然能保留 textures 属性，
     * 后续生成皮套时就能继续沿用这份外观。</p>
     */
    @Nullable
    public transient GameProfile selectedTargetProfileSnapshot;

    public MagicianStage stage = MagicianStage.IDLE;
    public int stageTicksRemaining = 0;

    /**
     * 当前录制已经走到第几 tick。
     *
     * <p>这个值只在录制期使用，用来给动作事件打时间戳。</p>
     */
    public int recordedTicks = 0;

    /**
     * 当前这一轮录制得到的所有轨迹帧。
     */
    public transient List<MagicianReplayFrame> recordedFrames = new ArrayList<>();

    /**
     * 当前这一轮录制得到的所有语义动作。
     */
    public transient List<MagicianRecordedAction> recordedActions = new ArrayList<>();

    /**
     * 录制开始时复制出来的一份背包快照。
     */
    @Nullable
    public transient MagicianInventorySnapshot inventorySnapshot;

    @Nullable public transient UUID playbackDisguiseUuid;
    public transient String playbackDisguiseName = "";
    @Nullable public transient GameProfile playbackProfileSnapshot;
    public transient int playbackPerformedTicks = 0;

    /**
     * 用于去重“同一 tick 同一只手的 use 事件”。
     *
     * <p>服务端一次右键在不同物品上，可能会先后经过：
     * 1. interactBlock；
     * 2. interactItem；
     * 3. 某些扩展物品自己的额外逻辑。
     *
     * <p>如果录制层完全不做去重，就会把同一帧的同一只手操作记成两到三次，
     * 回放阶段也会因此重复触发右键行为。这里保留一份最小状态专门收口这个问题。</p>
     */
    private transient int lastRecordedUseTick = Integer.MIN_VALUE;
    @Nullable private transient Hand lastRecordedUseHand = null;

    public transient double recordStartX;
    public transient double recordStartY;
    public transient double recordStartZ;
    public transient float recordStartYaw;
    public transient float recordStartPitch;

    public transient double recordEndX;
    public transient double recordEndY;
    public transient double recordEndZ;
    public transient float recordEndYaw;
    public transient float recordEndPitch;

    public MagicianPlayerComponent(PlayerEntity player) {
        this.player = player;
        this.selectedTarget = player.getUuid();
        this.selectedTargetName = this.getSafeOwnName();
        this.selectedTargetProfileSnapshot = this.createOwnProfileSnapshot();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 重置回到魔术师开局初始状态。
     *
     * <p>这里会：
     * 1. 默认重新选中自己；
     * 2. 停止录制 / 播放并清理缓存；
     * 3. 不在这里直接设置能力冷却，冷却由职业分配处理器统一写入。</p>
     */
    public void reset() {
        MagicianPlaybackManager.clearCachedRecording(this.player);
        this.selectedTarget = this.player.getUuid();
        this.selectedTargetName = this.getSafeOwnName();
        this.selectedTargetProfileSnapshot = this.createOwnProfileSnapshot();
        this.clearTransientRecordingState();
        this.stage = MagicianStage.IDLE;
        this.stageTicksRemaining = 0;
        this.sync();
    }

    public void setSelectedTarget(@NotNull UUID targetUuid, @NotNull String targetName, @Nullable GameProfile profileSnapshot) {
        this.selectedTarget = targetUuid;
        this.selectedTargetName = targetName;
        this.selectedTargetProfileSnapshot = profileSnapshot;
        this.sync();
    }

    public UUID getSelectedTarget() {
        return this.selectedTarget == null ? this.player.getUuid() : this.selectedTarget;
    }

    public String getSelectedTargetName() {
        return this.selectedTargetName == null || this.selectedTargetName.isBlank()
                ? this.getSafeOwnName()
                : this.selectedTargetName;
    }

    /**
     * 安全获取“自己”的名字。
     *
     * <p>CCA 会在 {@link PlayerEntity} 构造过程中非常早地创建组件，
     * 此时玩家的 GameProfile 还可能没有被写入。这里不能直接
     * player.getGameProfile().getName()，否则玩家进服时会因为组件构造异常被踢出。</p>
     */
    private String getSafeOwnName() {
        GameProfile profile = this.player.getGameProfile();
        if (profile != null && profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName();
        }

        UUID uuid = this.player.getUuid();
        if (uuid != null) {
            String uuidString = uuid.toString();
            return uuidString.substring(0, Math.min(8, uuidString.length()));
        }
        return "未知玩家";
    }

    /**
     * 创建自己的外观快照。
     *
     * <p>如果组件初始化时 GameProfile 尚未准备好，就先创建一份只含安全名字的临时快照。
     * 之后开局 reset 或选择玩家时会重新写入真实玩家资料，不影响正式皮套外观。</p>
     */
    private GameProfile createOwnProfileSnapshot() {
        GameProfile profile = this.player.getGameProfile();
        if (profile != null) {
            return profile;
        }
        return new GameProfile(this.player.getUuid(), this.getSafeOwnName());
    }

    public boolean isRecording() {
        return this.stage == MagicianStage.RECORDING;
    }

    public boolean isReadyPlayback() {
        return this.stage == MagicianStage.READY_PLAYBACK;
    }

    public boolean isPlaying() {
        return this.stage == MagicianStage.PLAYING;
    }

    public void startRecording() {
        MagicianPlaybackManager.clearCachedRecording(this.player);
        this.clearTransientRecordingState();
        this.stage = MagicianStage.RECORDING;
        this.stageTicksRemaining = MagicianConstants.RECORD_DURATION_TICKS;
        this.recordedTicks = 0;
        this.inventorySnapshot = MagicianInventorySnapshot.capture(this.player);

        this.recordStartX = this.player.getX();
        this.recordStartY = this.player.getY();
        this.recordStartZ = this.player.getZ();
        this.recordStartYaw = this.player.getYaw();
        this.recordStartPitch = this.player.getPitch();

        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordGlobalEvent(
                    serverPlayer.getServerWorld(),
                    Noellesroles.MAGICIAN_RECORDING_STARTED_EVENT,
                    serverPlayer,
                    null
            );
        }

        this.captureCurrentFrame();
        this.sync();
    }

    public void finishRecording(boolean stoppedEarly) {
        if (!this.isRecording()) {
            return;
        }

        if (this.recordedFrames.isEmpty()) {
            this.captureCurrentFrame();
        }

        this.recordEndX = this.player.getX();
        this.recordEndY = this.player.getY();
        this.recordEndZ = this.player.getZ();
        this.recordEndYaw = this.player.getYaw();
        this.recordEndPitch = this.player.getPitch();

        this.stage = MagicianStage.READY_PLAYBACK;
        this.stageTicksRemaining = 0;

        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            MagicianPlaybackManager.cacheFinishedRecording(serverPlayer, this);
            GameRecordManager.recordGlobalEvent(
                    serverPlayer.getServerWorld(),
                    stoppedEarly ? Noellesroles.MAGICIAN_RECORDING_STOPPED_EARLY_EVENT : Noellesroles.MAGICIAN_RECORDING_FINISHED_EVENT,
                    serverPlayer,
                    null
            );
        }
        this.sync();
    }

    public void beginPlaybackLock() {
        this.beginPlaybackLock(this.recordedFrames.size());
    }

    public void beginPlaybackLock(int playbackTicks) {
        this.stage = MagicianStage.PLAYING;
        this.stageTicksRemaining = Math.max(0, playbackTicks);
        this.playbackDisguiseUuid = this.getSelectedTarget();
        this.playbackDisguiseName = this.getSelectedTargetName();
        this.playbackProfileSnapshot = this.selectedTargetProfileSnapshot;
        this.playbackPerformedTicks = 0;
        this.sync();
    }

    public void finishPlaybackStage() {
        this.stage = MagicianStage.IDLE;
        this.stageTicksRemaining = 0;
        this.playbackDisguiseUuid = null;
        this.playbackDisguiseName = "";
        this.playbackProfileSnapshot = null;
        this.playbackPerformedTicks = 0;
        this.sync();
    }

    public void recordAction(@NotNull MagicianRecordedAction action) {
        if (!this.isRecording()) {
            return;
        }
        this.recordedActions.add(action);
    }

    /**
     * 供 mixin / 包接收器统一读取“当前这次录制已经走到第几 tick”。
     */
    public int getRecordedTick() {
        return this.recordedTicks;
    }

    /**
     * 记录一条普通左键攻击语义。
     */
    public void recordAttackAction() {
        this.recordAction(MagicianRecordedAction.attack(this.recordedTicks));
    }

    /**
     * 记录一条普通右键使用语义，并自动去重同 tick 同手的重复 use。
     */
    public void recordUseAction(@NotNull Hand hand) {
        if (!this.isRecording()) {
            return;
        }
        if (this.lastRecordedUseTick == this.recordedTicks && this.lastRecordedUseHand == hand) {
            return;
        }
        this.lastRecordedUseTick = this.recordedTicks;
        this.lastRecordedUseHand = hand;
        this.recordAction(MagicianRecordedAction.useHand(this.recordedTicks, hand));
    }

    /**
     * 记录一条方块右键语义，并保存真实命中的方块位置。
     *
     * <p>Wathe 的小门、按钮、床等方块交互很依赖玩家当时点中的方块面。
     * 如果播放期重新 raycast，皮套穿墙或门状态变化时就可能点不到原目标。</p>
     */
    public void recordUseBlockAction(@NotNull Hand hand, @NotNull BlockHitResult hitResult) {
        if (!this.isRecording()) {
            return;
        }
        if (this.lastRecordedUseTick == this.recordedTicks && this.lastRecordedUseHand == hand) {
            return;
        }
        this.lastRecordedUseTick = this.recordedTicks;
        this.lastRecordedUseHand = hand;
        this.recordAction(MagicianRecordedAction.useBlock(this.recordedTicks, hand, hitResult));
    }

    /**
     * 记录一次松开使用键的语义。
     */
    public void recordReleaseUseAction() {
        this.recordAction(MagicianRecordedAction.releaseUseItem(this.recordedTicks));
    }

    /**
     * 记录一次纯视觉挥手。
     *
     * <p>空挥、左键空气、某些门/按钮的客户端挥手不一定会进入 attack/use 语义。
     * 这条动作只负责播放手臂动画，不做伤害和交互，避免影响魔术师玩法归属。</p>
     */
    public void recordSwingAction(@NotNull Hand hand) {
        this.recordAction(MagicianRecordedAction.swingHand(this.recordedTicks, hand));
    }

    /**
     * 记录快捷栏切槽。
     */
    public void recordSelectSlotAction(int slot) {
        this.recordAction(MagicianRecordedAction.selectSlot(this.recordedTicks, slot));
    }

    /**
     * 记录一次枪击语义。
     */
    public void recordGunShootAction() {
        this.recordAction(MagicianRecordedAction.gunShoot(this.recordedTicks));
    }

    /**
     * 记录一次匕首刺杀语义。
     */
    public void recordKnifeStabAction() {
        this.recordAction(MagicianRecordedAction.knifeStab(this.recordedTicks));
    }

    /**
     * 记录一次刺刀瞬杀语义。
     */
    public void recordBayonetStabAction() {
        this.recordAction(MagicianRecordedAction.bayonetStab(this.recordedTicks));
    }

    /**
     * 记录一次刺刀击退语义。
     */
    public void recordBayonetKnockbackAction() {
        this.recordAction(MagicianRecordedAction.bayonetKnockback(this.recordedTicks));
    }

    /**
     * 记录一次狙击枪射击语义，并把锁定方向一起保存下来。
     */
    public void recordSniperShootAction(double x, double y, double z) {
        this.recordAction(MagicianRecordedAction.sniperShoot(this.recordedTicks, x, y, z));
    }

    @Override
    public void serverTick() {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!gameWorld.isRole(this.player, Noellesroles.MAGICIAN)
                || !gameWorld.isRunning()
                || !GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            if (this.isPlaying()) {
                MagicianPlaybackManager.stopPlaybackSilently(this.player);
            }
            this.reset();
            return;
        }

        if (this.isRecording()) {
            this.recordedTicks++;
            this.captureCurrentFrame();
            this.stageTicksRemaining--;
            if (this.stageTicksRemaining <= 0) {
                this.finishRecording(false);
            } else if (this.stageTicksRemaining % 5 == 0) {
                this.sync();
            }
            return;
        }

        if (this.isPlaying()) {
            if (this.player instanceof ServerPlayerEntity serverPlayer) {
                /*
                 * 播放驱动正常由 MagicianPlaybackManager 的全局 server tick 推进。
                 * 这里额外从组件 tick 再推一次，并由 manager 内部按世界时间去重。
                 * 这样即使某些整合环境里全局事件时序异常，皮套也不会卡在原地不动。
                 */
                MagicianPlaybackManager.tickPlaybackForOwner(serverPlayer);
            }
            if (!this.isPlaying()) {
                return;
            }
            this.stageTicksRemaining = Math.max(0, MagicianPlaybackManager.getPlaybackTicksRemaining(this.player));
            this.playbackPerformedTicks = MagicianPlaybackManager.getPlaybackPerformedTicks(this.player);
            if (this.stageTicksRemaining % 5 == 0 || this.stageTicksRemaining == 0) {
                this.sync();
            }
        }
    }

    private void captureCurrentFrame() {
        /*
         * 每帧直接走 MagicianReplayFrame.capture。
         *
         * 这样录制期间后买到的新道具、当前蓄力状态、睡觉床位等“瞬时状态”
         * 都会一起进入播放数据，不再只依赖录制开始时的静态背包快照。
         */
        this.recordedFrames.add(MagicianReplayFrame.capture(this.player));
    }

    private void clearTransientRecordingState() {
        this.recordedTicks = 0;
        this.recordedFrames = new ArrayList<>();
        this.recordedActions = new ArrayList<>();
        this.inventorySnapshot = null;
        this.playbackDisguiseUuid = null;
        this.playbackDisguiseName = "";
        this.playbackProfileSnapshot = null;
        this.playbackPerformedTicks = 0;
        this.recordStartX = 0.0D;
        this.recordStartY = 0.0D;
        this.recordStartZ = 0.0D;
        this.recordStartYaw = 0.0F;
        this.recordStartPitch = 0.0F;
        this.recordEndX = 0.0D;
        this.recordEndY = 0.0D;
        this.recordEndZ = 0.0D;
        this.recordEndYaw = 0.0F;
        this.recordEndPitch = 0.0F;
        this.lastRecordedUseTick = Integer.MIN_VALUE;
        this.lastRecordedUseHand = null;
    }

    /**
     * 播放完成后统一进入技能冷却。
     */
    public void applyPlaybackCooldown() {
        AbilityPlayerComponent.KEY.get(this.player).setCooldown(MagicianConstants.PLAYBACK_COOLDOWN_TICKS);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.selectedTarget != null) {
            tag.putUuid("selectedTarget", this.selectedTarget);
        }
        tag.putString("selectedTargetName", this.getSelectedTargetName());
        tag.putString("stage", this.stage.name());
        tag.putInt("stageTicksRemaining", this.stageTicksRemaining);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.selectedTarget = tag.containsUuid("selectedTarget") ? tag.getUuid("selectedTarget") : this.player.getUuid();
        this.selectedTargetName = tag.getString("selectedTargetName");
        try {
            this.stage = tag.contains("stage")
                    ? MagicianStage.valueOf(tag.getString("stage"))
                    : MagicianStage.IDLE;
        } catch (IllegalArgumentException exception) {
            this.stage = MagicianStage.IDLE;
        }
        this.stageTicksRemaining = Math.max(0, tag.getInt("stageTicksRemaining"));

        /*
         * CCA 的 readFromNbt 同时用于“服务端存档读取”和“服务端同步到客户端”。
         * 录制帧与播放上下文确实不能从服务端存档恢复，但客户端 HUD 又必须接收
         * RECORDING / PLAYING 这些轻量阶段。之前这里不区分两种来源，导致客户端
         * 每次收到同步包都会把 RECORDING / PLAYING 洗成 IDLE，于是 HUD 只会显示
         * “开始录制 / 开始播放”，看不到录制中、播放中和剩余时间。
         */
        if (!this.player.getWorld().isClient()) {
            this.clearTransientRecordingState();
            this.stage = MagicianStage.IDLE;
            this.stageTicksRemaining = 0;
        }
    }
}
