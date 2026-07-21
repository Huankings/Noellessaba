package org.agmas.noellesroles.client.appearance.roles.muzzler;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.muzzler.MuzzlerConstants;
import org.agmas.noellesroles.roles.muzzler.SilencePlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 静语者的“目标被封嘴”准心提示。
 */
public final class MuzzlerSilencedTipHudHandler {
    private MuzzlerSilencedTipHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesAppearanceSupport.id("role_name/muzzler_silenced_tip"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    PlayerEntity target = context.targetPlayer();
                    if (target == null) {
                        return;
                    }

                    SilencePlayerComponent victimSilence = SilencePlayerComponent.KEY.get(target);
                    if (!victimSilence.isSilenced()
                            || victimSilence.getSilencedTicks() < MuzzlerConstants.DISPLAY_SILENCED_TIP_DELAY_TICKS) {
                        return;
                    }

                    renderSilencedTip(context.renderer(), context.drawContext());
                }
        );
    }

    private static void renderSilencedTip(@NotNull TextRenderer renderer, @NotNull DrawContext context) {
        Text text = Text.translatable("tip.noellesroles.muzzler.silenced");

        /*
         * 沿用 StarryExpress 原 UI：准心上方 37.5px 起点、0.6 倍缩放、静语者职业色。
         * 这类准心提示由 Wathe RoleNameHudApi 统一在正确时机绘制，不再自己 mixin 主 HUD。
         */
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2.0F, context.getScaledWindowHeight() / 2.0F - 37.5F, 0.0F);
        context.getMatrices().scale(0.6F, 0.6F, 1.0F);
        context.drawTextWithShadow(renderer, text, -renderer.getWidth(text) / 2, 32, Noellesroles.MUZZLER.color());
        context.getMatrices().pop();
    }
}
