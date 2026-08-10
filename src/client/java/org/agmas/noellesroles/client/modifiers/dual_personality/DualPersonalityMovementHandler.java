package org.agmas.noellesroles.client.modifiers.dual_personality;

import dev.doctor4t.wathe.api.movement.PlayerMovementApi;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityConstants;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 双重人格双活阶段的客户端移速预测。
 */
public final class DualPersonalityMovementHandler {
    private static final int PRIORITY = 1700;

    private DualPersonalityMovementHandler() {
    }

    public static void init() {
        PlayerMovementApi.registerSpeedModifier(NoellesRolesCore.id("movement/dual_personality_double_active_client"), PRIORITY, context -> {
            if (!DualPersonalityClientState.isDoubleActive(context.player())) {
                return PlayerMovementApi.MovementSpeedResult.pass();
            }

            /*
             * 这条规则保留为客户端注册，是因为旧实现读取的就是客户端双活状态。
             * 迁到 Wathe MovementApi 后，不再需要 mixin PlayerEntity#getMovementSpeed，
             * 但仍保持“只处理本地预测手感”的原有边界。
             */
            return PlayerMovementApi.MovementSpeedResult.multiply(DualPersonalityConstants.DOUBLE_ACTIVE_SPEED_MULTIPLIER);
        });
    }
}
