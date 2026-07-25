package org.agmas.noellesroles.client.mixin.roles.thief;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.thief.ThiefConstants;
import org.agmas.noellesroles.roles.thief.ThiefInteractionHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrosshairRenderer.class)
public class ThiefCrosshairRendererMixin {
    @Unique private static final Identifier CROSSHAIR_TARGET = Identifier.of("wathe", "hud/crosshair_target");
    @Unique private static final Identifier THIEF_READY = NoellesRolesCore.id("hud/thief_ready");
    @Unique private static final Identifier THIEF_PROGRESS_FILL = NoellesRolesCore.id("hud/thief_progress_fill");
    @Unique private static final Identifier THIEF_PROGRESS_BACKGROUND = NoellesRolesCore.id("hud/thief_progress_background");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$renderThiefCrosshair(
            @NotNull MinecraftClient client,
            @NotNull ClientPlayerEntity player,
            @NotNull DrawContext context,
            @NotNull RenderTickCounter tickCounter,
            @NotNull CallbackInfo ci
    ) {
        if (!client.options.getPerspective().isFirstPerson() || !shouldShowThiefCrosshair(client, player)) {
            return;
        }

        ci.cancel();

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2F, context.getScaledWindowHeight() / 2F, 0.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown <= 0) {
            context.drawGuiTexture(THIEF_READY, -5, 5, 10, 7);
        } else {
            float progress = 1.0F - ((float) ability.cooldown / (float) ThiefConstants.STEAL_COOLDOWN_TICKS);
            context.drawGuiTexture(THIEF_PROGRESS_BACKGROUND, -5, 5, 10, 7);
            context.drawGuiTexture(THIEF_PROGRESS_FILL, 10, 7, 0, 0, -5, 5, Math.max(0, (int) (progress * 10.0F)), 7);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.getMatrices().push();
        context.getMatrices().translate(-1.5F, -1.5F, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        context.drawGuiTexture(CROSSHAIR_TARGET, 0, 0, 3, 3);
        context.getMatrices().pop();
        context.getMatrices().pop();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Unique
    private static boolean shouldShowThiefCrosshair(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player) {
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return false;
        }
        if (!dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.THIEF)) {
            return false;
        }
        if (!player.getMainHandStack().isEmpty()) {
            return false;
        }

        HitResult hitResult = client.crosshairTarget;
        if (hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerEntity target) {
            return GameFunctions.isPlayerAliveAndSurvival(target)
                    && player.squaredDistanceTo(target) <= ThiefConstants.CLIENT_STEAL_RANGE * ThiefConstants.CLIENT_STEAL_RANGE
                    && ThiefInteractionHandler.validateDistance(player, target);
        }
        return false;
    }
}
