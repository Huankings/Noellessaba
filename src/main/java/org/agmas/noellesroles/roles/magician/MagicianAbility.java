package org.agmas.noellesroles.roles.magician;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 魔术师主动能力总入口。
 *
 * <p>按键逻辑如下：</p>
 * <p>1. 冷却结束时按 G：开始录制；</p>
 * <p>2. 录制中按 G：提前结束录制；</p>
 * <p>3. 录制完成后按 G：开始播放；</p>
 * <p>4. 播放中按 G：提前结束播放。</p>
 */
public final class MagicianAbility {
    private static final Map<UUID, Long> LAST_ACCEPTED_ABILITY_TICK = new ConcurrentHashMap<>();

    private MagicianAbility() {
    }

    public static void handle(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.MAGICIAN)
                || !gameWorld.isRunning()
                || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        long worldTime = player.getServerWorld().getTime();
        Long lastAcceptedTick = LAST_ACCEPTED_ABILITY_TICK.get(player.getUuid());
        if (lastAcceptedTick != null && worldTime - lastAcceptedTick < MagicianConstants.ABILITY_KEY_DEBOUNCE_TICKS) {
            /*
             * 魔术师的 G 键是一个状态机开关：开始录制、结束录制、开始播放、结束播放都靠它。
             * 客户端理论上用 wasPressed() 只发一次，但键盘重复、帧边界或网络重发仍可能让服务端
             * 在极短时间里收到两次包；如果不在服务端去抖，第二个包会把刚开始的播放立刻关闭。
             */
            return;
        }
        LAST_ACCEPTED_ABILITY_TICK.put(player.getUuid(), worldTime);

        MagicianPlayerComponent magician = MagicianPlayerComponent.KEY.get(player);
        if (magician.isRecording()) {
            magician.finishRecording(true);
            return;
        }
        if (magician.isPlaying()) {
            MagicianPlaybackManager.stopPlaybackEarly(player);
            return;
        }
        if (magician.isReadyPlayback() || MagicianPlaybackManager.hasCachedRecording(player)) {
            MagicianPlaybackManager.startPlayback(player);
            return;
        }

        if (AbilityPlayerComponent.KEY.get(player).cooldown > 0) {
            return;
        }

        magician.startRecording();
    }
}
