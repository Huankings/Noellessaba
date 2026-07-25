package org.agmas.noellesroles.client.roles.stalker;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.stalker.StalkerPlayerComponent;

/**
 * 潜行者右下角阶段状态 HUD。
 */
public final class StalkerStatusHud {
    private StalkerStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/stalker/status", NoellesRoleRegistry.STALKER, context -> {
            StalkerPlayerComponent stalker = StalkerPlayerComponent.KEY.get(context.player());
            if (!stalker.isActiveStalker()) {
                return;
            }

            int y = context.height() - 80;
            TextRenderer renderer = context.textRenderer();

            Text phaseText = switch (stalker.phase) {
                case 1 -> Text.translatable("hud.noellesroles.stalker.phase1").formatted(Formatting.DARK_PURPLE);
                case 2 -> Text.translatable("hud.noellesroles.stalker.phase2").formatted(Formatting.RED);
                case 3 -> Text.translatable("hud.noellesroles.stalker.phase3").formatted(Formatting.DARK_RED);
                default -> Text.empty();
            };
            y = drawLine(context, renderer, phaseText, y, 0xFFFFFF);

            int maxEnergy = stalker.phase == 1 ? stalker.getPhase1EnergyRequired() : stalker.getPhase2EnergyRequired();
            y = drawLine(context, renderer, Text.translatable("hud.noellesroles.stalker.energy", stalker.energy, maxEnergy), y, 0xAAAAAA);

            if (stalker.phase == 1) {
                Text immunityText = stalker.immunityUsed
                        ? Text.translatable("hud.noellesroles.stalker.immunity_used").formatted(Formatting.GRAY)
                        : Text.translatable("hud.noellesroles.stalker.immunity_available").formatted(Formatting.GREEN);
                y = drawLine(context, renderer, immunityText, y, 0xFFFFFF);
            }

            if (stalker.phase >= 2) {
                y = drawLine(
                        context,
                        renderer,
                        Text.translatable("hud.noellesroles.stalker.kills", stalker.phase2Kills, stalker.getPhase2KillsRequired()),
                        y,
                        0xFF6666
                );
            }

            if (stalker.phase >= 2 && stalker.attackCooldown > 0) {
                float cooldownSec = stalker.attackCooldown / 20.0f;
                Text cooldownText = Text.translatable("hud.noellesroles.stalker.attack_cooldown", String.format("%.1f", cooldownSec)).formatted(Formatting.RED);
                y = drawLine(context, renderer, cooldownText, y, 0xFF0000);
            }

            if (stalker.phase == 3) {
                int seconds = stalker.phase3Timer / 20;
                int minutes = seconds / 60;
                seconds %= 60;
                Text timerText = Text.translatable("hud.noellesroles.stalker.timer", String.format("%d:%02d", minutes, seconds));
                int color = stalker.phase3Timer < 30 * 20 ? 0xFF0000 : 0xFFAA00;
                y = drawLine(context, renderer, timerText, y, color);
            }

            if (stalker.isGazing) {
                Text gazingText = Text.translatable("hud.noellesroles.stalker.gazing", stalker.gazingTargetCount).formatted(Formatting.YELLOW);
                y = drawLine(context, renderer, gazingText, y, 0xFFFFFF);
            }

            if (stalker.isCharging) {
                float chargeSeconds = stalker.chargeTime / 20.0f;
                float maxCharge = StalkerPlayerComponent.MAX_CHARGE_SECONDS;
                Text chargeText = Text.translatable("hud.noellesroles.stalker.charging", String.format("%.1f", chargeSeconds), String.format("%.1f", maxCharge));
                int chargeColor = chargeSeconds >= 1.0f ? 0x00FF00 : 0xFFFF00;
                y = drawLine(context, renderer, chargeText, y, chargeColor);
            }

            if (stalker.isDashing) {
                Text dashText = Text.translatable("hud.noellesroles.stalker.dashing").formatted(Formatting.AQUA, Formatting.BOLD);
                drawLine(context, renderer, dashText, y, 0xFFFFFF);
            }
        });
    }

    private static int drawLine(dev.doctor4t.wathe.api.client.hud.HudOverlayContext context,
                                TextRenderer renderer,
                                Text text,
                                int y,
                                int color) {
        context.drawContext().drawText(renderer, text, context.width() - renderer.getWidth(text) - 5, y, color, true);
        return y - 12;
    }
}
