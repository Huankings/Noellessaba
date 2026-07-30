package org.agmas.noellesroles.roles.morphling;

import dev.doctor4t.wathe.api.death.DeathApi;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 变形试剂相关的死亡后置结算。
 */
public final class MorphlingDeathHandler {
    private static boolean initialized = false;

    private MorphlingDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("morphling_reagent_after_kill"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH,
                context -> {
                    ServerPlayerEntity victim = context.serverVictim();
                    if (victim != null) {
                        /*
                         * 变形试剂的死亡后置逻辑需要知道“这次死亡请求最终如何收场”。
                         * afterKill 内部会继续判断具体试剂状态；这里不提前用 confirmedDeath() 短路，
                         * 是为了保留旧 MorphlingReagentDeathMixin 在死亡流程返回后统一收尾的语义。
                         */
                        MorphlingReagentService.afterKill(victim, context.serverKiller(), context.deathReason());
                    }
                }
        );
    }
}
