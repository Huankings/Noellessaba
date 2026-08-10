package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 灵术师附身本体的目标可见性 API 接入。
 */
public final class SpiritualistTargetVisibilityHandler {
    private static boolean initialized = false;

    private SpiritualistTargetVisibilityHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TargetVisibilityApi.registerPlayerRule(
                NoellesRolesCore.id("visibility/spiritualist_possessing_body"),
                1000,
                context -> {
                    if (!SpiritualistBodyRules.isPossessingBody(context.target())) {
                        return TargetVisibilityApi.Decision.PASS;
                    }

                    /*
                     * 附身时灵术师真实本体是隐藏空气壳：不可渲染、不可准心选中、不可右键交互、不可被攻击。
                     * Wathe 本体已经把 TargetVisibilityApi 接进玩家渲染、准心、攻击和右键实体交互，
                     * 所以这里不再为这些入口单独写 Entity mixin。
                     */
                    return TargetVisibilityApi.Decision.DENY;
                }
        );
    }
}
