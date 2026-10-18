package org.agmas.noellesroles.roles.drugmaker;

import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.noellesroles.ModItems;

/** 制毒师没有独立组件，硬重置只清两件能力物品的实际冷却。 */
public final class DrugmakerResetHandler {
    private DrugmakerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            player.getItemCooldownManager().remove(ModItems.BLOWGUN);
            player.getItemCooldownManager().remove(ModItems.POISON_INJECTOR);
        });
    }
}
