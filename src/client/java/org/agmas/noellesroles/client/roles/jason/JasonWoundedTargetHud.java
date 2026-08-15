package org.agmas.noellesroles.client.roles.jason;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.jason.JasonConstants;
import org.agmas.noellesroles.roles.jason.JasonWoundManager;
import org.agmas.noellesroles.roles.jason.JasonWoundedPlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 准心指向杰森重伤倒地玩家时的救治提示。
 *
 * <p>这个 HUD 使用 Wathe 的 RoleNameHudApi，因此它会和名字、同伙提示共享同一条准心射线。
 * 文字放在基础名字和可能的同伙标记之后，避免覆盖其它职业的名字渲染。</p>
 */
public final class JasonWoundedTargetHud {
    private JasonWoundedTargetHud() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesRolesCore.id("role_name/jason/wounded_rescue"),
                RoleNameHudApi.DEFAULT_PRIORITY - 10,
                context -> {
                    ClientPlayerEntity viewer = context.player();
                    PlayerEntity target = context.targetPlayer();
                    if (!GameFunctions.isPlayerAliveAndSurvival(viewer)
                            || target == null
                            || !GameFunctions.isPlayerAliveAndSurvival(target)) {
                        return;
                    }

                    JasonWoundedPlayerComponent wounded = JasonWoundedPlayerComponent.KEY.get(target);
                    if (!wounded.isWounded()) {
                        return;
                    }

                    Text text;
                    if (JasonWoundManager.isRescuing(wounded, viewer)) {
                        text = Text.translatable(
                                "hud.noellesroles.jason.rescuing",
                                JasonWoundManager.getRemainingRescueSeconds(wounded, viewer)
                        );
                    } else if (JasonWoundManager.isBeingRescuedByOther(wounded, viewer)) {
                        /*
                         * 同一名倒地玩家只允许一名存活玩家救治。
                         * 其他人靠近准心指向时提示“正在被其他人救治”，避免他们以为自己蹲下也能叠加进度。
                         */
                        text = Text.translatable("hud.noellesroles.jason.rescued_by_other");
                    } else {
                        text = Text.translatable(
                                "hud.noellesroles.jason.wounded",
                                JasonWoundManager.getRemainingBleedSeconds(wounded)
                        );
                    }
                    drawCentered(context.renderer(), context.drawContext(), text);
                }
        );
    }

    private static void drawCentered(@NotNull TextRenderer renderer, @NotNull DrawContext context, @NotNull Text text) {
        context.getMatrices().push();
        context.getMatrices().translate(
                context.getScaledWindowWidth() / 2.0F,
                context.getScaledWindowHeight() / 2.0F + 6.0F,
                0.0F
        );
        context.getMatrices().scale(JasonConstants.ROLE_NAME_HUD_SCALE, JasonConstants.ROLE_NAME_HUD_SCALE, 1.0F);
        context.drawTextWithShadow(
                renderer,
                text,
                -renderer.getWidth(text) / 2,
                JasonConstants.ROLE_NAME_HUD_Y_OFFSET,
                JasonConstants.ROLE_COLOR
        );
        context.getMatrices().pop();
    }
}
