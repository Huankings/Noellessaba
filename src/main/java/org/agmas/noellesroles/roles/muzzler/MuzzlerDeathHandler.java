package org.agmas.noellesroles.roles.muzzler;

import dev.doctor4t.wathe.api.death.DeathApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 玩家确认死亡后的胶带状态清理，防止复活或时间狭缝继续继承窒息标签。 */
public final class MuzzlerDeathHandler {
    private static boolean initialized;

    private MuzzlerDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        DeathApi.registerAfterMarkedDead(
                NoellesRolesCore.id("muzzler_tape_death_cleanup"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH + 30,
                context -> {
                    if (context.serverVictim() != null) {
                        /*
                         * afterMarkedDead 已确认 Wathe 清除了存活授权并切到旁观，
                         * 此时清理不会影响 killPlayer 入口提前保存的回放 extraDeathData。
                         */
                        SilencePlayerComponent.KEY.get(context.serverVictim()).reset();
                    }
                }
        );
    }
}
