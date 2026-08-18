package org.agmas.noellesroles.roles.shadow_jester;

import dev.doctor4t.wathe.api.combat.GunCooldownContext;
import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyContext;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyResult;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 影子小丑左轮规则。
 *
 * <p>第三阶段后影子小丑是独立胜利阵营，对好人开枪不应触发 Wathe 的“误伤好人”反火。
 * 第四阶段进一步把普通左轮冷却固定到 4 秒，用于谢幕收割节奏。</p>
 */
public final class ShadowJesterGunHandler {
    private static boolean initialized = false;

    private ShadowJesterGunHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        GunShotApi.registerInnocentRevolverPenaltyRule(
                NoellesRolesCore.id("shadow_jester_revolver_penalty"),
                100,
                ShadowJesterGunHandler::resolvePenalty
        );
        GunShotApi.registerCooldownModifier(
                NoellesRolesCore.id("shadow_jester_revolver_cooldown"),
                GunShotApi.DEFAULT_PRIORITY + 100,
                ShadowJesterGunHandler::modifyCooldown
        );
    }

    private static RevolverPenaltyResult resolvePenalty(RevolverPenaltyContext context) {
        if (!context.stack().isOf(WatheItems.REVOLVER)
                || !context.game().isRole(context.shooter(), NoellesRoleRegistry.SHADOW_JESTER)
                || !GameFunctions.isPlayerAliveAndSurvival(context.shooter())) {
            return RevolverPenaltyResult.PASS;
        }

        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(context.shooter().getWorld());
        if (component.getPhase(context.shooter().getUuid()).atLeast(ShadowJesterPhase.VOW_BOUND)
                && context.game().isInnocent(context.target())) {
            return RevolverPenaltyResult.SKIP;
        }
        return RevolverPenaltyResult.PASS;
    }

    private static int modifyCooldown(GunCooldownContext context, int currentCooldown) {
        if (!context.stack().isOf(WatheItems.REVOLVER)) {
            return currentCooldown;
        }
        if (!dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(context.shooter().getWorld()).isRole(context.shooter(), NoellesRoleRegistry.SHADOW_JESTER)) {
            return currentCooldown;
        }
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(context.shooter().getWorld());
        return component.getPhase(context.shooter().getUuid()) == ShadowJesterPhase.CURTAIN_CALL
                ? ShadowJesterConstants.PHASE_FOUR_REVOLVER_COOLDOWN_TICKS
                : currentCooldown;
    }
}
