package org.agmas.noellesroles.client.roles.commoner;

import dev.doctor4t.wathe.api.client.hud.HudOverlayApi;
import dev.doctor4t.wathe.api.client.hud.HudOverlayLayer;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.outlaw.OutlawConstants;
import org.agmas.noellesroles.roles.outlaw.OutlawPlayerComponent;

/** 平民死亡后的右下角亡命复活倒计时。 */
public final class CommonerRevivalHud {
    private CommonerRevivalHud() {
    }

    public static void register() {
        HudOverlayApi.register(NoellesRolesCore.id("hud/roles/commoner/revival"), HudOverlayLayer.MAIN_HUD, 100, context -> {
            if (GameFunctions.isPlayerAliveAndSurvival(context.player())) {
                return;
            }
            OutlawPlayerComponent state = OutlawPlayerComponent.KEY.get(context.player());
            if (!state.isRevivalPending()) {
                return;
            }
            int seconds = Math.max(1, (state.getRevivalTicksLeft() + OutlawConstants.HUD_ROUNDING_OFFSET_TICKS)
                    / OutlawConstants.HUD_TICKS_PER_SECOND);
            NoellesHudSupport.drawBottomRightLine(
                    context,
                    Text.translatable("hud.noellesroles.outlaw.revival", seconds),
                    OutlawConstants.ROLE_COLOR
            );
        });
    }
}
