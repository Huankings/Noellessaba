package org.agmas.noellesroles.roles.licensed_villain;

import dev.doctor4t.wathe.api.event.GameEvents;

/** 执照恶棍全局时刻状态的回合结束清理。 */
public final class LicensedVillainResetHandler {
    private LicensedVillainResetHandler() {}
    public static void init() {
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                LicensedVillainMomentWorldComponent.KEY.get(serverWorld).reset();
            }
        });
    }
}
