package org.agmas.noellesroles.roles.party_animal;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 派对狂击杀变声目标奖励与死亡后的普通变声清理。 */
public final class PartyAnimalDeathHandler {
    private PartyAnimalDeathHandler() {
    }

    public static void init() {
        DeathApi.registerAfterAttempt(NoellesRolesCore.id("party_animal/death"), DeathApi.PRIORITY_POST_CONFIRMED_DEATH, context -> {
            if (!context.confirmedDeath()) return;
            int victimLevel = Math.abs(HeliumBuzzPlayerComponent.KEY.get(context.victim()).captureDeathLevelAndClearNormal());
            if (!(context.serverKiller() instanceof net.minecraft.server.network.ServerPlayerEntity killer)
                    || killer == context.serverVictim()) return;
            GameWorldComponent game = GameWorldComponent.KEY.get(killer.getWorld());
            if (!game.isRole(killer, NoellesRoleRegistry.PARTY_ANIMAL)) return;
            int level = victimLevel;
            int reward = switch (level) {
                case 1 -> PartyAnimalConstants.LEVEL_1_REWARD_COINS;
                case 2 -> PartyAnimalConstants.LEVEL_2_REWARD_COINS;
                case 3 -> PartyAnimalConstants.LEVEL_3_REWARD_COINS;
                default -> 0;
            };
            if (reward > 0) PlayerShopComponent.KEY.get(killer).addToBalance(reward);
        });
    }
}
