package org.agmas.noellesroles.client.roles.vecna;

import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

/** 维克那近距离能力的准心目标解析。 */
public final class VecnaAbilityInputHandler {
    private VecnaAbilityInputHandler() {
    }

    public static int resolveTargetId(PlayerEntity player) {
        EntityHitResult target = WeaponTargetingApi.getVisibleAlivePlayerTarget(player, 2.0F);
        return target == null ? -1 : target.getEntity().getId();
    }
}
