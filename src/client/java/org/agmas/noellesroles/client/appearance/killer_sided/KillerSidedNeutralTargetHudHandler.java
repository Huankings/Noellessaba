package org.agmas.noellesroles.client.appearance.killer_sided;

import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;

/**
 * NoellesRoles 杀手侧中立的单向目标显示规则。
 *
 * <p>这个 handler 只回答“target 在有资格看同伙的人眼里是否像杀手同伙”，
 * 不赋予 target 自己查看同伙提示的资格。这样 Mimic、Jester、Vulture 只会被杀手侧看见，
 * 不会反向暴露真正的杀手阵营。</p>
 */
public final class KillerSidedNeutralTargetHudHandler {
    private KillerSidedNeutralTargetHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerCohortTargetState(
                NoellesAppearanceSupport.id("shared/role_name/one_way_killer_sided_targets"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                (viewer, target, vanillaValue) -> {
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(target.getWorld());
                    /*
                     * Mimic 单独显式判断，是为了兼容它在 KILLER_SIDED_NEUTRALS 集合以外也要显示的历史规则。
                     * Executioner 已经由双向 handler 处理，这里返回 null 让最终逻辑继续使用双向结果。
                     */
                    if (gameWorld.isRole(target, NoellesRoleRegistry.MIMIC)) {
                        return true;
                    }
                    if (gameWorld.isRole(target, NoellesRoleRegistry.EXECUTIONER)) {
                        return null;
                    }
                    return gameWorld.getRole(target) != null
                            && NoellesRoleGroups.KILLER_SIDED_NEUTRALS.contains(gameWorld.getRole(target))
                            ? true
                            : null;
                }
        );
    }
}
