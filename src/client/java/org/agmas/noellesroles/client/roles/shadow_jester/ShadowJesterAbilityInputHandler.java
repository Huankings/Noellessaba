package org.agmas.noellesroles.client.roles.shadow_jester;

import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterConstants;

/** 影子小丑缔结能力的准心目标解析。 */
public final class ShadowJesterAbilityInputHandler {
    private ShadowJesterAbilityInputHandler() {
    }

    public static int resolveTargetId(PlayerEntity player) {
        EntityHitResult target = WeaponTargetingApi.getVisibleAlivePlayerTarget(
                player, ShadowJesterConstants.VOW_TARGET_RANGE_BLOCKS);
        return target == null ? -1 : target.getEntity().getId();
    }
}
