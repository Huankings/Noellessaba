package org.agmas.noellesroles.roles.killer_queen;

import net.minecraft.server.network.ServerPlayerEntity;

/** 被招募者向仍存活好人/独立中立发送语音或文字时的单向隔离判定。 */
public final class KillerQueenCommunicationHelper {
    private KillerQueenCommunicationHelper() {
    }

    public static boolean shouldBlockBetween(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        if (sender == null || receiver == null || sender.getUuid().equals(receiver.getUuid())) return false;
        return KillerQueenWorldComponent.KEY.get(sender.getServerWorld()).isRecruited(sender.getUuid())
                && KillerQueenRules.isLivingGoodOrIndependent(receiver);
    }
}
