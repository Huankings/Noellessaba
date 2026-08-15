package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 无恶不在期间的玩家实体可见性 / 可选中 / 可交互 / 可攻击规则。
 */
public final class JasonTargetVisibilityHandler {
    private static boolean initialized;

    private JasonTargetVisibilityHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TargetVisibilityApi.registerPlayerRule(
                NoellesRolesCore.id("visibility/jason_ability"),
                JasonConstants.ABILITY_TARGET_VISIBILITY_PRIORITY,
                context -> {
                    if (context.viewer().getUuid().equals(context.target().getUuid())) {
                        return TargetVisibilityApi.Decision.PASS;
                    }

                    /*
                     * 其他存活玩家看幽魂杰森时，渲染、准心名字、右键交互和攻击都要隐藏/失效。
                     * 非存活或创造调试视角不走这里，方便复盘与管理员排查。
                     */
                    if (JasonAbilityRules.isAbilityActiveLike(context.target())) {
                        return GameFunctions.isPlayerAliveAndSurvival(context.viewer())
                                ? TargetVisibilityApi.Decision.DENY
                                : TargetVisibilityApi.Decision.PASS;
                    }

                    /*
                     * 作为代价，幽魂杰森本人也不能直接看到或锁定其它存活玩家。
                     * 红色粒子提示是单独的客户端表现，不依赖玩家实体渲染。
                     */
                    if (JasonAbilityRules.isAbilityActiveLike(context.viewer())
                            && GameFunctions.isPlayerAliveAndSurvival(context.target())) {
                        return TargetVisibilityApi.Decision.DENY;
                    }

                    return TargetVisibilityApi.Decision.PASS;
                }
        );
    }
}
