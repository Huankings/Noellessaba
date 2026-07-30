package org.agmas.noellesroles.roles.stalker;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 潜伏者二阶段后的匕首击杀计数。
 */
public final class StalkerDeathHandler {
    private static boolean initialized = false;

    private StalkerDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerBeforeAttempt(
                NoellesRolesCore.id("stalker_knife_kill_count"),
                DeathApi.DEFAULT_PRIORITY,
                context -> {
                    /*
                     * 保留旧 StalkerKillCountMixin 的时机：刀击死亡请求进入时就增加成长计数。
                     * 潜伏者阶段成长本身与“死亡后来是否被尸体生成回调处理”无关，
                     * 所以这里仍放在 beforeAttempt，而不是 afterAttempt。
                     */
                    if (context.killer() == null || !GameConstants.DeathReasons.KNIFE.equals(context.deathReason())) {
                        return;
                    }
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.killer().getWorld());
                    if (!gameWorld.isRole(context.killer(), NoellesRoleRegistry.STALKER)) {
                        return;
                    }

                    StalkerPlayerComponent component = StalkerPlayerComponent.KEY.get(context.killer());
                    if (component.isActiveStalker() && component.phase >= 2) {
                        component.addKill();
                    }
                }
        );
    }
}
