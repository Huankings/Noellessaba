package org.agmas.noellesroles.roles.robber;

import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.noellesroles.ModItems;

/** 强盗投斧和手枪实际物品冷却的玩家硬重置规则。 */
public final class RobberResetHandler {
    private RobberResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            player.getItemCooldownManager().remove(ModItems.THROWING_AXE);
            player.getItemCooldownManager().remove(ModItems.ROBBER_PISTOL);
        });
    }
}
