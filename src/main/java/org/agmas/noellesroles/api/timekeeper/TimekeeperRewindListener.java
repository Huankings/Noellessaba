package org.agmas.noellesroles.api.timekeeper;

import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * 时停回溯生命周期监听器。
 *
 * <p>扩展职业的静态管理器、延迟任务、临时物品或自定义实体引用，可能无法仅靠
 * NBT 恢复。此监听器用于在回溯开始时暂停外部逻辑，在最终帧落地后重新计算缓存，
 * 以及在回合强制结束时清理外部状态。</p>
 */
public interface TimekeeperRewindListener {
    /**
     * 回溯已经建立保护名单并开始播放历史帧。
     */
    default void onRewindStarted(
            @NotNull ServerWorld world,
            @NotNull Set<UUID> protectedPlayers
    ) {
    }

    /**
     * 最终历史帧已经恢复，内置职业收束完成，扩展可以重建自己的缓存和引用。
     */
    default void onRewindFinished(@NotNull ServerWorld world) {
    }

    /**
     * 回合结束或世界重置时回溯被中断，扩展应清除本轮临时状态。
     */
    default void onRewindReset(@NotNull ServerWorld world) {
    }
}
