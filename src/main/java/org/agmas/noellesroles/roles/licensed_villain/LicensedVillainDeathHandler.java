package org.agmas.noellesroles.roles.licensed_villain;

import dev.doctor4t.wathe.api.death.DeathApi;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 执照恶棍确认死亡后结束其时刻并记录专属回放。 */
public final class LicensedVillainDeathHandler {
    private static boolean initialized;

    private LicensedVillainDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("licensed_villain_moment_death"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH,
                context -> {
                    if (!context.confirmedDeath() || !(context.serverVictim() instanceof ServerPlayerEntity victim)) {
                        return;
                    }
                    if (dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(victim.getWorld())
                            .isRole(victim, NoellesRoleRegistry.LICENSED_VILLAIN)) {
                        /* 暂停期间死亡也会结束此前已经触发的同一次时刻，但不会补记新的进入事件。 */
                        LicensedVillainMomentManager.finishMoment(victim, true);
                    }
                }
        );
    }
}
