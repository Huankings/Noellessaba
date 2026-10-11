package org.agmas.noellesroles.client.roles.angel;

import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.angel.AngelAbility;

/** 天使能力键目标解析。 */
public final class AngelAbilityInputHandler {
    private AngelAbilityInputHandler() {
    }

    public static int resolveTargetId(PlayerEntity player) {
        PlayerEntity target = AngelAbility.getGenericGuardTarget(player);
        return target == null ? -1 : target.getId();
    }
}
