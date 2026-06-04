package org.agmas.noellesroles.client.mixin.roles.rememberer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.roles.rememberer.RemembererClientEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 追忆者右下角提示 HUD。
 */
@Mixin(InGameHud.class)
public abstract class RemembererHudMixin {

    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderRemembererHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!RemembererClientEffects.shouldRenderRemembererHud(client.player)) {
            return;
        }

        Text line;
        AbilityPlayerComponent abilityComponent = AbilityPlayerComponent.KEY.get(client.player);
        if (abilityComponent.cooldown > 0) {
            line = Text.translatable("tip.noellesroles.cooldown", Math.max(0, (abilityComponent.cooldown + 19) / 20));
        } else {
            line = Text.translatable("hud.noellesroles.rememberer.use", client.options.useKey.getBoundKeyLocalizedText());
        }

        int drawY = context.getScaledWindowHeight() - getTextRenderer().fontHeight;
        context.drawTextWithShadow(
                getTextRenderer(),
                line,
                context.getScaledWindowWidth() - getTextRenderer().getWidth(line),
                drawY,
                Noellesroles.REMEMBERER.color()
        );
    }
}
