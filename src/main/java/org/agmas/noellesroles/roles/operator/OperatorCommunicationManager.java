package org.agmas.noellesroles.roles.operator;

import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 接线员的原版聊天桥接管理器。
 *
 * <p>这里和灵术师的通信管理器有一个关键区别：</p>
 * <p>1. 灵术师需要改写“谁能看到原聊天”；</p>
 * <p>2. 接线员只需要在保留原聊天的前提下，额外往指定玩家 actionbar 再打一份转发小字。</p>
 *
 * <p>因此这里不会拦截或取消原聊天，只把消息旁路复制给接线目标 / 广播目标。</p>
 */
public final class OperatorCommunicationManager {

    private OperatorCommunicationManager() {
    }

    public static void init() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(OperatorCommunicationManager::handleChatMessage);
    }

    private static boolean handleChatMessage(
            SignedMessage message,
            ServerPlayerEntity sender,
            MessageType.Parameters params
    ) {
        if (!isLiveConnectionEndpoint(sender)) {
            return true;
        }

        String rawContent = message.getSignedContent();
        if (rawContent == null || rawContent.isBlank()) {
            return true;
        }

        bridgeConnectedPairChat(sender, rawContent);
        bridgeBroadcastChat(sender, rawContent);
        return true;
    }

    private static void bridgeConnectedPairChat(@NotNull ServerPlayerEntity sender, @NotNull String rawContent) {
        for (ServerPlayerEntity possibleOperator : sender.getServer().getPlayerManager().getPlayerList()) {
            OperatorPlayerComponent component = OperatorPlayerComponent.KEY.get(possibleOperator);
            if (!component.hasActiveConnection()) {
                continue;
            }

            UUIDPair pair = getConnectionPair(component);
            if (pair == null) {
                continue;
            }

            ServerPlayerEntity other = null;
            if (sender.getUuid().equals(pair.first())) {
                other = sender.getServer().getPlayerManager().getPlayer(pair.second());
            } else if (sender.getUuid().equals(pair.second())) {
                other = sender.getServer().getPlayerManager().getPlayer(pair.first());
            }

            if (!isLiveConnectionEndpoint(other)) {
                continue;
            }

            sendBridgedActionbar(other, sender, rawContent);
        }
    }

    private static void bridgeBroadcastChat(@NotNull ServerPlayerEntity sender, @NotNull String rawContent) {
        for (ServerPlayerEntity possibleOperator : sender.getServer().getPlayerManager().getPlayerList()) {
            OperatorPlayerComponent component = OperatorPlayerComponent.KEY.get(possibleOperator);
            if (!component.hasActiveBroadcast() || component.getBroadcastTarget() == null) {
                continue;
            }
            if (!sender.getUuid().equals(component.getBroadcastTarget())) {
                continue;
            }

            for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
                if (recipient.getUuid().equals(sender.getUuid())) {
                    continue;
                }
                if (!isLiveConnectionEndpoint(recipient)) {
                    continue;
                }
                sendBridgedActionbar(recipient, sender, rawContent);
            }
        }
    }

    private static void sendBridgedActionbar(
            @NotNull ServerPlayerEntity recipient,
            @NotNull ServerPlayerEntity sender,
            @NotNull String rawContent
    ) {
        MutableText message = Text.translatable("message.noellesroles.operator.chat_bridge", sender.getGameProfile().getName(), rawContent)
                .withColor(Noellesroles.OPERATOR.color());
        recipient.sendMessage(message, true);
    }

    public static boolean isLiveConnectionEndpoint(@Nullable ServerPlayerEntity player) {
        return player != null && GameFunctions.isPlayerAliveAndSurvival(player);
    }

    public static @Nullable UUIDPair getConnectionPair(@NotNull OperatorPlayerComponent component) {
        if (!component.hasActiveConnection()
                || component.getConnectionPlayerOne() == null
                || component.getConnectionPlayerTwo() == null) {
            return null;
        }
        return new UUIDPair(component.getConnectionPlayerOne(), component.getConnectionPlayerTwo());
    }

    public record UUIDPair(java.util.UUID first, java.util.UUID second) {
    }
}
