package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.api.movement.PlayerMovementApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 杰森重伤倒地的移动速度修正。
 *
 * <p>这里只接入 Wathe 公开的移动 API，不写新的 travel/getMovementSpeed 大 mixin。
 * 跳跃和交互封锁另有窄 mixin/事件兜底，移动速度本身保持在 API 汇总链里。</p>
 */
public final class JasonMovementHandler {
    private JasonMovementHandler() {
    }

    public static void init() {
        PlayerMovementApi.registerSpeedModifier(
                NoellesRolesCore.id("jason/wounded_crawl"),
                JasonConstants.WOUNDED_MOVEMENT_PRIORITY,
                context -> JasonWoundManager.isWoundedActionLocked(context.player())
                        ? PlayerMovementApi.MovementSpeedResult.override(JasonConstants.WOUNDED_CRAWL_SPEED)
                        : PlayerMovementApi.MovementSpeedResult.pass()
        );
        PlayerMovementApi.registerSpeedModifier(
                NoellesRolesCore.id("jason/ability_speed"),
                JasonConstants.ABILITY_MOVEMENT_PRIORITY,
                context -> JasonAbilityRules.isAbilityActiveLike(context.player())
                        ? PlayerMovementApi.MovementSpeedResult.multiply(JasonConstants.ABILITY_SPEED_MULTIPLIER)
                        : PlayerMovementApi.MovementSpeedResult.pass()
        );
    }
}
