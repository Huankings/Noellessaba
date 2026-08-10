package org.agmas.noellesroles.roles.convener;

import dev.doctor4t.wathe.api.movement.PlayerMovementApi;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 召集者成功召集后的短时移速加成。
 */
public final class ConvenerMovementHandler {
    private static final int PRIORITY = 1600;

    private ConvenerMovementHandler() {
    }

    public static void init() {
        PlayerMovementApi.registerSpeedModifier(NoellesRolesCore.id("movement/convener_momentum"), PRIORITY, context -> {
            PlayerEntity player = context.player();
            if (!context.gameWorld().isRole(player, NoellesRoleRegistry.CONVENER)
                    || ConvenerMomentumComponent.KEY.get(player).getTicks() <= 0) {
                return PlayerMovementApi.MovementSpeedResult.pass();
            }

            /*
             * 召集者爆发是倍率加成，交给 Wathe 的移动 API 累计叠加，
             * 这样如果之后有词条或其它职业状态同时改速度，不会互相覆盖返回值。
             */
            return PlayerMovementApi.MovementSpeedResult.multiply((float) (1.0D + ConvenerConstants.SUMMON_SPEED_MULTIPLIER_BONUS));
        });
    }
}
