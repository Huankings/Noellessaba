package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.jetbrains.annotations.NotNull;

/**
 * 时停者回放记录辅助。
 *
 * <p>回放数据里只保存稳定枚举 id、价格和物品名翻译 key。
 * 真正显示中文或英文交给 formatter + lang，避免记录里写死某一种语言。</p>
 */
public final class TimekeeperReplayHelper {
    private TimekeeperReplayHelper() {
    }

    public static void recordWatchUse(
            @NotNull ServerPlayerEntity player,
            @NotNull ItemStack watchStack,
            @NotNull TimekeeperWatchMode mode,
            int cost
    ) {
        NbtCompound extra = new NbtCompound();
        extra.putString("watch_state", TimekeeperWatchItemAccess.stateId(watchStack));
        extra.putString("mode", mode.id());
        extra.putInt("cost", cost);
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.TIMEKEEPER_WATCH_USED_EVENT, player, extra);
    }

    public static void recordWatchBroken(@NotNull ServerPlayerEntity player, @NotNull ItemStack watchStack) {
        NbtCompound extra = new NbtCompound();
        extra.putString("watch_state", TimekeeperWatchItemAccess.stateId(watchStack));
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.TIMEKEEPER_WATCH_BROKEN_EVENT, player, extra);
    }

    public static void recordWatchRepair(@NotNull ServerPlayerEntity player, @NotNull ItemStack watchStack, int cost) {
        NbtCompound extra = new NbtCompound();
        extra.putString("watch_state", TimekeeperWatchItemAccess.stateId(watchStack));
        extra.putInt("cost", cost);
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.TIMEKEEPER_WATCH_REPAIRED_EVENT, player, extra);
    }

    public static void recordWatchUpgrade(
            @NotNull ServerPlayerEntity player,
            @NotNull ItemStack fromStack,
            @NotNull ItemStack toStack,
            int cost
    ) {
        NbtCompound extra = new NbtCompound();
        extra.putString("from_state", TimekeeperWatchItemAccess.stateId(fromStack));
        extra.putString("to_state", TimekeeperWatchItemAccess.stateId(toStack));
        extra.putInt("cost", cost);
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.TIMEKEEPER_WATCH_UPGRADED_EVENT, player, extra);
    }
}
