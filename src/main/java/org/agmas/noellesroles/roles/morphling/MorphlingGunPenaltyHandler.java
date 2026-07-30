package org.agmas.noellesroles.roles.morphling;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyContext;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyResult;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 变形试剂伪装中的左轮误伤惩罚豁免。
 */
public final class MorphlingGunPenaltyHandler {
    private static boolean initialized = false;

    private MorphlingGunPenaltyHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerInnocentRevolverPenaltyRule(
                NoellesRolesCore.id("morphling_reagent_revolver_penalty"),
                100,
                MorphlingGunPenaltyHandler::resolvePenalty
        );
    }

    private static RevolverPenaltyResult resolvePenalty(RevolverPenaltyContext context) {
        /*
         * 变形试剂让目标在一段时间内承担伪装身份的社会风险。
         * 如果服务层判断这名目标当前应取消左轮误伤惩罚，就返回 SKIP；
         * 这样开枪者不会因为“看起来/被机制处理为非无辜”的目标而掉枪或反火。
         */
        return MorphlingReagentService.shouldCancelRevolverPenalty(context.target())
                ? RevolverPenaltyResult.SKIP
                : RevolverPenaltyResult.PASS;
    }
}
