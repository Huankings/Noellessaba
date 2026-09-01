package org.agmas.noellesroles.roles.vecna;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyContext;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyResult;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.registry.NoellesRoleGroups;

/** 维克那标记好人使用普通左轮误伤好人时的免罚与奖励。 */
public final class VecnaGunHandler {
    private static boolean initialized;
    private VecnaGunHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        GunShotApi.registerInnocentRevolverPenaltyRule(
                NoellesRolesCore.id("vecna_marked_good_revolver_penalty"),
                250,
                VecnaGunHandler::resolvePenalty
        );
    }

    private static RevolverPenaltyResult resolvePenalty(RevolverPenaltyContext context) {
        if (!context.stack().isOf(WatheItems.REVOLVER)
                || !GameFunctions.isPlayerAliveAndSurvival(context.shooter())
                || !GameFunctions.isPlayerAliveAndSurvival(context.target())) {
            return RevolverPenaltyResult.PASS;
        }
        VecnaPlayerComponent mark = VecnaPlayerComponent.KEY.get(context.shooter());
        if (!mark.isMarked()) return RevolverPenaltyResult.PASS;
        var shooterRole = context.game().getRole(context.shooter());
        var targetRole = context.game().getRole(context.target());
        boolean shooterGood = shooterRole != null && (shooterRole.getFaction() == Faction.CIVILIAN
                || shooterRole.getFaction() == Faction.VIGILANTE
                || NoellesRoleGroups.INDEPENDENT_NEUTRALS.contains(shooterRole));
        boolean targetGood = targetRole != null && (targetRole.getFaction() == Faction.CIVILIAN
                || targetRole.getFaction() == Faction.VIGILANTE
                || NoellesRoleGroups.INDEPENDENT_NEUTRALS.contains(targetRole));
        if (!shooterGood || !targetGood) return RevolverPenaltyResult.PASS;
        var owner = context.shooter().getServer() == null ? null
                : context.shooter().getServer().getPlayerManager().getPlayer(mark.getMarker());
        if (owner != null) PlayerShopComponent.KEY.get(owner).addToBalance(VecnaConstants.REVOLVER_MARK_REWARD_COINS);
        return RevolverPenaltyResult.SKIP;
    }
}
