package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;

/** 被招募者不能重新捡起被迫掉落的普通左轮。 */
public final class KillerQueenPickupRules {
    private KillerQueenPickupRules() {
    }

    public static boolean shouldBlockRevolverPickup(PlayerEntity player, ItemEntity item) {
        return item.getStack().isOf(WatheItems.REVOLVER)
                && KillerQueenWorldComponent.KEY.get(player.getWorld()).isRecruited(player.getUuid());
    }
}
