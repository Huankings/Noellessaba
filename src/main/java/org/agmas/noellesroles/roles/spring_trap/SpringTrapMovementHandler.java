package org.agmas.noellesroles.roles.spring_trap;

import dev.doctor4t.wathe.api.movement.PlayerMovementApi;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 增速飞斧蓄力疾跑加速。
 *
 * <p>速度修正走 Wathe PlayerMovementApi，而不是玩家 travel/getMovementSpeed mixin。
 * 这样它能和其它职业/词条速度规则按优先级叠加，减少互相覆盖的风险。</p>
 */
public final class SpringTrapMovementHandler {
    private SpringTrapMovementHandler() {
    }

    public static void init() {
        PlayerMovementApi.registerSpeedModifier(
                NoellesRolesCore.id("movement/spring_trap_speed_axe_charge"),
                SpringTrapConstants.THROWING_SPEED_AXE_CHARGE_SPEED_PRIORITY,
                context -> {
                    if (!context.sprinting()
                            || !context.player().isUsingItem()
                            || !context.player().getActiveItem().isOf(ModItems.THROWING_SPEED_AXE)) {
                        return PlayerMovementApi.MovementSpeedResult.pass();
                    }
                    return PlayerMovementApi.MovementSpeedResult.multiply(SpringTrapConstants.THROWING_SPEED_AXE_CHARGE_SPEED_MULTIPLIER);
                }
        );
    }
}
