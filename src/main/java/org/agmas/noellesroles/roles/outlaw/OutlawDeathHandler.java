package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 平民死亡转亡命徒、亡命徒死亡收口以及亡命徒击杀统计。 */
public final class OutlawDeathHandler {
    private static boolean initialized;

    private OutlawDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        DeathApi.registerAfterAttempt(NoellesRolesCore.id("outlaw_death_flow"), DeathApi.PRIORITY_POST_CONFIRMED_DEATH, context -> {
            if (!context.confirmedDeath() || !(context.serverVictim() instanceof ServerPlayerEntity victim)) {
                return;
            }
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getWorld());
            if (gameWorld.isRole(victim, NoellesRoleRegistry.COMMONER)
                    && shouldBeginRevival(context.deathReason(), context.killer(), victim)) {
                boolean pushedOut = GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(context.deathReason())
                        && context.killer() != null
                        && context.killer() != victim;
                OutlawManager.beginCommonerRevival(
                        victim,
                        context.killer() instanceof ServerPlayerEntity killer ? killer : null,
                        pushedOut
                );
            }

            if (gameWorld.isRole(victim, NoellesRoleRegistry.OUTLAW)) {
                OutlawManager.markOutlawDeath(victim);
                if (NoellesDeathReasons.OUTLAW_TIMEOUT_DEATH_REASON.equals(context.deathReason())
                        || NoellesDeathReasons.OUTLAW_OFFLINE_DEATH_REASON.equals(context.deathReason())) {
                    dev.doctor4t.wathe.record.GameRecordManager.recordGlobalEvent(
                            victim.getServerWorld(),
                            NoellesEventIds.OUTLAW_TIME_ENDED_EVENT,
                            victim,
                            null
                    );
                }
            }

            if (context.serverKiller() instanceof ServerPlayerEntity killer
                    && killer != victim
                    && GameWorldComponent.KEY.get(killer.getWorld()).isRole(killer, NoellesRoleRegistry.OUTLAW)
                    && !NoellesDeathReasons.OUTLAW_TIMEOUT_DEATH_REASON.equals(context.deathReason())
                    && !NoellesDeathReasons.OUTLAW_OFFLINE_DEATH_REASON.equals(context.deathReason())) {
                OutlawManager.recordOutlawKill(killer);
            }
        });
    }

    private static boolean shouldBeginRevival(net.minecraft.util.Identifier deathReason, net.minecraft.entity.player.PlayerEntity killer, ServerPlayerEntity victim) {
        if (NoellesDeathReasons.MENTAL_BREAKDOWN_DEATH_REASON.equals(deathReason)) {
            return false;
        }
        if (!GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(deathReason)) {
            return true;
        }
        return killer != null && killer != victim;
    }
}
