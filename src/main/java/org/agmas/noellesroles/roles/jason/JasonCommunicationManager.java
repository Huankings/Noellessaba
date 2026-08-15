package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 杰森“无恶不在”期间的文字与语音通信隔离。
 *
 * <p>能力期间的隔离目标很窄：只隔离“幽魂杰森”和“其他仍在局内存活的玩家”。
 * 非存活、旁观或创造玩家通常是复盘 / 管理调试视角，因此不被这条规则拦截。</p>
 */
public final class JasonCommunicationManager {
    private static boolean initialized;
    private static final ThreadLocal<Set<UUID>> COMMAND_MESSAGE_SENDERS = ThreadLocal.withInitial(HashSet::new);

    private JasonCommunicationManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        /*
         * 玩家执行 /me、/say 等命令广播时，Fabric 可能会在命令事件后继续触发聊天事件。
         * 无恶不在只隔离普通聊天和语音，不应该挡住管理员调试命令输出，所以这里记录命令来源并在聊天阶段放行。
         */
        ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register(JasonCommunicationManager::allowCommandMessage);
        ServerMessageEvents.COMMAND_MESSAGE.register(JasonCommunicationManager::rememberCommandMessage);
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(JasonCommunicationManager::handleChatMessage);
    }

    private static boolean allowCommandMessage(
            SignedMessage message,
            ServerCommandSource source,
            MessageType.Parameters params
    ) {
        return true;
    }

    private static void rememberCommandMessage(
            SignedMessage message,
            ServerCommandSource source,
            MessageType.Parameters params
    ) {
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
            return true;
        }

        boolean hasBlockedRecipient = sender.getServer().getPlayerManager().getPlayerList().stream()
                .anyMatch(recipient -> shouldBlockChatBetween(sender, recipient));
        if (!hasBlockedRecipient) {
            return true;
        }

        /*
         * 原版聊天没有按接收者取消的入口：只要有一个接收者不该看见，
         * 就取消默认广播，再手动发给允许接收的人。这样杰森自己仍能看到自己说的话，
         * 死亡/创造调试视角也仍可观察通信，但其他存活玩家不会互通到幽魂杰森。
         */
        SentMessage outgoing = SentMessage.of(message);
        for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
            if (shouldBlockChatBetween(sender, recipient)) {
                continue;
            }
            outgoing.send(recipient, sender.shouldFilterMessagesSentTo(recipient), params);
        }
        return false;
    }

    public static boolean shouldBlockChatBetween(
            @Nullable ServerPlayerEntity sender,
            @Nullable ServerPlayerEntity recipient
    ) {
        return shouldBlockActiveJasonAndLivingPlayer(sender, recipient);
    }

    public static boolean shouldBlockVoiceBetween(
            @Nullable ServerPlayerEntity sender,
            @Nullable ServerPlayerEntity recipient
    ) {
        return shouldBlockActiveJasonAndLivingPlayer(sender, recipient);
    }

    /**
     * 判断某个世界里是否存在正在无恶不在阶段的杰森。
     *
     * <p>语音距离压缩按世界生效：只有同一个世界 / 车厢对局里真的有幽魂杰森时，
     * 才压缩普通存活玩家之间的 proximity voice。</p>
     */
    public static boolean hasActiveAbilityInWorld(@Nullable ServerWorld world) {
        return getVoiceDampenProgressInWorld(world) > 0.0F;
    }

    /**
     * 获取当前世界的无恶不在语音压制进度。
     *
     * <p>理论上杰森最大生成数为 1，但调试或配置异常时可能存在多个杰森。
     * 这里取所有幽魂杰森中的最大进度，保证只要有一个杰森正在完全施压，语音环境就保持最高压制。</p>
     */
    public static float getVoiceDampenProgressInWorld(@Nullable ServerWorld world) {
        if (world == null) {
            return 0.0F;
        }
        float progress = 0.0F;
        for (ServerPlayerEntity player : world.getPlayers()) {
            progress = Math.max(progress, JasonAbilityRules.getAbilityTransitionProgress(player));
        }
        return progress;
    }

    private static boolean shouldBlockActiveJasonAndLivingPlayer(
            @Nullable ServerPlayerEntity first,
            @Nullable ServerPlayerEntity second
    ) {
        if (first == null || second == null || first.getUuid().equals(second.getUuid())) {
            return false;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(first) || !GameFunctions.isPlayerAliveAndSurvival(second)) {
            return false;
        }

        boolean firstIsPhasedJason = JasonAbilityRules.isAbilityActiveLike(first);
        boolean secondIsPhasedJason = JasonAbilityRules.isAbilityActiveLike(second);
        /*
         * 只有“幽魂杰森 <-> 其他存活玩家”被隔离。
         * 双方都不是幽魂杰森时，普通存活玩家之间仍能聊天；双方都是幽魂杰森的极端调试情况也不互相屏蔽。
         */
        return firstIsPhasedJason != secondIsPhasedJason;
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
