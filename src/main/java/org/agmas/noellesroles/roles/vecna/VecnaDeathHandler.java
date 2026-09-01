package org.agmas.noellesroles.roles.vecna;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 维克那标记的致死反噬。
 * 这里只监听 Wathe 的确认死亡流程，因此普通扣血不会误触发颠倒伤害。
 */
public final class VecnaDeathHandler {
    private static boolean initialized;
    private VecnaDeathHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        DeathApi.registerAfterAttempt(NoellesRolesCore.id("vecna_reverse_backfire"), DeathApi.PRIORITY_POST_CONFIRMED_DEATH, context -> {
            if (!context.confirmedDeath() || context.deathReason().equals(NoellesDeathReasons.REVERSE_DEATH_REASON)
                    || !(context.killer() instanceof ServerPlayerEntity attacker)) return;
            ServerPlayerEntity victim = context.serverVictim();
            if (victim == null) return;
            GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());

            // 情形一：被标记的杀手/杀手中立被好人攻击，攻击者承担颠倒伤害并给维克那 60 金币。
            VecnaPlayerComponent victimMark = VecnaPlayerComponent.KEY.get(victim);
            if (victimMark.isMarked() && isKillerSide(game, victim)
                    && isGoodSide(game, attacker)) {
                rewardMarker(attacker, victimMark.getMarker(), VecnaConstants.KILLER_MARK_REWARD_COINS);
                GameFunctions.killPlayer(attacker, true, victim, NoellesDeathReasons.REVERSE_DEATH_REASON);
                return;
            }

            // 情形二：被标记的好人攻击杀手/杀手中立，标记者自己承担颠倒伤害并给维克那 50 金币。
            VecnaPlayerComponent attackerMark = VecnaPlayerComponent.KEY.get(attacker);
            if (attackerMark.isMarked() && isGoodSide(game, attacker)
                    && isKillerSide(game, victim)) {
                rewardMarker(attacker, attackerMark.getMarker(), VecnaConstants.CIVILIAN_MARK_REWARD_COINS);
                GameFunctions.killPlayer(attacker, true, victim, NoellesDeathReasons.REVERSE_DEATH_REASON);
            }
        });
    }

    private static boolean isKillerSide(GameWorldComponent game, ServerPlayerEntity player) {
        var role = game.getRole(player);
        return role != null && (role.getFaction() == Faction.KILLER || NoellesRoleGroups.KILLER_SIDED_NEUTRALS.contains(role));
    }

    private static boolean isGoodSide(GameWorldComponent game, ServerPlayerEntity player) {
        var role = game.getRole(player);
        return role != null && (role.getFaction() == Faction.CIVILIAN || role.getFaction() == Faction.VIGILANTE
                || NoellesRoleGroups.INDEPENDENT_NEUTRALS.contains(role));
    }

    private static void rewardMarker(ServerPlayerEntity source, java.util.UUID marker, int coins) {
        if (marker == null || source.getServer() == null) return;
        ServerPlayerEntity vecna = source.getServer().getPlayerManager().getPlayer(marker);
        if (vecna != null) PlayerShopComponent.KEY.get(vecna).addToBalance(coins);
    }

}
