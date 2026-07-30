package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.api.death.DeathDecision;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.death.DeathProcessComponent;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 时停者“时间狭缝”的死亡流程接入。
 */
public final class TimekeeperDeathHandler {
    private static boolean initialized = false;

    private TimekeeperDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerEarlyInterceptor(
                NoellesRolesCore.id("timekeeper_repeated_death_guard"),
                DeathApi.PRIORITY_REPEATED_DEATH_GUARD,
                context -> {
                    ServerPlayerEntity victim = context.serverVictim();
                    if (victim == null || !TimekeeperRiftHandler.shouldSuppressRepeatedDeathInRift(victim)) {
                        return DeathDecision.PASS;
                    }

                    /*
                     * 如果某个外层连锁死亡已经把处理标记打开，而本次重复死亡在最前面被吞掉，
                     * Wathe 不会进入 afterAttempt 清理阶段；这里主动复位，避免状态残留到后续 tick。
                     */
                    DeathProcessComponent.KEY.get(victim).setProcessing(false);
                    return DeathDecision.CANCEL;
                }
        );

        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("timekeeper_start_rift"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH,
                context -> {
                    ServerPlayerEntity victim = context.serverVictim();
                    if (victim != null && context.confirmedDeath()) {
                        /*
                         * 时间狭缝必须等 Wathe 确认死亡后再启动：
                         * 这样免死、疯魔护盾、双重人格致死转化都不会误把玩家拉进狭缝。
                         * 同时它仍在死亡尝试收尾阶段，能及时把玩家登记成特殊存活旁观。
                         */
                        TimekeeperRiftHandler.tryStartRiftAfterDeath(victim);
                    }
                }
        );
    }
}
