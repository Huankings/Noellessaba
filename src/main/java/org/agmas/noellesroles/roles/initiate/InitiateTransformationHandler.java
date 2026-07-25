package org.agmas.noellesroles.roles.initiate;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.AllowPlayerDeath;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 初学者死亡与考核转职逻辑。
 */
public final class InitiateTransformationHandler {
    private static boolean initialized = false;

    private InitiateTransformationHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        AllowPlayerDeath.EVENT.register(InitiateTransformationHandler::handleAllowDeath);
    }

    private static boolean handleAllowDeath(PlayerEntity victim, @Nullable PlayerEntity killer, Identifier deathReason) {
        if (!(victim instanceof ServerPlayerEntity serverVictim) || !(serverVictim.getWorld() instanceof ServerWorld world)) {
            return true;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        boolean victimIsInitiate = gameWorld.isRole(serverVictim, NoellesRoleRegistry.INITIATE);
        boolean killerIsInitiate = killer instanceof ServerPlayerEntity serverKiller
                && gameWorld.isRole(serverKiller, NoellesRoleRegistry.INITIATE);

        if (killerIsInitiate && !victimIsInitiate) {
            /*
             * 初学者杀错人时，目标不应死亡；考核失败的初学者自己死亡。
             * 这次自亡会再次进入本事件，届时 failed_initiation 分支会把其它存活初学者转为随机杀手。
             */
            GameFunctions.killPlayer(killer, true, null, NoellesDeathReasons.FAILED_INITIATION_DEATH_REASON);
            return false;
        }

        if (!victimIsInitiate) {
            return true;
        }

        if (killerIsInitiate) {
            /*
             * 正确完成考核：击杀另一名初学者的玩家晋升为随机杀手。
             * 被杀的初学者继续按本次死亡正常处理。
             */
            InitiateRoleSelector.transformInitiate((ServerPlayerEntity) killer, gameWorld, InitiateRoleSelector.selectRandomKillerRole());
            return true;
        }

        /*
         * 其它初学者的死亡都需要影响“仍然活着的其它初学者”。
         * AllowPlayerDeath 发生在实际死亡前，所以这里必须显式排除即将死亡的 victim。
         */
        transformOtherLivingInitiates(world, gameWorld, serverVictim.getUuid(), selectFallbackRoleForDeath(gameWorld, killer, deathReason));
        return true;
    }

    private static @NotNull Role selectFallbackRoleForDeath(
            @NotNull GameWorldComponent gameWorld,
            @Nullable PlayerEntity killer,
            @NotNull Identifier deathReason
    ) {
        /*
         * failed_initiation 本质上是“杀错人导致自己死亡”，按用户要求其它初学者转为随机杀手。
         * 没有 killer 的掉出列车、精神崩溃等自杀/环境死因，也同样按随机杀手处理。
         */
        if (NoellesDeathReasons.FAILED_INITIATION_DEATH_REASON.equals(deathReason) || killer == null) {
            return InitiateRoleSelector.selectRandomKillerRole();
        }

        Role killerRole = gameWorld.getRole(killer);
        Faction faction = killerRole == null ? null : killerRole.getFaction();
        if (faction == Faction.CIVILIAN || faction == Faction.VIGILANTE) {
            return InitiateRoleSelector.selectRandomKillerRole();
        }
        if (faction == Faction.KILLER) {
            return InitiateRoleSelector.selectRandomGoodOrVigilanteRole();
        }
        if (faction == Faction.NEUTRAL) {
            return InitiateRoleSelector.selectRandomNeutralRole();
        }

        return InitiateRoleSelector.selectRandomKillerRole();
    }

    private static void transformOtherLivingInitiates(
            @NotNull ServerWorld world,
            @NotNull GameWorldComponent gameWorld,
            @NotNull UUID excludedVictim,
            @NotNull Role fallbackRole
    ) {
        for (ServerPlayerEntity player : world.getPlayers(candidate ->
                GameFunctions.isPlayerAliveAndSurvival(candidate)
                        && !candidate.getUuid().equals(excludedVictim)
                        && gameWorld.isRole(candidate, NoellesRoleRegistry.INITIATE))) {
            InitiateRoleSelector.transformInitiate(player, gameWorld, fallbackRole);
        }
    }
}
