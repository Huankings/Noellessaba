package org.agmas.noellesroles.client.mixin.roles.kidnapper;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;
import org.agmas.noellesroles.roles.kidnapper.KidnapperConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class KidnapperControlledMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void noellesroles$renderKidnapperControlledHud(@NotNull DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        KidnapperComponent controlled = KidnapperComponent.KEY.get(client.player);
        if (controlled.controlTicks <= 0 || !WatheClient.isPlayerAliveAndInSurvival()) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        context.fill(0, 0, width, height, 0xFF000000);
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.translatable("tip.noellesroles.kidnapper.warning"),
                width / 2,
                height / 2 - 10,
                KidnapperConstants.ROLE_COLOR
        );
        context.drawCenteredTextWithShadow(
                client.textRenderer,
                Text.translatable("tip.noellesroles.kidnapper.timeleft", controlled.controlTicks / 20),
                width / 2,
                height / 2 + 10,
                KidnapperConstants.ROLE_COLOR
        );
    }
}
