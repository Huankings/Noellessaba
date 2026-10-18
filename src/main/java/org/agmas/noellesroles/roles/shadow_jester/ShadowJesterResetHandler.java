package org.agmas.noellesroles.roles.shadow_jester;

import dev.doctor4t.wathe.api.event.GameEvents;

/** 影子小丑世界阶段状态的回合结束清理。 */
public final class ShadowJesterResetHandler {
    private ShadowJesterResetHandler() {}
    public static void init() {
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                ShadowJesterComponent.KEY.get(serverWorld).clear();
            }
        });
    }
}
