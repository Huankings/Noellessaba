package org.agmas.noellesroles.client.mixin.roles.dreamer;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.agmas.noellesroles.roles.dreamer.DreamerKillerComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class DreamerHudMixin {
    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderDreamerCounts(@NotNull DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        DreamerKillerComponent dreamer = DreamerKillerComponent.KEY.get(MinecraftClient.getInstance().player);
        if (!gameWorld.isRole(MinecraftClient.getInstance().player, NoellesRoleRegistry.DREAMER)
                || !WatheClient.isPlayerAliveAndInSurvival()
                || dreamer.hasBecomeKiller()) {
            return;
        }

        int drawY = context.getScaledWindowHeight();
        Text line = Text.translatable("tip.noellesroles.dreamer.counts", dreamer.dreamerCounts, dreamer.dreamerRequired);
        drawY -= getTextRenderer().getWrappedLinesHeight(line, 999999);
        context.drawTextWithShadow(getTextRenderer(), line, context.getScaledWindowWidth() - getTextRenderer().getWidth(line), drawY, NoellesRoleRegistry.DREAMER.color());
    }
}
