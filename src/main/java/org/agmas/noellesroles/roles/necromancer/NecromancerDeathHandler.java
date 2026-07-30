package org.agmas.noellesroles.roles.necromancer;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 死灵法师的杀手阵营死亡计数与回合清理。
 */
public final class NecromancerDeathHandler {
    private static boolean initialized = false;

    private NecromancerDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("necromancer_killer_death_tracker"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH,
                context -> {
                    if (!context.confirmedDeath()) {
                        return;
                    }
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.victim().getWorld());
                    if (gameWorld.canUseKillerFeatures(context.victim())) {
                        /*
                         * 统计的是“死亡者是否属于杀手能力阵营”，而不是谁杀了他。
                         * 自杀、误伤和好人击杀杀手都会增加可复活次数。
                         */
                        NecromancerWorldComponent.KEY.get(context.victim().getWorld()).increaseAvailableRevives();
                    }
                }
        );

        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            if (world instanceof ServerWorld serverWorld) {
                // 可复活次数是局内运行态，结算完成后必须清空，避免下一局继承上一局杀手死亡计数。
                NecromancerWorldComponent.KEY.get(serverWorld).reset();
            }
        });
    }
}
