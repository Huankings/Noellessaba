package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.api.collision.PlayerCollisionApi;
import dev.doctor4t.wathe.api.collision.PlayerCollisionMode;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 无恶不在期间的玩家碰撞规则。
 */
public final class JasonPlayerCollisionHandler {
    private static boolean initialized;

    private JasonPlayerCollisionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PlayerCollisionApi.registerRule(
                NoellesRolesCore.id("collision/jason_ability"),
                JasonConstants.ABILITY_COLLISION_PRIORITY,
                context -> {
                    /*
                     * 幽魂杰森应能穿过其它玩家，也不应该把自己作为实体墙挡住别人。
                     * Wathe 的 PlayerCollisionApi 会同时处理移动碰撞 shape 和原版推挤入口。
                     */
                    return JasonAbilityRules.isAbilityActiveLike(context.self())
                            || JasonAbilityRules.isAbilityActiveLike(context.other())
                            ? PlayerCollisionMode.NO_COLLISION
                            : JasonConstants.ABILITY_DISABLE_OTHER_SURVIVOR_COLLISION
                            && JasonAbilityRules.hasActiveAbilityInWorld(context.world())
                            && GameFunctions.isPlayerAliveAndSurvival(context.self())
                            && GameFunctions.isPlayerAliveAndSurvival(context.other())
                            ? PlayerCollisionMode.NO_COLLISION
                            : PlayerCollisionMode.PASS;
                }
        );
    }
}
