package org.agmas.noellesroles.roles.assassin;

import dev.doctor4t.wathe.api.event.GameEvents;
import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 刺客物品与能力冷却的玩家硬重置规则。 */
public final class AssassinResetHandler {
    private AssassinResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(AssassinCooldownHelper::reset);
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                HiddenBodiesWorldComponent.KEY.get(serverWorld).reset();
            }
        });
    }
}
