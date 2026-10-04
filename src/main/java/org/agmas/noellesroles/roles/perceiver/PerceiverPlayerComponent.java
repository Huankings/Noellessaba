package org.agmas.noellesroles.roles.perceiver;

import dev.doctor4t.wathe.api.blackout.BlackoutApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 感知者的选人效果、凝视进度和当前反向透视结果。
 *
 * <p>所有最终判定都在服务端完成。客户端只报告服务端无法直接读取的“本地本能是否开启，
     * 或本地是否已有被动描边”这一项；距离、视野角度、持续时间、停电与存活状态仍由本组件校验。</p>
 */
public final class PerceiverPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<PerceiverPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "perceiver"),
            PerceiverPlayerComponent.class
    );

    private static final String SELECTED_TARGET_KEY = "selectedTarget";
    private static final String SELECTED_TARGET_NAME_KEY = "selectedTargetName";
    private static final String SELECTED_EFFECT_TICKS_KEY = "selectedEffectTicks";
    private static final String GAZE_PROGRESS_KEY = "gazeProgress";
    private static final String AWARENESS_TTLS_KEY = "awarenessTtls";
    private static final String SENSED_PLAYERS_KEY = "sensedPlayers";
    private static final String PLAYER_KEY = "player";
    private static final String TICKS_KEY = "ticks";

    private final PlayerEntity player;

    @Nullable
    private UUID selectedTarget;
    private String selectedTargetName = "";
    private int selectedEffectTicks;

    /** 每名候选观察者已经连续看向感知者的 tick 数。 */
    private final Map<UUID, Integer> gazeProgress = new HashMap<>();

    /** 客户端“主动本能或被动描边有效”报告的剩余可信 tick。 */
    private final Map<UUID, Integer> awarenessReportTtls = new HashMap<>();

    /** 已满足连续凝视时间、当前应被感知者描边的玩家。 */
    private final Set<UUID> sensedPlayers = new HashSet<>();

    public PerceiverPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /** 回合重置或重新分配职业时无声清空，不产生技能结束回放。 */
    public void reset() {
        boolean changed = this.selectedTarget != null
                || this.selectedEffectTicks > 0
                || !this.gazeProgress.isEmpty()
                || !this.awarenessReportTtls.isEmpty()
                || !this.sensedPlayers.isEmpty();
        this.selectedTarget = null;
        this.selectedTargetName = "";
        this.selectedEffectTicks = 0;
        this.gazeProgress.clear();
        this.awarenessReportTtls.clear();
        this.sensedPlayers.clear();
        if (changed) {
            this.sync();
        }
    }

    public void startSelectedEffect(@NotNull ServerPlayerEntity target) {
        this.selectedTarget = target.getUuid();
        this.selectedTargetName = target.getGameProfile().getName();
        this.selectedEffectTicks = PerceiverConstants.SELECTED_EFFECT_DURATION_TICKS;
        this.sync();
    }

    public boolean hasSelectedEffect() {
        return this.selectedTarget != null && this.selectedEffectTicks > 0;
    }

    public boolean isSelectedViewer(@NotNull UUID viewerUuid) {
        return this.hasSelectedEffect() && viewerUuid.equals(this.selectedTarget);
    }

    public int getSelectedEffectTicks() {
        return this.selectedEffectTicks;
    }

    public boolean senses(@NotNull UUID targetUuid) {
        return this.sensedPlayers.contains(targetUuid);
    }

    /**
     * 接收某个客户端的感知资格心跳。
     *
     * <p>false 会立即撤销资格；true 只写入一个短时 TTL，客户端必须持续心跳，
     * 因此掉线、切换维度或停止渲染感知者都不会永久留下旧状态。</p>
     */
    public void updateAwarenessReport(@NotNull UUID viewerUuid, boolean aware) {
        if (aware) {
            this.awarenessReportTtls.put(viewerUuid, PerceiverConstants.AWARENESS_REPORT_TIMEOUT_TICKS);
        } else {
            this.awarenessReportTtls.remove(viewerUuid);
        }
    }

    @Override
    public void serverTick() {
        if (!(this.player instanceof ServerPlayerEntity perceiver)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(perceiver.getWorld());
        boolean activePerceiver = gameWorld.isRunning()
                && gameWorld.isRole(perceiver, NoellesRoleRegistry.PERCEIVER)
                && GameFunctions.isPlayerAliveAndSurvival(perceiver)
                && !TimekeeperPlayerComponent.KEY.get(perceiver).isInTimeRift();
        if (!activePerceiver) {
            this.reset();
            return;
        }

        this.tickSelectedEffect(perceiver);
        this.tickAwarenessReportTtls();
        this.tickGazeDetection(perceiver);
    }

    private void tickSelectedEffect(@NotNull ServerPlayerEntity perceiver) {
        if (!this.hasSelectedEffect()) {
            return;
        }

        /*
         * 目标掉线时 PlayerManager 返回 null。需求明确要求掉线不提前结束，
         * 所以 null 目标只让倒计时继续；目标重新上线后会继续收到同步组件并恢复被动描边。
         */
        ServerPlayerEntity target = perceiver.getServer().getPlayerManager().getPlayer(this.selectedTarget);
        if (target != null && (GameFunctions.isPlayerSpectatingOrCreative(target)
                || TimekeeperPlayerComponent.KEY.get(target).isInTimeRift())) {
            this.finishSelectedEffect(perceiver);
            return;
        }

        this.selectedEffectTicks--;
        if (this.selectedEffectTicks <= 0) {
            this.finishSelectedEffect(perceiver);
        } else if (this.selectedEffectTicks % 20 == 0) {
            this.sync();
        }
    }

    private void finishSelectedEffect(@NotNull ServerPlayerEntity perceiver) {
        UUID endedTarget = this.selectedTarget;
        String endedTargetName = this.selectedTargetName;

        this.selectedTarget = null;
        this.selectedTargetName = "";
        this.selectedEffectTicks = 0;
        this.sync();

        if (endedTarget == null) {
            return;
        }
        NbtCompound extra = new NbtCompound();
        extra.putUuid("target_player", endedTarget);
        extra.putString("target_player_name", endedTargetName);
        GameRecordManager.recordGlobalEvent(
                perceiver.getServerWorld(),
                NoellesEventIds.PERCEIVER_SELECTED_EFFECT_ENDED_EVENT,
                perceiver,
                extra
        );
    }

    private void tickAwarenessReportTtls() {
        Iterator<Map.Entry<UUID, Integer>> iterator = this.awarenessReportTtls.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    private void tickGazeDetection(@NotNull ServerPlayerEntity perceiver) {
        /*
         * 停电只关闭感知者“看见观察者”的能力；选中目标看见感知者的 30 秒效果仍保留。
         * 停电期间清空连续计时，恢复供电后必须重新满足配置的凝视时间，避免隔着停电续算进度。
         */
        if (BlackoutApi.isActive(perceiver.getWorld())) {
            boolean changed = !this.sensedPlayers.isEmpty();
            this.gazeProgress.clear();
            this.sensedPlayers.clear();
            if (changed) {
                this.sync();
            }
            return;
        }

        Set<UUID> validCandidates = new HashSet<>();
        Set<UUID> nextSensedPlayers = new HashSet<>();

        for (ServerPlayerEntity viewer : perceiver.getServerWorld().getPlayers()) {
            if (viewer == perceiver
                    || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                    || TimekeeperPlayerComponent.KEY.get(viewer).isInTimeRift()
                    || viewer.squaredDistanceTo(perceiver) > PerceiverConstants.PERCEPTION_DISTANCE_SQUARED) {
                continue;
            }

            UUID viewerUuid = viewer.getUuid();
            validCandidates.add(viewerUuid);

            boolean selectedPassiveVision = this.isSelectedViewer(viewerUuid);
            boolean clientReportedVision = this.awarenessReportTtls.containsKey(viewerUuid);
            boolean perceiverGlowing = perceiver.isGlowing();
            /*
             * 普通肉眼可见是第四种独立资格：即使玩家没有主动本能、没有任何被动描边，
             * 且感知者也未发光，只要双方之间无遮挡，观察者仍然能够“看见”感知者本人。
             * viewer.canSee 只用于这一普通可见分支；前三种特殊透视来源仍按需求允许隔墙生效。
             */
            boolean directlyVisible = viewer.canSee(perceiver);
            boolean qualifies = selectedPassiveVision || clientReportedVision || perceiverGlowing || directlyVisible;

            if (!qualifies || !isPerceiverWithinViewAngle(viewer, perceiver)) {
                this.gazeProgress.remove(viewerUuid);
                continue;
            }

            int progress = Math.min(
                    PerceiverConstants.REQUIRED_GAZE_TICKS,
                    this.gazeProgress.getOrDefault(viewerUuid, 0) + 1
            );
            this.gazeProgress.put(viewerUuid, progress);
            if (progress >= PerceiverConstants.REQUIRED_GAZE_TICKS) {
                nextSensedPlayers.add(viewerUuid);
            }
        }

        this.gazeProgress.keySet().removeIf(uuid -> !validCandidates.contains(uuid));
        if (!this.sensedPlayers.equals(nextSensedPlayers)) {
            this.sensedPlayers.clear();
            this.sensedPlayers.addAll(nextSensedPlayers);
            this.sync();
        }
    }

    private static boolean isPerceiverWithinViewAngle(
            @NotNull ServerPlayerEntity viewer,
            @NotNull ServerPlayerEntity perceiver
    ) {
        Vec3d toPerceiver = perceiver.getEyePos().subtract(viewer.getEyePos());
        if (toPerceiver.lengthSquared() < 1.0E-8D) {
            return true;
        }
        /*
         * 这里只比较观察方向和“观察者眼睛 -> 感知者眼睛”方向的夹角，不再要求准心射线
         * 命中感知者碰撞箱。因此准心移出身体后，只要感知者仍在配置的视野锥内，凝视就不会断开。
         * 方块遮挡不在这里处理；是否要求无遮挡由上方 directlyVisible 资格分支单独决定。
         */
        double dot = viewer.getRotationVector().normalize().dotProduct(toPerceiver.normalize());
        return dot >= PerceiverConstants.GAZE_DOT_THRESHOLD;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.selectedTarget != null) {
            tag.putUuid(SELECTED_TARGET_KEY, this.selectedTarget);
        }
        tag.putString(SELECTED_TARGET_NAME_KEY, this.selectedTargetName);
        tag.putInt(SELECTED_EFFECT_TICKS_KEY, this.selectedEffectTicks);
        tag.put(GAZE_PROGRESS_KEY, writeTickMap(this.gazeProgress));
        tag.put(AWARENESS_TTLS_KEY, writeTickMap(this.awarenessReportTtls));

        NbtList sensedList = new NbtList();
        for (UUID uuid : this.sensedPlayers) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid(PLAYER_KEY, uuid);
            sensedList.add(entry);
        }
        tag.put(SENSED_PLAYERS_KEY, sensedList);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.selectedTarget = tag.containsUuid(SELECTED_TARGET_KEY) ? tag.getUuid(SELECTED_TARGET_KEY) : null;
        this.selectedTargetName = tag.getString(SELECTED_TARGET_NAME_KEY);
        this.selectedEffectTicks = Math.max(0, tag.getInt(SELECTED_EFFECT_TICKS_KEY));

        this.gazeProgress.clear();
        readTickMap(tag.getList(GAZE_PROGRESS_KEY, NbtElement.COMPOUND_TYPE), this.gazeProgress);
        this.awarenessReportTtls.clear();
        readTickMap(tag.getList(AWARENESS_TTLS_KEY, NbtElement.COMPOUND_TYPE), this.awarenessReportTtls);

        this.sensedPlayers.clear();
        for (NbtElement element : tag.getList(SENSED_PLAYERS_KEY, NbtElement.COMPOUND_TYPE)) {
            NbtCompound entry = (NbtCompound) element;
            if (entry.containsUuid(PLAYER_KEY)) {
                this.sensedPlayers.add(entry.getUuid(PLAYER_KEY));
            }
        }
    }

    private static @NotNull NbtList writeTickMap(@NotNull Map<UUID, Integer> values) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Integer> value : values.entrySet()) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid(PLAYER_KEY, value.getKey());
            entry.putInt(TICKS_KEY, value.getValue());
            list.add(entry);
        }
        return list;
    }

    private static void readTickMap(@NotNull NbtList list, @NotNull Map<UUID, Integer> destination) {
        for (NbtElement element : list) {
            NbtCompound entry = (NbtCompound) element;
            if (entry.containsUuid(PLAYER_KEY)) {
                destination.put(entry.getUuid(PLAYER_KEY), Math.max(0, entry.getInt(TICKS_KEY)));
            }
        }
    }
}
