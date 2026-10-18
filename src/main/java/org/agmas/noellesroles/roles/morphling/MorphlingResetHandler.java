package org.agmas.noellesroles.roles.morphling;

import dev.doctor4t.wathe.api.event.GameEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 变形者自身变形状态的玩家硬重置规则。 */
public final class MorphlingResetHandler {
    private MorphlingResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            MorphlingPlayerComponent.KEY.get(player).reset();
            MorphMarkPlayerComponent.KEY.get(player).clear();
            if (player instanceof ServerPlayerEntity serverPlayer) {
                MorphlingReagentService.clearReagentReleaseGate(serverPlayer);
            }
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            MorphlingReagentService.clearAllReleaseGates();
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                MorphBodyDisguiseWorldComponent.KEY.get(serverWorld).reset();
            }
        });
    }
}
