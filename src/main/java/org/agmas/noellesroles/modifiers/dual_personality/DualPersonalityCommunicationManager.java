package org.agmas.noellesroles.modifiers.dual_personality;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 双重人格聊天事件入口。
 */
public final class DualPersonalityCommunicationManager {
    private static boolean initialized;

    private DualPersonalityCommunicationManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(DualPersonalityCommunicationManager::handleChatMessage);
    }

    private static boolean handleChatMessage(
            SignedMessage message,
            ServerPlayerEntity sender,
            MessageType.Parameters params
    ) {
        DualPersonalityCommunicationHelper.bridgeChatIfNeeded(message, sender);
        if (DualPersonalityCommunicationHelper.shouldRestrictChat(sender)) {
            DualPersonalityCommunicationHelper.redirectDormantChat(message, sender, params);
            return false;
        }
        return true;
    }
}
