package org.agmas.noellesroles.client.mixin.roles.controller;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class ControlledScreenMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void renderControlledOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ControlledPlayerComponent controlledComp = ControlledPlayerComponent.KEY.get(client.player);

        if (controlledComp.isControlled) {
            // 检查玩家是否在旁观或创造模式
            if (client.player.isSpectator() || client.player.isCreative()) {
                return;
            }

            // 绘制全黑覆盖层
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();
            context.fill(0, 0, width, height, 0xFF000000); // 完全不透明的黑色

            // 显示"你被附体了！"的提示
            Text warning = Text.translatable("ui.controller.controlled_warning");
            int textWidth = client.textRenderer.getWidth(warning);
            context.drawCenteredTextWithShadow(client.textRenderer, warning,
                    width / 2, height / 2 - 10, 0xFFFF0000); // 红色文字

            // 显示倒计时提示（可选）
            if (client.player.getWorld().getPlayerByUuid(controlledComp.controller) != null) {
                Text controllerName = client.player.getWorld().getPlayerByUuid(controlledComp.controller).getName();
                Text info = Text.translatable("ui.controller.controlled_by", controllerName);
                context.drawCenteredTextWithShadow(client.textRenderer, info,
                        width / 2, height / 2 + 10, 0xFFFFFF00); // 黄色文字
            }
        }
    }
}