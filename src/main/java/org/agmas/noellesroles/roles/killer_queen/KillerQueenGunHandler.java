package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyResult;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 好人用左轮击杀被招募者时跳过 Wathe 的无辜目标反火惩罚。 */
public final class KillerQueenGunHandler {
    private KillerQueenGunHandler() {
    }

    public static void init() {
        GunShotApi.registerInnocentRevolverPenaltyRule(NoellesRolesCore.id("killer_queen/recruited_revolver_penalty"), KillerQueenConstants.RECRUIT_REVOLVER_PENALTY_PRIORITY,
                context -> {
                    var shooterRole = context.game().getRole(context.shooter());
                    boolean goodShooter = shooterRole != null
                            && (shooterRole.getFaction() == Faction.CIVILIAN || shooterRole.getFaction() == Faction.VIGILANTE);
                    return goodShooter && KillerQueenWorldComponent.KEY.get(context.target().getWorld()).isRecruited(context.target().getUuid())
                            ? RevolverPenaltyResult.SKIP : RevolverPenaltyResult.PASS;
                });
    }
}
