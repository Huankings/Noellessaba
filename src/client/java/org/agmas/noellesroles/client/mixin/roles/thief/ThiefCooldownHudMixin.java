package org.agmas.noellesroles.client.mixin.roles.thief;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class ThiefCooldownHudMixin {
    private static final int RIGHT_MARGIN = 10;
    private static final int BOTTOM_MARGIN = 10;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderThiefHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        DebugHud debugHud = client.inGameHud.getDebugHud();
        if (debugHud != null && debugHud.shouldShowDebugHud()) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, NoellesRoleRegistry.THIEF)) {
            return;
        }
        if (!WatheClient.isPlayerAliveAndInSurvival() && !client.player.isCreative()) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(client.player);
        Text keyName = client.options.useKey.getBoundKeyLocalizedText();
        Text displayText = ability.cooldown > 0
                ? Text.translatable("hud.noellesroles.thief.cooldown", Math.max(0, (ability.cooldown + 19) / 20), keyName)
                : Text.translatable("hud.noellesroles.thief.ready", keyName);

        TextRenderer renderer = this.getTextRenderer();
        int x = context.getScaledWindowWidth() - renderer.getWidth(displayText) - RIGHT_MARGIN;
        int y = context.getScaledWindowHeight() - BOTTOM_MARGIN;
        context.drawTextWithShadow(renderer, displayText, x, y, NoellesRoleRegistry.THIEF.color());
    }
}
