package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.api.blackout.BlackoutApi;
import dev.doctor4t.wathe.api.blackout.BlackoutDuration;
import dev.doctor4t.wathe.api.blackout.BlackoutEffectResult;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 亡命徒复活时的 3～10 秒停电和全场失明规则。 */
public final class OutlawBlackoutHandler {
    private static boolean initialized;

    private OutlawBlackoutHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        BlackoutApi.registerDurationModifier(NoellesRolesCore.id("outlaw_revival_blackout_duration"), 500, (context, current) -> {
            OutlawWorldComponent state = OutlawWorldComponent.KEY.get(context.world());
            return state.isRevivalBlackoutPending()
                    ? BlackoutDuration.of(
                    OutlawConstants.BLACKOUT_MIN_SECONDS * 20,
                    OutlawConstants.BLACKOUT_MAX_SECONDS * 20
            ) : current;
        });
        BlackoutApi.registerEffectRule(NoellesRolesCore.id("outlaw_revival_blackout_effect"), 500, context -> {
            OutlawWorldComponent state = OutlawWorldComponent.KEY.get(context.world());
            return state.isRevivalBlackoutActive()
                    && GameFunctions.isPlayerAliveAndSurvival(context.player())
                    ? BlackoutEffectResult.blindness()
                    : BlackoutEffectResult.pass();
        });
    }

    public static void restartForRevival(ServerWorld world) {
        OutlawWorldComponent state = OutlawWorldComponent.KEY.get(world);
        state.setRevivalBlackoutPending(true);
        state.setRevivalBlackoutActive(true);
        // 需求明确要求复活时重新开始一轮停电；已有停电会先恢复再重新触发。
        dev.doctor4t.wathe.api.blackout.BlackoutApi.restorePower(world);
        dev.doctor4t.wathe.api.blackout.BlackoutApi.trigger(world);
        state.setRevivalBlackoutPending(false);
    }
}
