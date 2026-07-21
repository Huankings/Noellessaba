package org.agmas.noellesroles.client.mixin.roles.starstruck;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 星界使者右下角能力提示。
 */
@Mixin(InGameHud.class)
public abstract class StarstruckHudMixin {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void renderStarstruckHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, Noellesroles.STARSTRUCK)) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(client.player);
        Text line = ability.cooldown > 0
                ? Text.translatable("tip.noellesroles.cooldown", Math.max(0, (ability.cooldown + 19) / 20))
                : Text.translatable("tip.noellesroles.starstruck", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());

        int drawY = context.getScaledWindowHeight() - getTextRenderer().getWrappedLinesHeight(line, 999999);
        context.drawTextWithShadow(
                getTextRenderer(),
                line,
                context.getScaledWindowWidth() - getTextRenderer().getWidth(line),
                drawY,
                Noellesroles.STARSTRUCK.color()
        );
    }
}
