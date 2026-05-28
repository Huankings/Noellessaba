package org.agmas.noellesroles.client.mixin.roles.stalker;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.stalker.StalkerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class StalkerHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void renderStalkerHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, Noellesroles.STALKER)) return;
        StalkerPlayerComponent comp = StalkerPlayerComponent.KEY.get(client.player);
        if (!comp.isActiveStalker()) return;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        TextRenderer renderer = getTextRenderer();
        int y = height - 80; // 右下角，从底部向上

        // 阶段
        Text phaseText = switch (comp.phase) {
            case 1 -> Text.translatable("hud.noellesroles.stalker.phase1").formatted(Formatting.DARK_PURPLE);
            case 2 -> Text.translatable("hud.noellesroles.stalker.phase2").formatted(Formatting.RED);
            case 3 -> Text.translatable("hud.noellesroles.stalker.phase3").formatted(Formatting.DARK_RED);
            default -> Text.empty();
        };
        context.drawText(renderer, phaseText, width - renderer.getWidth(phaseText) - 5, y, 0xFFFFFF, true);
        y -= 12;

        // 能量
        int maxEnergy = comp.phase == 1 ? comp.getPhase1EnergyRequired() : comp.getPhase2EnergyRequired();
        Text energyText = Text.translatable("hud.noellesroles.stalker.energy", comp.energy, maxEnergy);
        context.drawText(renderer, energyText, width - renderer.getWidth(energyText) - 5, y, 0xAAAAAA, true);
        y -= 12;

        // 一阶段免疫状态
        if (comp.phase == 1) {
            Text immunityText = comp.immunityUsed
                    ? Text.translatable("hud.noellesroles.stalker.immunity_used").formatted(Formatting.GRAY)
                    : Text.translatable("hud.noellesroles.stalker.immunity_available").formatted(Formatting.GREEN);
            context.drawText(renderer, immunityText, width - renderer.getWidth(immunityText) - 5, y, 0xFFFFFF, true);
            y -= 12;
        }

        // 二阶段击杀数
        if (comp.phase >= 2) {
            Text killsText = Text.translatable("hud.noellesroles.stalker.kills", comp.phase2Kills, comp.getPhase2KillsRequired());
            context.drawText(renderer, killsText, width - renderer.getWidth(killsText) - 5, y, 0xFF6666, true);
            y -= 12;
        }

        // 攻击冷却
        if (comp.phase >= 2 && comp.attackCooldown > 0) {
            float cooldownSec = comp.attackCooldown / 20.0f;
            Text cooldownText = Text.translatable("hud.noellesroles.stalker.attack_cooldown", String.format("%.1f", cooldownSec)).formatted(Formatting.RED);
            context.drawText(renderer, cooldownText, width - renderer.getWidth(cooldownText) - 5, y, 0xFF0000, true);
            y -= 12;
        }

        // 三阶段倒计时
        if (comp.phase == 3) {
            int seconds = comp.phase3Timer / 20;
            int minutes = seconds / 60;
            seconds %= 60;
            Text timerText = Text.translatable("hud.noellesroles.stalker.timer", String.format("%d:%02d", minutes, seconds));
            int color = comp.phase3Timer < 30 * 20 ? 0xFF0000 : 0xFFAA00;
            context.drawText(renderer, timerText, width - renderer.getWidth(timerText) - 5, y, color, true);
            y -= 12;
        }

        // 窥视状态
        if (comp.isGazing) {
            Text gazingText = Text.translatable("hud.noellesroles.stalker.gazing", comp.gazingTargetCount).formatted(Formatting.YELLOW);
            context.drawText(renderer, gazingText, width - renderer.getWidth(gazingText) - 5, y, 0xFFFFFF, true);
            y -= 12;
        }

        // 蓄力进度
        if (comp.isCharging) {
            float chargeSeconds = comp.chargeTime / 20.0f;
            float maxCharge = StalkerPlayerComponent.MAX_CHARGE_SECONDS;
            Text chargeText = Text.translatable("hud.noellesroles.stalker.charging", String.format("%.1f", chargeSeconds), String.format("%.1f", maxCharge));
            int chargeColor = chargeSeconds >= 1.0f ? 0x00FF00 : 0xFFFF00;
            context.drawText(renderer, chargeText, width - renderer.getWidth(chargeText) - 5, y, chargeColor, true);
            y -= 12;
        }

        // 突进状态
        if (comp.isDashing) {
            Text dashText = Text.translatable("hud.noellesroles.stalker.dashing").formatted(Formatting.AQUA, Formatting.BOLD);
            context.drawText(renderer, dashText, width - renderer.getWidth(dashText) - 5, y, 0xFFFFFF, true);
        }
    }
}