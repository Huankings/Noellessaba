package org.agmas.noellesroles.roles.assassin;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 刺客隐藏尸体接入 Wathe 目标可见性 API。
 *
 * <p>旧版这里拆成两个客户端 mixin：一个取消尸体渲染，一个让尸体不能被准心选中。
 * 现在统一注册到 Wathe API，让渲染、准心、RoleNameHud 和服务端玩法交互都能复用同一套规则。</p>
 */
public final class AssassinTargetVisibilityHandler {
    private static boolean initialized = false;

    private AssassinTargetVisibilityHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TargetVisibilityApi.registerBodyRule(
                NoellesRolesCore.id("visibility/assassin_hidden_body"),
                TargetVisibilityApi.DEFAULT_PRIORITY,
                context -> {
                    if (!HiddenBodiesWorldComponent.KEY.get(context.body().getWorld()).isHidden(context.body().getUuid())) {
                        return TargetVisibilityApi.Decision.PASS;
                    }

                    if (AssassinVisibility.canPlayerSeeHiddenBodies(context.viewer())) {
                        return TargetVisibilityApi.Decision.PASS;
                    }

                    /*
                     * 对看不见刺客尸体的玩家，四类动作都禁止：
                     * 1. RENDER：客户端不渲染尸体；
                     * 2. TARGET：准心和 RoleNameHud 的尸体射线不会选中；
                     * 3. INTERACT：失忆者、死灵法师、召集者、秃鹫、硫酸桶等服务端交互不会绕过；
                     * 4. ATTACK：保留给未来可能“攻击尸体”的物品，语义和不可交互保持一致。
                     */
                    return TargetVisibilityApi.Decision.DENY;
                }
        );
    }
}
