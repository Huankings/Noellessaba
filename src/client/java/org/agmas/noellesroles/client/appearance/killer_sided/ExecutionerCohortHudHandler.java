package org.agmas.noellesroles.client.appearance.killer_sided;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;

/**
 * Executioner 的双向“杀手同伙”HUD 规则。
 *
 * <p>Executioner 是 NoellesRoles 里明确保留双向同伙机制的例外：
 * 杀手侧玩家看它会显示同伙，它自己看杀手/同伙目标时也拥有同伙识别资格。</p>
 */
public final class ExecutionerCohortHudHandler {
    private ExecutionerCohortHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerCohortState(
                NoellesAppearanceSupport.id("shared/role_name/executioner_two_way_cohort"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                (viewer, subject, vanillaValue) -> {
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(subject.getWorld());
                    /*
                     * 这里只有 Executioner 返回 true。
                     * Mimic、Jester、Vulture 等如果也放到双向状态里，它们本人就能反查谁是杀手。
                     */
                    return gameWorld.isRole(subject, Noellesroles.EXECUTIONER) ? true : null;
                }
        );
    }
}
