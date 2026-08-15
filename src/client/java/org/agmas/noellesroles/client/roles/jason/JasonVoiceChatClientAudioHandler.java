package org.agmas.noellesroles.client.roles.jason;

import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonAbilityRules;
import org.agmas.noellesroles.roles.jason.JasonConstants;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 杰森无恶不在期间的客户端语音接收压低。
 *
 * <p>服务端的 {@code VoiceDistanceEvent} 只能压短传播半径，无法改变已经送到客户端的 PCM 采样响度。
 * 这里再在接收端把“杰森活跃时的普通存活玩家语音”按常量压低，补上更明显的距离衰减感。</p>
 */
public final class JasonVoiceChatClientAudioHandler {
    private JasonVoiceChatClientAudioHandler() {
    }

    public static void register(EventRegistration registration) {
        registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class, JasonVoiceChatClientAudioHandler::handleEntitySound);
        registration.registerEvent(ClientReceiveSoundEvent.LocationalSound.class, JasonVoiceChatClientAudioHandler::handleLocationalSound);
        registration.registerEvent(ClientReceiveSoundEvent.StaticSound.class, JasonVoiceChatClientAudioHandler::handleStaticSound);
    }

    private static void handleEntitySound(ClientReceiveSoundEvent.EntitySound event) {
        dampenIfNeeded(event, event.getEntityId());
    }

    private static void handleLocationalSound(ClientReceiveSoundEvent.LocationalSound event) {
        /*
         * 位置型语音/音效通常也来自某个发声者或系统通道。
         * 这里只要事件携带的 sender UUID 能映射到玩家，就同样做压低处理。
         */
        dampenIfNeeded(event, event.getId());
    }

    private static void handleStaticSound(ClientReceiveSoundEvent.StaticSound event) {
        dampenIfNeeded(event, event.getId());
    }

    private static void dampenIfNeeded(ClientReceiveSoundEvent event, UUID sourceId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            return;
        }

        short[] rawAudio = event.getRawAudio();
        if (rawAudio == null || rawAudio.length == 0) {
            return;
        }

        PlayerEntity source = client.world.getPlayerByUuid(sourceId);
        float dampenProgress = getDampenProgress(client, source);
        if (dampenProgress <= 0.0F) {
            return;
        }

        /*
         * 初始音量也跟随无恶不在的进入 / 退出过渡：
         * 进度为 0 时保持原声，进度为 1 时压到常量指定倍率，中间帧线性过渡。
         */
        float volumeMultiplier = lerp(1.0F, JasonConstants.ABILITY_VOICE_VOLUME_MULTIPLIER, dampenProgress);
        short[] mutedAudio = rawAudio.clone();
        for (int index = 0; index < mutedAudio.length; index++) {
            mutedAudio[index] = scaleSample(mutedAudio[index], volumeMultiplier);
        }
        event.setRawAudio(mutedAudio);
    }

    private static float getDampenProgress(@NotNull MinecraftClient client, PlayerEntity source) {
        /*
         * 只在“本地听者 + 发声者”都是局内存活玩家时压低音量。
         * 这样不会影响创造/旁观调试视角，也不会改动死亡旁观者的回放听感。
         */
        if (!GameFunctions.isPlayerAliveAndSurvival(client.player) || source == null) {
            return 0.0F;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(source)) {
            return 0.0F;
        }
        if (JasonAbilityRules.isAliveJason(client.player) || JasonAbilityRules.isAliveJason(source)) {
            return 0.0F;
        }
        return getActiveJasonProgress(client);
    }

    private static float getActiveJasonProgress(@NotNull MinecraftClient client) {
        float progress = 0.0F;
        for (PlayerEntity player : client.world.getPlayers()) {
            progress = Math.max(progress, JasonAbilityRules.getAbilityTransitionProgress(player));
        }
        return progress;
    }

    private static short scaleSample(short sample, float multiplier) {
        int scaled = Math.round(sample * multiplier);
        if (scaled > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (scaled < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short) scaled;
    }

    private static float lerp(float from, float to, float progress) {
        float clampedProgress = Math.max(0.0F, Math.min(1.0F, progress));
        return from + (to - from) * clampedProgress;
    }
}
