package org.agmas.noellesroles.client.roles.controller;

import dev.doctor4t.wathe.api.client.hud.HudOverlayApi;
import dev.doctor4t.wathe.api.client.hud.HudOverlayLayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;

/**
 * 附体师目标被控制时的黑屏 HUD。
 */
public final class ControlledStatusHud {
    private ControlledStatusHud() {
    }

    public static void register() {
        HudOverlayApi.register(NoellesHudSupport.id("roles/controller/controlled"), HudOverlayLayer.BEFORE_HUD, HudOverlayApi.DEFAULT_PRIORITY, context -> {
            ControlledPlayerComponent controlled = ControlledPlayerComponent.KEY.get(context.player());
            if (!controlled.isControlled || !context.aliveAndSurvival()) {
                return;
            }

            int width = context.width();
            int height = context.height();
            context.drawContext().fill(0, 0, width, height, 0xFF000000);
            context.drawContext().drawCenteredTextWithShadow(
                    context.textRenderer(),
                    Text.translatable("ui.controller.controlled_warning"),
                    width / 2,
                    height / 2 - 10,
                    0xFFFF0000
            );

            /*
             * 控制者信息来自服务端同步的 UUID。客户端玩家列表和实体可能晚一帧同步到，
             * 所以这里取不到时只跳过副标题，不影响黑屏本身。
             */
            if (controlled.controller != null) {
                PlayerEntity controller = context.player().getWorld().getPlayerByUuid(controlled.controller);
                if (controller != null) {
                    context.drawContext().drawCenteredTextWithShadow(
                            context.textRenderer(),
                            Text.translatable("ui.controller.controlled_by", controller.getName()),
                            width / 2,
                            height / 2 + 10,
                            0xFFFFFF00
                    );
                }
            }
        });
    }
}
