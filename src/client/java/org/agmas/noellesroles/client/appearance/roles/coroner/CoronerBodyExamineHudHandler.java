package org.agmas.noellesroles.client.appearance.roles.coroner;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.appearance.modifiers.graverobber.GraverobberBodyInfoAccess;
import org.agmas.noellesroles.roles.coroner.CoronerConstants;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 验尸官的“靠近检查尸体”提示。
 *
 * <p>这个提示原来和验尸信息写在同一个 {@code RoleNameRenderer.renderHud} 注入点，
 * 只要前面那段矩阵没收好，后面的提示就会被顶到莫名其妙的位置。
 * 现在拆成独立的 extra HUD，位置直接用常量控制，也更方便后续再调高一点。</p>
 */
public final class CoronerBodyExamineHudHandler {
    private CoronerBodyExamineHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesAppearanceSupport.id("role_name/coroner_body_examine"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    ClientPlayerEntity player = context.player();
                    if (!GraverobberBodyInfoAccess.canSeeExaminePrompt(player)) {
                        return;
                    }

                    if (GraverobberBodyInfoAccess.shouldBlockCoronerReadoutForSanity(player)) {
                        return;
                    }

                    PlayerBodyEntity body = RoleNameHudApi.findLookedAtBody(player, CoronerConstants.BODY_EXAMINE_RANGE);
                    if (body == null) {
                        return;
                    }

                    CoronerPlayerComponent coronerComp = CoronerPlayerComponent.KEY.get(player);
                    boolean alreadyExamined = coronerComp.examinedBodies.contains(body.getUuid());

                    drawCentered(
                            context.renderer(),
                            context.drawContext(),
                            alreadyExamined
                                    ? Text.literal("§7已检查过此尸体")
                                    : Text.literal("§e靠近检查尸体 §7(获得金币)"),
                            CoronerConstants.BODY_EXAMINE_PROMPT_Y,
                            alreadyExamined ? Colors.GRAY : Colors.YELLOW
                    );

                    if (!alreadyExamined) {
                        Text stats = Text.literal(String.format("§7已检查: %d具尸体 | 总计: %d金币",
                                coronerComp.totalBodiesExamined,
                                coronerComp.totalGoldEarned));
                        drawCentered(context.renderer(), context.drawContext(), stats, CoronerConstants.BODY_EXAMINE_STATS_Y, Colors.LIGHT_GRAY);
                    }
                }
        );
    }

    private static void drawCentered(@NotNull TextRenderer renderer,
                                     @NotNull DrawContext context,
                                     @NotNull Text text,
                                     int y,
                                     int color) {
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2.0F, context.getScaledWindowHeight() / 2.0F + CoronerConstants.HUD_TRANSLATE_Y, 0.0F);
        context.getMatrices().scale(0.6F, 0.6F, 1.0F);
        context.drawTextWithShadow(renderer, text, -renderer.getWidth(text) / 2, y, color);
        context.getMatrices().pop();
    }
}
