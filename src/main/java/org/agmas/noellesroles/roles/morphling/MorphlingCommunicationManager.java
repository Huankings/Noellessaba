package org.agmas.noellesroles.roles.morphling;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonCommunicationManager;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 变形试剂的原版聊天伪装。
 *
 * <p>聊天规则和语音规则保持同构：
 * 1. 正在被试剂伪装的玩家自己发言时，真实身份的原消息被取消；
 * 2. 样本玩家发言时，额外复制一份同内容消息，但显示成所有正在伪装成他的玩家。</p>
 *
 * <p>这个管理器在启动顺序上放在时停者、灵术师、召集者等通信隔离之后。
 * 如果那些规则已经取消并手动重发原消息，试剂不会再额外复制，避免绕过现有隔离边界。</p>
 */
public final class MorphlingCommunicationManager {
    private static boolean initialized;

    private MorphlingCommunicationManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(MorphlingCommunicationManager::handleChatMessage);
    }

    private static boolean handleChatMessage(
            SignedMessage message,
            ServerPlayerEntity sender,
            MessageType.Parameters params
    ) {
        if (MorphlingReagentService.isActivelyReagentDisguised(sender)) {
            /*
             * 被试剂变形的玩家自己发言时，活人不能看到真实身份消息。
             * 但非存活玩家属于观察视角，应该仍能看到真实聊天，方便回放外的局外观战和排查。
             * 因此这里取消原版全服广播，再手动只发给非存活接收者。
             */
            relayDisguisedPlayerRealChatToOutOfGame(message, sender, params);
            return false;
        }

        String rawContent = message.getSignedContent();
        if (rawContent == null || rawContent.isBlank()) {
            return true;
        }

        List<ServerPlayerEntity> disguisedPlayers = MorphlingReagentService.findActivePlayersDisguisedAs(sender);
        if (disguisedPlayers.isEmpty()) {
            return true;
        }

        for (ServerPlayerEntity disguisedPlayer : disguisedPlayers) {
            relayChatAsDisguisedPlayer(message, rawContent, sender, disguisedPlayer);
        }
        return true;
    }

    private static void relayDisguisedPlayerRealChatToOutOfGame(
            @NotNull SignedMessage message,
            @NotNull ServerPlayerEntity sender,
            @NotNull MessageType.Parameters params
    ) {
        SentMessage outgoing = SentMessage.of(message);
        for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
            if (!canReceiveDisguisedPlayerRealChat(sender, recipient)) {
                continue;
            }
            outgoing.send(recipient, sender.shouldFilterMessagesSentTo(recipient), params);
        }
    }

    private static boolean canReceiveDisguisedPlayerRealChat(
            @NotNull ServerPlayerEntity sender,
            @NotNull ServerPlayerEntity recipient
    ) {
        /*
         * “非存活玩家”统一走 Wathe 的旁观/创造非存活判定。
         * 时间狭缝和回溯通信隔离优先级更高，手动转发时也不能绕过它。
         */
        return GameFunctions.isPlayerSpectatingOrCreative(recipient)
                && !TimekeeperWorldComponent.KEY.get(sender.getServerWorld()).shouldBlockCommunication(sender)
                && !TimekeeperWorldComponent.KEY.get(recipient.getServerWorld()).shouldBlockCommunication(recipient)
                && !JasonCommunicationManager.shouldBlockChatBetween(sender, recipient);
    }

    private static void relayChatAsDisguisedPlayer(
            @NotNull SignedMessage sourceMessage,
            @NotNull String rawContent,
            @NotNull ServerPlayerEntity originalSpeaker,
            @NotNull ServerPlayerEntity disguisedPlayer
    ) {
        SignedMessage outgoingMessage = SignedMessage.ofUnsigned(disguisedPlayer.getUuid(), rawContent);
        MessageType.Parameters outgoingParams = MessageType.params(
                MessageType.CHAT,
                disguisedPlayer.getRegistryManager(),
                disguisedPlayer.getDisplayName()
        );
        SentMessage outgoing = SentMessage.of(outgoingMessage);

        for (ServerPlayerEntity recipient : originalSpeaker.getServer().getPlayerManager().getPlayerList()) {
            /*
             * 这里不跳过原说话者：和语音一样，如果样本玩家在伪装者附近/同聊天范围内，
             * 他也应该看到自己这句话被伪装者“复读”出来。
             */
            if (JasonCommunicationManager.shouldBlockChatBetween(originalSpeaker, recipient)
                    || JasonCommunicationManager.shouldBlockChatBetween(disguisedPlayer, recipient)) {
                continue;
            }
            outgoing.send(recipient, disguisedPlayer.shouldFilterMessagesSentTo(recipient), outgoingParams);
        }
    }
}
