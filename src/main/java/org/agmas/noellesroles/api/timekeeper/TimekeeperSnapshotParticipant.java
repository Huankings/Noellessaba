package org.agmas.noellesroles.api.timekeeper;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 非 CCA 状态的时停回溯参与者。
 *
 * <p>普通职业状态优先使用 CCA 玩家组件或世界组件注册；但扩展职业经常还会有
 * 静态 Map、延迟任务、自定义实体索引或其它不能挂在 CCA 上的运行态。此接口让
 * 扩展自己负责把这些状态编码成 NBT，并在每个历史帧恢复。回溯引擎不会猜测
 * 扩展状态的语义，也不会替扩展创建实体或清理任务。</p>
 *
 * <p>同一个参与者可以只实现玩家状态或只实现世界状态，未使用的方法返回
 * {@code null} / 空实现即可。恢复方法收到 {@code null} 时，表示目标历史帧
 * 没有该参与者的数据，扩展通常应把当前状态清空到“未创建”状态。</p>
 */
public interface TimekeeperSnapshotParticipant {
    /**
     * 用于在快照中区分参与者状态的稳定 id，必须使用参与者自己的命名空间。
     */
    @NotNull Identifier id();

    /**
     * 捕获整张世界的非 CCA 运行态。
     */
    default @Nullable NbtCompound captureWorldState(
            @NotNull ServerWorld world,
            @NotNull RegistryWrapper.WrapperLookup registryLookup
    ) {
        return null;
    }

    /**
     * 恢复整张世界的非 CCA 运行态。
     *
     * @param syncClientState 最终帧或显式开启中间帧同步时为 true
     */
    default void restoreWorldState(
            @NotNull ServerWorld world,
            @Nullable NbtCompound data,
            @NotNull RegistryWrapper.WrapperLookup registryLookup,
            boolean syncClientState
    ) {
    }

    /**
     * 捕获单个玩家的非 CCA 运行态。
     */
    default @Nullable NbtCompound capturePlayerState(
            @NotNull ServerPlayerEntity player,
            @NotNull RegistryWrapper.WrapperLookup registryLookup
    ) {
        return null;
    }

    /**
     * 恢复单个玩家的非 CCA 运行态。
     *
     * @param syncClientState 最终帧或显式开启中间帧同步时为 true
     */
    default void restorePlayerState(
            @NotNull ServerPlayerEntity player,
            @Nullable NbtCompound data,
            @NotNull RegistryWrapper.WrapperLookup registryLookup,
            boolean syncClientState
    ) {
    }
}
