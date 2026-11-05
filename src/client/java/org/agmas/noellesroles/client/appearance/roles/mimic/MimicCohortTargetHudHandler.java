package org.agmas.noellesroles.client.appearance.roles.mimic;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * Mimic 单向伪装成杀手同伙的准心规则。
 *
 * <p>真正的杀手中立目标已经由 Harpy 根据公共中立分类统一处理；Mimic 的真实阵营不是中立，
 * 因而仍作为 NoellesRoles 自己的职业特例保留。这里只改变杀手看到 Mimic 时的目标表现，
 * 不赋予 Mimic 反查杀手同伙的资格。</p>
 */
public final class MimicCohortTargetHudHandler {
    private MimicCohortTargetHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerCohortTargetState(
                NoellesAppearanceSupport.id("role_name/mimic/cohort_target"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                (viewer, target, vanillaValue) -> GameWorldComponent.KEY.get(target.getWorld())
                        .isRole(target, NoellesRoleRegistry.MIMIC) ? true : null
        );
    }
}
