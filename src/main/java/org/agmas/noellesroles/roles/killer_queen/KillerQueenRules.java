package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.Nullable;

/** 杀手皇后各系统共享的显式阵营与存活判定。 */
public final class KillerQueenRules {
    private KillerQueenRules() {
    }

    public static boolean isLivingGoodOrIndependent(PlayerEntity player) {
        if (!GameFunctions.isPlayerAliveAndSurvival(player)
                || org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent.KEY.get(player).isInTimeRift()) {
            return false;
        }
        return isGoodOrIndependent(GameWorldComponent.KEY.get(player.getWorld()).getRole(player));
    }

    public static boolean isGoodOrIndependent(@Nullable Role role) {
        if (role == null) return false;
        return role.getFaction() == Faction.CIVILIAN
                || role.getFaction() == Faction.VIGILANTE
                || NoellesRoleGroups.INDEPENDENT_NEUTRALS.contains(role);
    }

    public static boolean isKillerOrKillerSidedNeutral(@Nullable Role role) {
        return role != null && (role.getFaction() == Faction.KILLER
                || NoellesRoleGroups.KILLER_SIDED_NEUTRALS.contains(role));
    }

    public static boolean isKillerQueen(PlayerEntity player) {
        return GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.KILLER_QUEEN);
    }
}
