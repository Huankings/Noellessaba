package org.agmas.noellesroles.client.mixin.roles.detective;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.roles.detective.DetectiveConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 侦探右下角能力 HUD。
 */
@Mixin(InGameHud.class)
public abstract class DetectiveHudMixin {
    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderDetectiveHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || NoellesrolesClient.abilityBind == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, NoellesRoleRegistry.DETECTIVE)
                || !GameFunctions.isPlayerAliveAndSurvival(client.player)) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(client.player);
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(client.player);
        Text line;
        if (shop.balance < DetectiveConstants.ABILITY_PRICE) {
            line = Text.translatable("tip.noellesroles.detective.not_enough_money", DetectiveConstants.ABILITY_PRICE);
        } else if (ability.cooldown > 0) {
            line = Text.translatable("tip.noellesroles.cooldown", Math.max(0, (ability.cooldown + 19) / 20));
        } else {
            line = Text.translatable("tip.noellesroles.detective.use", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
        }

        int drawY = context.getScaledWindowHeight() - getTextRenderer().getWrappedLinesHeight(line, 999999);
        context.drawTextWithShadow(
                getTextRenderer(),
                line,
                context.getScaledWindowWidth() - getTextRenderer().getWidth(line),
                drawY,
                NoellesRoleRegistry.DETECTIVE.color()
        );
    }
}
