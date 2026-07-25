package org.agmas.noellesroles.client.roles.kidnapper;

import dev.doctor4t.wathe.api.client.hud.HudOverlayApi;
import dev.doctor4t.wathe.api.client.hud.HudOverlayLayer;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;
import org.agmas.noellesroles.roles.kidnapper.KidnapperConstants;

/**
 * 绑匪控制目标时的黑屏 HUD。
 */
public final class KidnapperControlledHud {
    private KidnapperControlledHud() {
    }

    public static void register() {
        HudOverlayApi.register(NoellesHudSupport.id("roles/kidnapper/controlled"), HudOverlayLayer.BEFORE_HUD, HudOverlayApi.DEFAULT_PRIORITY, context -> {
            KidnapperComponent controlled = KidnapperComponent.KEY.get(context.player());
            if (controlled.controlTicks <= 0 || !context.aliveAndSurvival()) {
                return;
            }

            int width = context.width();
            int height = context.height();
            context.drawContext().fill(0, 0, width, height, 0xFF000000);
            context.drawContext().drawCenteredTextWithShadow(
                    context.textRenderer(),
                    Text.translatable("tip.noellesroles.kidnapper.warning"),
                    width / 2,
                    height / 2 - 10,
                    KidnapperConstants.ROLE_COLOR
            );
            context.drawContext().drawCenteredTextWithShadow(
                    context.textRenderer(),
                    Text.translatable("tip.noellesroles.kidnapper.timeleft", controlled.controlTicks / 20),
                    width / 2,
                    height / 2 + 10,
                    KidnapperConstants.ROLE_COLOR
            );
        });
    }
}
