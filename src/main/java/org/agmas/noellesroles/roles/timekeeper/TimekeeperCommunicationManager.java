package org.agmas.noellesroles.roles.timekeeper;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 时停者通信隔离。
 *
 * <p>时间狭缝和回溯播放都不是“静音某个人”这么简单：
 * 处于隔离状态的玩家不能发言，也不能听见外界发言。
 * Fabric 的 ALLOW_CHAT_MESSAGE 只能决定是否继续走原版广播，
 * 所以当任意接收者需要被隔离时，这里会取消原版广播，
 * 再手动把消息发给允许听见的人。</p>
 */
public final class TimekeeperCommunicationManager {
    private static boolean initialized;
    private static final ThreadLocal<Set<UUID>> COMMAND_MESSAGE_SENDERS =
            ThreadLocal.withInitial(HashSet::new);

    private TimekeeperCommunicationManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register(TimekeeperCommunicationManager::allowCommandMessage);
        ServerMessageEvents.COMMAND_MESSAGE.register(TimekeeperCommunicationManager::rememberCommandMessage);
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(TimekeeperCommunicationManager::handleChatMessage);
    }

    private static boolean allowCommandMessage(
            SignedMessage message,
            ServerCommandSource source,
            MessageType.Parameters params
    ) {
        /*
         * 管理员调试时经常会在回溯/时间狭缝期间执行 /say、/me 等带广播输出的指令。
         * 时停者的聊天隔离不能阻断命令系统本身，所以命令消息事件在本职业这里永远放行。
         * 注意：这里不做“下一跳聊天事件”的标记，因为其它模组仍可能在本事件后续监听器里拦截命令消息；
         * 标记放到 COMMAND_MESSAGE 里，只有真正通过 ALLOW_COMMAND_MESSAGE 的命令广播才会被记录。
         */
        return true;
    }

    private static void rememberCommandMessage(
            SignedMessage message,
            ServerCommandSource source,
            MessageType.Parameters params
    ) {
        /*
         * Fabric 文档说明：玩家执行的命令广播会先触发 COMMAND_MESSAGE，
         * 然后再进入 ALLOW_CHAT_MESSAGE / CHAT_MESSAGE。这里记录玩家 UUID，
         * 让紧随其后的聊天阶段能识别“这是指令输出，不是普通玩家聊天”。
         */
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            COMMAND_MESSAGE_SENDERS.get().add(player.getUuid());
        }
    }

    private static boolean handleChatMessage(
            SignedMessage message,
            ServerPlayerEntity sender,
            MessageType.Parameters params
    ) {
        if (consumeCommandMessageSender(sender)) {
            /*
             * 指令输入/执行必须独立于时停者的聊天隔离。
             * 普通聊天仍然会被下面的 shouldBlockCommunication 拦截；只有 Fabric 明确标记为
             * “命令产生的广播消息”的这一跳会直接放行，方便 OP 在测试回溯、狭缝时调试。
             */
            return true;
        }

        TimekeeperWorldComponent worldComponent = TimekeeperWorldComponent.KEY.get(sender.getServerWorld());

        /*
         * 发言者处于时间狭缝，或回溯期间是未受保护的存活玩家时，直接取消消息。
         * 狭缝本身每 tick 会持续 actionbar 提示剩余秒数，这里不再额外刷屏。
         */
        if (worldComponent.shouldBlockCommunication(sender)) {
            return false;
        }

        boolean hasBlockedRecipient = sender.getServer().getPlayerManager().getPlayerList().stream()
                .anyMatch(TimekeeperCommunicationManager::shouldHideMessageFromRecipient);
        if (!hasBlockedRecipient) {
            return true;
        }

        /*
         * 只要存在不能“听言”的玩家，就不能让原版广播直接发出去。
         * 改为手动发送给允许接收的玩家，保证隔离玩家不会通过文字聊天获得外界信息。
         */
        SentMessage outgoing = SentMessage.of(message);
        for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
            if (shouldHideMessageFromRecipient(recipient)) {
                continue;
            }
            outgoing.send(recipient, sender.shouldFilterMessagesSentTo(recipient), params);
        }
        return false;
    }

    private static boolean shouldHideMessageFromRecipient(@NotNull ServerPlayerEntity recipient) {
        return TimekeeperWorldComponent.KEY.get(recipient.getServerWorld()).shouldBlockCommunication(recipient);
    }

    private static boolean consumeCommandMessageSender(@NotNull ServerPlayerEntity sender) {
        Set<UUID> senders = COMMAND_MESSAGE_SENDERS.get();
        boolean wasCommandMessage = senders.remove(sender.getUuid());
        if (senders.isEmpty()) {
            COMMAND_MESSAGE_SENDERS.remove();
        }
        return wasCommandMessage;
    }
}
