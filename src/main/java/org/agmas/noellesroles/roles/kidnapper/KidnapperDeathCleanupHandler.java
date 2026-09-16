package org.agmas.noellesroles.roles.kidnapper;

import dev.doctor4t.wathe.api.death.DeathApi;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 绑匪死亡链的控制状态清理。
 *
 * <p>时间狭缝会让死者暂时仍被 Wathe 视为存活；如果只等待普通 tick，
 * 控制者死亡后被控制目标可能继续携带劫持状态。因此在 Wathe 已确认切换为死亡旁观后，
 * 立即清理目标自身状态以及所有指向该控制者的控制关系。</p>
 */
public final class KidnapperDeathCleanupHandler {
    private static boolean initialized;

    private KidnapperDeathCleanupHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        DeathApi.registerAfterMarkedDead(
                NoellesRolesCore.id("kidnapper_control_cleanup"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH + 20,
                context -> {
                    if (!(context.serverVictim() instanceof ServerPlayerEntity victim)
                            || !(victim.getServerWorld() instanceof ServerWorld world)) {
                        return;
                    }
                    KidnapperComponent.KEY.get(victim).reset();
                    for (ServerPlayerEntity player : world.getPlayers()) {
                        KidnapperComponent state = KidnapperComponent.KEY.get(player);
                        if (victim.getUuid().equals(state.controllerUUID)) {
                            state.reset();
                        }
                    }
                }
        );
    }
}
