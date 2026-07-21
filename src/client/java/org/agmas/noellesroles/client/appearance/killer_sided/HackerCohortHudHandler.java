package org.agmas.noellesroles.client.appearance.killer_sided;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;

/**
 * Hacker 的双向杀手同伙 HUD 资格。
 *
 * <p>Hacker 是协助杀手的中立职业，和 Dreamer/Jester/Vulture 的“只被杀手看见”不同；
 * 它自己也需要看到真杀手同伙，才能配合手机语音与破解信息。</p>
 */
public final class HackerCohortHudHandler {
    private HackerCohortHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerCohortState(
                NoellesAppearanceSupport.id("shared/role_name/hacker_two_way_cohort"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                (viewer, subject, vanillaValue) -> {
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(subject.getWorld());
                    return gameWorld.isRole(subject, Noellesroles.HACKER) ? true : null;
                }
        );
    }
}
