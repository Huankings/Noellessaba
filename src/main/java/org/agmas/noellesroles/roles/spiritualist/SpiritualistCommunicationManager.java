package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 灵术师的聊天可见性管理器。
 *
 * <p>这里统一处理三条特殊通讯规则：</p>
 * <p>1. 灵魂出窍时，看不到别人发言，自己发言只给局外玩家；</p>
 * <p>2. 被附身者如果 somehow 成功发言，只允许灵术师和局外玩家看到；</p>
 * <p>3. 灵术师附身期间发言时，消息会伪装成宿主本人发出。</p>
 */
public final class SpiritualistCommunicationManager {

    private SpiritualistCommunicationManager() {
    }

    public static void init() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(SpiritualistCommunicationManager::handleChatMessage);
    }

    private static boolean handleChatMessage(
            SignedMessage message,
            ServerPlayerEntity sender,
            MessageType.Parameters params
    ) {
        ServerPlayerEntity possessionTarget = SpiritualistManager.getCurrentPossessionTarget(sender);
        SpiritualistPlayerComponent senderSpiritualist = SpiritualistPlayerComponent.KEY.get(sender);
        SpiritualistHostComponent senderHost = SpiritualistHostComponent.KEY.get(sender);

        boolean senderProjecting = senderSpiritualist.isProjecting();
        boolean senderPossessing = possessionTarget != null && senderSpiritualist.isPossessing();
        boolean senderIsPossessedHost = senderHost.possessed && senderHost.spiritualistController != null;
        boolean anyRecipientNeedsFiltering = sender.getServer().getPlayerManager().getPlayerList().stream()
                .anyMatch(recipient -> !canRecipientSeeMessage(sender, recipient, senderProjecting, senderPossessing, senderIsPossessedHost));

        if (!senderProjecting && !senderPossessing && !senderIsPossessedHost && !anyRecipientNeedsFiltering) {
            return true;
        }

        SignedMessage outgoingMessage = senderPossessing && possessionTarget != null
                ? SignedMessage.ofUnsigned(possessionTarget.getUuid(), message.getSignedContent())
                : message;
        MessageType.Parameters outgoingParams = senderPossessing && possessionTarget != null
                ? MessageType.params(MessageType.CHAT, sender.getRegistryManager(), possessionTarget.getDisplayName())
                : params;

        redirectChatMessage(
                sender,
                outgoingMessage,
                outgoingParams,
                senderProjecting,
                senderPossessing,
                senderIsPossessedHost
        );
        return false;
    }

    private static void redirectChatMessage(
            @NotNull ServerPlayerEntity sender,
            @NotNull SignedMessage message,
            @NotNull MessageType.Parameters params,
            boolean senderProjecting,
            boolean senderPossessing,
            boolean senderIsPossessedHost
    ) {
        SentMessage outgoing = SentMessage.of(message);

        for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
            if (!canRecipientSeeMessage(sender, recipient, senderProjecting, senderPossessing, senderIsPossessedHost)) {
                continue;
            }

            outgoing.send(recipient, sender.shouldFilterMessagesSentTo(recipient), params);
        }
    }

    private static boolean canRecipientSeeMessage(
            @NotNull ServerPlayerEntity sender,
            @NotNull ServerPlayerEntity recipient,
            boolean senderProjecting,
            boolean senderPossessing,
            boolean senderIsPossessedHost
    ) {
        if (senderProjecting) {
            return isOutOfGameAudience(recipient);
        }

        if (senderPossessing) {
            return !isProjectingRecipient(recipient, sender);
        }

        if (senderIsPossessedHost) {
            SpiritualistHostComponent hostComponent = SpiritualistHostComponent.KEY.get(sender);
            return isOutOfGameAudience(recipient)
                    || (hostComponent.spiritualistController != null
                    && hostComponent.spiritualistController.equals(recipient.getUuid()));
        }

        return !isProjectingRecipient(recipient, sender);
    }

    private static boolean isProjectingRecipient(@Nullable ServerPlayerEntity recipient, @Nullable ServerPlayerEntity sender) {
        if (recipient == null || recipient == sender) {
            return false;
        }
        return SpiritualistPlayerComponent.KEY.get(recipient).isProjecting();
    }

    public static boolean isOutOfGameAudience(@Nullable ServerPlayerEntity player) {
        return player != null && GameFunctions.isPlayerSpectatingOrCreative(player);
    }
}
