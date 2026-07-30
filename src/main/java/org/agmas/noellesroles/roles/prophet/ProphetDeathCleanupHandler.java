package org.agmas.noellesroles.roles.prophet;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 先知死亡后只清理自己的当前标记。
 */
public final class ProphetDeathCleanupHandler {
    private static boolean initialized = false;

    private ProphetDeathCleanupHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerBeforeMoodReset(
                NoellesRolesCore.id("prophet_death_cleanup"),
                DeathApi.DEFAULT_PRIORITY,
                context -> {
                    /*
                     * 先知死亡后只清除“自己当前正在揭露/标记的目标”。
                     * 放在 beforeMoodReset 是为了确认死亡成立后再清理，同时保留组件可读状态。
                     */
                    if (context.victim().getWorld() == null) {
                        return;
                    }
                    if (GameWorldComponent.KEY.get(context.victim().getWorld()).isRole(context.victim(), NoellesRoleRegistry.PROPHET)) {
                        ProphetPlayerComponent.KEY.get(context.victim()).clearMarkOnly();
                    }
                }
        );
    }
}
