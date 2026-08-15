package org.agmas.noellesroles.roles.convener;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonCommunicationManager;

/**
 * 召集者限时变形期间的普通聊天重定向。
 */
public final class ConvenerCommunicationManager {
    private static boolean initialized = false;

    private ConvenerCommunicationManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(ConvenerCommunicationManager::handleChatMessage);
    }

    private static boolean handleChatMessage(
            SignedMessage message,
            ServerPlayerEntity sender,
            MessageType.Parameters params
    ) {
        if (!ConvenerCommunicationHelper.shouldRestrictChat(sender)) {
            return true;
        }

        SentMessage outgoing = SentMessage.of(message);
        for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
            if (recipient == sender
                    || !ConvenerCommunicationHelper.canReceiveRestrictedChat(recipient)
                    || JasonCommunicationManager.shouldBlockChatBetween(sender, recipient)) {
                continue;
            }
            outgoing.send(recipient, sender.shouldFilterMessagesSentTo(recipient), params);
        }
        return false;
    }
}
