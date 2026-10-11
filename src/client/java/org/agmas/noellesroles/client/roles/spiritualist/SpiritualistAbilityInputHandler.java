package org.agmas.noellesroles.client.roles.spiritualist;

import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistTargeting;

/** 灵术师能力键目标解析。附身视角和持续控制仍由 SpiritualistClientController 负责。 */
public final class SpiritualistAbilityInputHandler {
    private SpiritualistAbilityInputHandler() {
    }

    public static int resolveTargetId(PlayerEntity player) {
        PlayerEntity target = SpiritualistTargeting.getPossessionTarget(player);
        return target == null ? -1 : target.getId();
    }
}
