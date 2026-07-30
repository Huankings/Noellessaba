package org.agmas.noellesroles.roles.coroner;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 验尸官读取尸体时需要的死因和角色快照。
 */
public final class CoronerBodySpawnHandler {
    private static boolean initialized = false;

    private CoronerBodySpawnHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerBodySpawn(
                NoellesRolesCore.id("coroner_body_death_reason"),
                DeathApi.DEFAULT_PRIORITY,
                context -> {
                    /*
                     * 验尸官读取尸体时需要两类稳定快照：
                     * 1. deathReason：这具尸体真实死因；
                     * 2. playerRole：死者死亡瞬间的真实职业 id。
                     *
                     * 这些数据必须写在尸体实体生成时，而不是客户端查看时再查玩家当前职业；
                     * 否则转职、断线或回合后续状态变化会污染验尸结果。
                     */
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.victim().getWorld());
                    if (gameWorld.getRole(context.victim()) == null) {
                        return;
                    }
                    BodyDeathReasonComponent.KEY.get(context.body()).deathReason = context.deathReason();
                    BodyDeathReasonComponent.KEY.get(context.body()).playerRole = gameWorld.getRole(context.victim()).identifier();
                }
        );
    }
}
