package org.agmas.noellesroles.client.roles.outlaw;

import dev.doctor4t.wathe.api.time.TimeHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.outlaw.OutlawPlayerComponent;

/** 亡命时刻顶部时间 HUD。 */
public final class OutlawTimeHud {
    private OutlawTimeHud() {
    }

    public static void register() {
        TimeHudApi.registerProvider(NoellesRolesCore.id("outlaw/time"), 250, player -> {
            if (!GameFunctions.isPlayerAliveAndSurvival(player)
                    || !dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.OUTLAW)) {
                return TimeHudApi.TimeDisplay.pass();
            }
            int ticks = OutlawPlayerComponent.KEY.get(player).getOutlawTicksLeft();
            return ticks > 0
                    ? TimeHudApi.TimeDisplay.showFixedColor(ticks, NoellesRoleRegistry.OUTLAW.color())
                    : TimeHudApi.TimeDisplay.pass();
        });
    }
}
