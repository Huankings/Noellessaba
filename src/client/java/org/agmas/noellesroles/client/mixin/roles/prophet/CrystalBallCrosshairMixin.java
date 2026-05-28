package org.agmas.noellesroles.client.mixin.roles.prophet;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.CrystalBallItem;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 水晶球专用准心渲染。
 *
 * <p>这里单独做一个 mixin，而不是把逻辑硬塞进现有准心扩展里，
 * 这样后续你如果还要继续加“占卜类物品”的特殊准心，
 * 也更容易独立维护。</p>
 */
@Mixin(CrosshairRenderer.class)
public class CrystalBallCrosshairMixin {

    @Unique private static final Identifier CROSSHAIR = Identifier.of("wathe", "hud/crosshair");
    @Unique private static final Identifier CROSSHAIR_TARGET = Identifier.of("wathe", "hud/crosshair_target");
    @Unique private static final Identifier KNIFE_ATTACK = Identifier.of("wathe", "hud/knife_attack");
    @Unique private static final Identifier KNIFE_PROGRESS = Identifier.of("wathe", "hud/knife_progress");
    @Unique private static final Identifier KNIFE_BACKGROUND = Identifier.of("wathe", "hud/knife_background");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void renderCrystalBallCrosshair(
            @NotNull MinecraftClient client,
            @NotNull ClientPlayerEntity player,
            @NotNull DrawContext context,
            @NotNull RenderTickCounter tickCounter,
            @NotNull CallbackInfo ci
    ) {
        if (!client.options.getPerspective().isFirstPerson()) {
            return;
        }
        if (!player.getMainHandStack().isOf(ModItems.CRYSTAL_BALL) && !player.getOffHandStack().isOf(ModItems.CRYSTAL_BALL)) {
            return;
        }

        ci.cancel();

        boolean target = false;
        HitResult hitResult = CrystalBallItem.getCrystalBallTarget(player);
        if (hitResult instanceof EntityHitResult) {
            target = true;
        }

        float chargeProgress = CrystalBallItem.getChargeProgress(player);

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2F, context.getScaledWindowHeight() / 2F, 0.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        if (chargeProgress >= 1.0F && target) {
            context.drawGuiTexture(KNIFE_ATTACK, -5, 5, 10, 7);
        } else {
            context.drawGuiTexture(KNIFE_BACKGROUND, -5, 5, 10, 7);
            context.drawGuiTexture(KNIFE_PROGRESS, 10, 7, 0, 0, -5, 5, (int) (chargeProgress * 10.0F), 7);
        }

        context.getMatrices().push();
        context.getMatrices().translate(-1.5F, -1.5F, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        context.drawGuiTexture(target ? CROSSHAIR_TARGET : CROSSHAIR, 0, 0, 3, 3);
        context.getMatrices().pop();
        context.getMatrices().pop();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
