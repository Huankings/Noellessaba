package org.agmas.noellesroles.roles.licensed_villain;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyContext;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyResult;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 执照恶棍使用 Wathe 左轮时不触发“误伤好人”惩罚。
 */
public final class LicensedVillainGunPenaltyHandler {
    private static boolean initialized = false;

    private LicensedVillainGunPenaltyHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerInnocentRevolverPenaltyRule(
                NoellesRolesCore.id("licensed_villain_revolver_penalty"),
                100,
                LicensedVillainGunPenaltyHandler::resolvePenalty
        );
    }

    private static RevolverPenaltyResult resolvePenalty(RevolverPenaltyContext context) {
        if (context.game().isRole(context.shooter(), NoellesRoleRegistry.LICENSED_VILLAIN)
                && GameFunctions.isPlayerAliveAndSurvival(context.shooter())) {
            /*
             * 执照恶棍的核心能力是“合法作恶”：
             * 使用 Wathe 普通左轮误伤好人时，仍会正常尝试击杀目标，
             * 但不再执行 Wathe 默认的反火、掉枪和清空心情惩罚。
             */
            return RevolverPenaltyResult.SKIP;
        }
        return RevolverPenaltyResult.PASS;
    }
}
