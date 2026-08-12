package org.agmas.noellesroles.roles.insane_damned_paranoid_killer;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 亡语杀手尸体伪装的准心目标隐藏。
 *
 * <p>玩家实体仍然要被渲染出来，然后由客户端姿态 mixin 把它躺成尸体；
 * 但它不能被准心当作“活着的玩家目标”，否则 Wathe 默认准心、武器准心或准心名字都会泄露伪装。
 * 注意：这里不能再屏蔽攻击和物品交互，否则伪装者会获得不合理的无敌窗口。</p>
 */
public final class InsaneDamnedKillerTargetVisibilityHandler {
    private static boolean initialized = false;

    private InsaneDamnedKillerTargetVisibilityHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TargetVisibilityApi.registerPlayerRule(
                NoellesRolesCore.id("visibility/insane_damned_killer_corpse"),
                InsaneDamnedKillerConstants.CORPSE_TARGET_VISIBILITY_PRIORITY,
                context -> {
                    if (!InsaneDamnedKillerPlayerComponent.isActiveCorpseMode(context.target())) {
                        return TargetVisibilityApi.Decision.PASS;
                    }

                    if (context.action() == TargetVisibilityApi.Action.RENDER) {
                        /*
                         * RENDER 必须放行：这个状态不是隐身，而是让活人以尸体姿态显示。
                         * 真正躺倒由客户端极窄 mixin 处理；这里仅负责“不要把它识别成活人目标”。
                         */
                        return TargetVisibilityApi.Decision.PASS;
                    }

                    if (context.action() == TargetVisibilityApi.Action.TARGET) {
                        /*
                         * TARGET 只负责“准心射线能不能把它识别成玩家目标”：
                         * - Wathe 默认准心不会变成命中态；
                         * - RoleNameHud 不会显示玩家名 / 同伙提示；
                         * - 职业准心图标仍会像对准普通尸体一样保持空目标。
                         *
                         * 这条规则故意不拒绝 ATTACK / INTERACT。武器、控制道具和服务端 C2S 兜底
                         * 应该继续通过 canAttackPlayer / canInteractWithPlayer 命中伪装者，
                         * 否则亡语杀手可以躺尸期间白嫖无敌并反杀。
                         */
                        return TargetVisibilityApi.Decision.DENY;
                    }

                    return TargetVisibilityApi.Decision.PASS;
                }
        );
    }
}
