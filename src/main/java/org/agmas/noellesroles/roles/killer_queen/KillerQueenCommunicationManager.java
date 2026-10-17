package org.agmas.noellesroles.roles.killer_queen;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;

/** 被招募者的普通文字聊天按接收者重定向。 */
public final class KillerQueenCommunicationManager {
    private static boolean initialized;

    private KillerQueenCommunicationManager() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(KillerQueenCommunicationManager::handleChat);
    }

    private static boolean handleChat(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params) {
        boolean restricted = sender.getServer().getPlayerManager().getPlayerList().stream()
                .anyMatch(recipient -> KillerQueenCommunicationHelper.shouldBlockBetween(sender, recipient));
        if (!restricted) return true;
        SentMessage outgoing = SentMessage.of(message);
        for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
            if (!KillerQueenCommunicationHelper.shouldBlockBetween(sender, recipient)) {
                outgoing.send(recipient, sender.shouldFilterMessagesSentTo(recipient), params);
            }
        }
        return false;
    }
}
