package org.agmas.noellesroles.client.roles.jason;

import dev.doctor4t.wathe.api.client.hud.HudOverlayApi;
import dev.doctor4t.wathe.api.client.hud.HudOverlayContext;
import dev.doctor4t.wathe.api.client.hud.HudOverlayLayer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.roles.jason.JasonConstants;
import org.agmas.noellesroles.roles.jason.JasonWoundManager;
import org.agmas.noellesroles.roles.jason.JasonWoundedPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 杰森重伤倒地玩家本人看到的濒死提示。
 *
 * <p>这里不用原版 actionbar，是因为其它职业、物品或 Wathe 本体可能同时写 actionbar，
 * 会把濒死提示挤掉。改走 HudOverlayApi 在 actionbar 附近单独绘制，既不会抢消息通道，
 * 也能在救治状态变化时稳定刷新。</p>
 */
public final class JasonWoundedSelfHud {
    private JasonWoundedSelfHud() {
    }

    public static void register() {
        HudOverlayApi.register(
                NoellesHudSupport.id("roles/jason/wounded_self"),
                HudOverlayLayer.AFTER_HUD,
                HudOverlayApi.DEFAULT_PRIORITY,
                JasonWoundedSelfHud::render
        );
    }

    private static void render(@NotNull HudOverlayContext context) {
        if (!context.aliveAndSurvival()) {
            return;
        }

        ClientPlayerEntity player = context.player();
        JasonWoundedPlayerComponent component = JasonWoundedPlayerComponent.KEY.get(player);
        if (!component.isWounded()) {
            return;
        }

        Text text;
        if (component.getRescuerUuid() != null && component.getRescueTicks() > 0) {
            PlayerEntity rescuer = resolveRescuer(player, component.getRescuerUuid());
            text = Text.translatable(
                    "hud.noellesroles.jason.self_rescuing",
                    JasonWoundManager.getRemainingRescueSeconds(component, rescuer)
            );
        } else {
            text = Text.translatable(
                    "hud.noellesroles.jason.self_wounded",
                    JasonWoundManager.getRemainingBleedSeconds(component)
            );
        }

        drawActionbarLikeText(context, text);
    }

    private static @Nullable PlayerEntity resolveRescuer(@NotNull ClientPlayerEntity player, @Nullable UUID rescuerUuid) {
        if (rescuerUuid == null) {
            return null;
        }
        if (rescuerUuid.equals(player.getUuid())) {
            return player;
        }
        return player.getWorld().getPlayerByUuid(rescuerUuid);
    }

    private static void drawActionbarLikeText(@NotNull HudOverlayContext context, @NotNull Text text) {
        TextRenderer renderer = context.textRenderer();
        DrawContext drawContext = context.drawContext();
        int textWidth = renderer.getWidth(text);
        int availableWidth = Math.max(1, context.width() - JasonConstants.WOUNDED_SELF_HUD_HORIZONTAL_PADDING * 2);
        float scale = Math.min(JasonConstants.WOUNDED_SELF_HUD_SCALE, availableWidth / (float) Math.max(1, textWidth));

        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(
                context.width() / 2.0F,
                context.height() - JasonConstants.WOUNDED_SELF_HUD_Y_FROM_BOTTOM,
                0.0F
        );
        drawContext.getMatrices().scale(scale, scale, 1.0F);
        drawContext.drawTextWithShadow(
                renderer,
                text,
                -textWidth / 2,
                0,
                JasonConstants.ROLE_COLOR
        );
        drawContext.getMatrices().pop();
    }
}
