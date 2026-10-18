package org.agmas.noellesroles.roles.swapper;

import dev.doctor4t.wathe.api.event.GameEvents;

/** 交换者尚未结算的延迟交换请求清理。 */
public final class SwapperResetHandler {
    private SwapperResetHandler() {}
    public static void init() {
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> SwapperAbility.clearPendingSwaps());
    }
}
