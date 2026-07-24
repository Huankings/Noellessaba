package org.agmas.noellesroles.client.appearance.roles.hacker;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.hacker.HackerComponent;
import org.agmas.noellesroles.roles.hacker.HackerConstants;
import org.agmas.noellesroles.roles.hacker.HackerSafeTimeComponent;
import org.agmas.noellesroles.roles.hacker.HackerTargeting;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

/**
 * 黑客准心破解 HUD。
 */
public final class HackerTargetHudHandler {
    private HackerTargetHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesAppearanceSupport.id("role_name/hacker_target"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    ClientPlayerEntity player = context.player();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (!gameWorld.isRole(player, NoellesRoleRegistry.HACKER) || !GameFunctions.isPlayerAliveAndSurvival(player)) {
                        return;
                    }

                    PlayerEntity target = aliveTarget(context.targetPlayer());
                    if (target == null
                            || gameWorld.getRole(target) == null
                            || HackerTargeting.countsAsFilteredKillerCohort(gameWorld, target)) {
                        return;
                    }

                    Text targetInfo;
                    if (HackerSafeTimeComponent.KEY.get(player.getWorld()).isSafe()) {
                        targetInfo = Text.translatable("hud.noellesroles.hacker.target_safe").withColor(NoellesRoleRegistry.HACKER.color());
                    } else {
                        HackerComponent targetHack = HackerComponent.KEY.get(target);
                        if (targetHack.hackingTime < HackerConstants.HACKING_TIME_TICKS) {
                            targetInfo = Text.translatable("hud.noellesroles.hacker.target")
                                    .styled(style -> style.withColor(NoellesRoleRegistry.HACKER.color()))
                                    .append(Text.literal(" [ " + (int) (((float) targetHack.hackingTime / HackerConstants.HACKING_TIME_TICKS) * 100) + "% ]")
                                            .styled(style -> style.withColor(Color.GREEN.getRGB())));
                        } else {
                            targetInfo = Text.translatable("hud.noellesroles.hacker.target_hacked")
                                    .styled(style -> style.withColor(Color.GREEN.getRGB()));
                        }
                    }

                    drawCentered(context.renderer(), context.drawContext(), targetInfo, 32, NoellesRoleRegistry.HACKER.color());
                }
        );
    }

    private static PlayerEntity aliveTarget(PlayerEntity target) {
        return target != null && GameFunctions.isPlayerAliveAndSurvival(target) ? target : null;
    }

    private static void drawCentered(@NotNull TextRenderer renderer,
                                     @NotNull DrawContext context,
                                     @NotNull Text text,
                                     int y,
                                     int color) {
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2.0F, context.getScaledWindowHeight() / 2.0F + 6.0F, 0.0F);
        context.getMatrices().scale(0.6F, 0.6F, 1.0F);
        context.drawTextWithShadow(renderer, text, -renderer.getWidth(text) / 2, y, color);
        context.getMatrices().pop();
    }
}
