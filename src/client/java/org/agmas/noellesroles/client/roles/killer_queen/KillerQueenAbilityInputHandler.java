package org.agmas.noellesroles.client.roles.killer_queen;

import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenConstants;

/** 杀手皇后 G 键准心目标解析。 */
public final class KillerQueenAbilityInputHandler {
    private KillerQueenAbilityInputHandler() {
    }

    public static int resolveTargetId(PlayerEntity player) {
        EntityHitResult hit = WeaponTargetingApi.getVisibleAlivePlayerTarget(player, KillerQueenConstants.ABILITY_RANGE_BLOCKS);
        return hit == null ? -1 : hit.getEntity().getId();
    }
}
