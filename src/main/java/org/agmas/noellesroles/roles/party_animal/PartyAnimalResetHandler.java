package org.agmas.noellesroles.roles.party_animal;

import dev.doctor4t.wathe.api.event.GameEvents;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import dev.doctor4t.wathe.cca.GameWorldComponent;

/** 派对狂普通变声与回合临时状态清理。 */
public final class PartyAnimalResetHandler {
    private PartyAnimalResetHandler() {
    }

    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            PartyAnimalPlayerComponent.KEY.get(player).resetRound();
            // 普通变声随玩家回合状态清理，调试等级由组件独立保存并保留。
            HeliumBuzzPlayerComponent.KEY.get(player).clearNormalLevel();
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                for (var player : serverWorld.getPlayers()) {
                    PartyAnimalPlayerComponent.KEY.get(player).resetRound();
                    HeliumBuzzPlayerComponent.KEY.get(player).clearNormalLevel();
                }
            }
        });
        /* 玩家可能在结算时离线，无法由结束事件直接遍历到；下次回到服务器时按当前局状态兜底清理普通变声。 */
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            if (!GameWorldComponent.KEY.get(player.getWorld()).isRunning()) {
                HeliumBuzzPlayerComponent.KEY.get(player).clearNormalLevel();
                /* 玩家可能在结算时离线，重新进服时按完整回合默认值恢复，避免选择类型残留。 */
                PartyAnimalPlayerComponent.KEY.get(player).resetRound();
            }
        });
    }
}
