package org.agmas.noellesroles.client.appearance.roles.killer_queen;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenRules;

/** 杀手皇后的准心杀手同伙识别，复用 Wathe 原生同伙文字和布局。 */
public final class KillerQueenCohortHudHandler {
    private KillerQueenCohortHudHandler() {
    }

    public static void register() {
        /*
         * 与 Hacker 一样赋予自身双向同伙资格。公共 KillerSidedNeutralTargetHudHandler
         * 已负责杀手中立目标的显示，这里无需重复绘制准心文字，也不改变真实职业阵营。
         */
        RoleNameHudApi.registerCohortState(
                NoellesAppearanceSupport.id("role_name/killer_queen/cohort_state"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                (viewer, subject, vanillaValue) -> GameWorldComponent.KEY.get(subject.getWorld())
                        .isRole(subject, NoellesRoleRegistry.KILLER_QUEEN) ? true : null
        );
        /*
         * 公共同伙目标规则还会把 Mimic 视为同伙，但杀手皇后只识别真正的杀手/杀手中立。
         * 对其它阵营返回 HIDE；合法目标返回 PASS，保留召集伪装、时间狭缝等更高优先级隐藏规则。
         */
        RoleNameHudApi.registerCohortHint(
                NoellesAppearanceSupport.id("role_name/killer_queen/cohort_hint"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                (viewer, target, vanillaValue) -> {
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
                    if (!gameWorld.isRole(viewer, NoellesRoleRegistry.KILLER_QUEEN)) {
                        return RoleNameHudApi.VisibilityResult.PASS;
                    }
                    return KillerQueenRules.isKillerOrKillerSidedNeutral(gameWorld.getRole(target))
                            ? RoleNameHudApi.VisibilityResult.PASS : RoleNameHudApi.VisibilityResult.HIDE;
                }
        );
    }
}
