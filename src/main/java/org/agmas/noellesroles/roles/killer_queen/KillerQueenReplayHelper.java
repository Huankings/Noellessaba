package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 杀手皇后专属回放记录的小型统一入口。 */
public final class KillerQueenReplayHelper {
    private KillerQueenReplayHelper() {
    }

    public static void record(ServerPlayerEntity worldAnchor, Identifier event, @Nullable ServerPlayerEntity actor,
                              @Nullable UUID target, @Nullable UUID secondaryTarget) {
        NbtCompound extra = new NbtCompound();
        if (target != null) extra.putUuid("target", target);
        if (secondaryTarget != null) extra.putUuid("secondary_target", secondaryTarget);
        GameRecordManager.recordGlobalEvent(worldAnchor.getServerWorld(), event, actor, extra);
    }
}
