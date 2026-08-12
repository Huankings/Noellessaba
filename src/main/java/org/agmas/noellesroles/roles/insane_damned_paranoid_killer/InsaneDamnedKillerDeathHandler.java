package org.agmas.noellesroles.roles.insane_damned_paranoid_killer;

import dev.doctor4t.wathe.api.death.DeathApi;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 亡语杀手死亡后的尸体伪装清理。
 */
public final class InsaneDamnedKillerDeathHandler {
    private static boolean initialized = false;

    private InsaneDamnedKillerDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("insane_damned_killer_corpse_cleanup"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH,
                context -> {
                    /*
                     * 只在“死亡已经确认”之后清理：
                     * 护盾、免死、时间狭缝拦截等取消死亡的场景不应该把玩家仍在使用的伪装关掉。
                     * 这里不写 GameRecordManager，满足需求里的“不记录回放事件，避免回放刷屏”。
                     */
                    if (!context.confirmedDeath() || !(context.victim() instanceof ServerPlayerEntity victim)) {
                        return;
                    }

                    InsaneDamnedKillerPlayerComponent.KEY.get(victim).reset();
                }
        );
    }
}
