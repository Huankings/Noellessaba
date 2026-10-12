package org.agmas.noellesroles.api.timekeeper;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperSnapshots;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;

import java.util.UUID;

/**
 * NoellesRoles 时停回溯公开接入 API。
 *
 * <p>扩展职业只应依赖这个门面，不要直接修改 {@code TimekeeperSnapshots} 的内部
 * 白名单。CCA 组件适合保存职业常规运行态；非 CCA 状态使用
 * {@link TimekeeperSnapshotParticipant}；静态管理器和最终状态收束使用
 * {@link TimekeeperRewindListener}。</p>
 */
public final class TimekeeperSnapshotApi {
    private TimekeeperSnapshotApi() {
    }

    /** 注册最终历史帧同步的玩家 CCA 组件。 */
    public static void registerPlayerComponent(
            @NotNull Identifier id,
            @NotNull ComponentKey<? extends Component> key
    ) {
        registerPlayerComponent(id, key, false);
    }

    /**
     * 注册玩家 CCA 组件。
     *
     * @param syncOnIntermediateFrame 是否在回溯中间帧同步给客户端；外观、名字或
     *                                 目标状态才应开启，普通冷却和计数应保持 false
     */
    public static void registerPlayerComponent(
            @NotNull Identifier id,
            @NotNull ComponentKey<? extends Component> key,
            boolean syncOnIntermediateFrame
    ) {
        TimekeeperSnapshots.registerExternalPlayerComponent(id, key, syncOnIntermediateFrame);
    }

    /** 注册最终历史帧同步的世界 CCA 组件。 */
    public static void registerWorldComponent(
            @NotNull Identifier id,
            @NotNull ComponentKey<? extends Component> key
    ) {
        registerWorldComponent(id, key, false);
    }

    /** 注册世界 CCA 组件，并选择是否进行中间帧同步。 */
    public static void registerWorldComponent(
            @NotNull Identifier id,
            @NotNull ComponentKey<? extends Component> key,
            boolean syncOnIntermediateFrame
    ) {
        TimekeeperSnapshots.registerExternalWorldComponent(id, key, syncOnIntermediateFrame);
    }

    /** 注册一个负责非 CCA 状态捕获和恢复的扩展参与者。 */
    public static void registerParticipant(@NotNull TimekeeperSnapshotParticipant participant) {
        TimekeeperSnapshots.registerExternalParticipant(participant);
    }

    /**
     * 注册回溯生命周期监听器。
     *
     * <p>优先级越高越先收到回调。建议外部职业使用 0；只有明确依赖其它扩展收束
     * 顺序时才使用其它优先级。</p>
     */
    public static void registerRewindListener(
            @NotNull Identifier id,
            int priority,
            @NotNull TimekeeperRewindListener listener
    ) {
        TimekeeperSnapshots.registerExternalRewindListener(id, priority, listener);
    }

    /** 查询当前世界是否正在播放回溯历史帧。 */
    public static boolean isRewinding(@NotNull ServerWorld world) {
        return TimekeeperWorldComponent.KEY.get(world).isRewinding();
    }

    /** 查询玩家是否被当前这次回溯保护、不会被历史帧覆盖。 */
    public static boolean isProtectedFromCurrentRewind(
            @NotNull ServerWorld world,
            @NotNull UUID playerUuid
    ) {
        return TimekeeperWorldComponent.KEY.get(world).isProtectedFromCurrentRewind(playerUuid);
    }

    /** 查询某位玩家当前是否可以发动回溯。 */
    public static boolean canStartRewind(@NotNull ServerPlayerEntity player) {
        return TimekeeperWorldComponent.KEY.get(player.getServerWorld()).canStartRewind(player);
    }
}
