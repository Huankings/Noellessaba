package org.agmas.noellesroles.client.mixin.roles.bounty_hunter;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.BountyDerringerItem;
import org.agmas.noellesroles.item.BountyPistolItem;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 赏金枪械专用准星。
 *
 * <p>强盗手枪可以复用 Wathe 左轮 20 格准星，但赏金手枪是 15 格、赏金德林加是 7 格。
 * 这里直接在 HEAD 接管两把赏金枪，确保准星高亮范围和服务端真实射程一致。</p>
 */
@Mixin(CrosshairRenderer.class)
public class BountyGunCrosshairMixin {
    @Unique private static final Identifier CROSSHAIR = Identifier.of("wathe", "hud/crosshair");
    @Unique private static final Identifier CROSSHAIR_TARGET = Identifier.of("wathe", "hud/crosshair_target");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$renderBountyGunCrosshair(
            @NotNull MinecraftClient client,
            @NotNull ClientPlayerEntity player,
            @NotNull DrawContext context,
            @NotNull RenderTickCounter tickCounter,
            @NotNull CallbackInfo ci
    ) {
        boolean holdingBountyPistol = player.getMainHandStack().isOf(ModItems.BOUNTY_PISTOL);
        boolean holdingBountyDerringer = player.getMainHandStack().isOf(ModItems.BOUNTY_DERRINGER);
        if (!client.options.getPerspective().isFirstPerson() || (!holdingBountyPistol && !holdingBountyDerringer)) {
            return;
        }

        ci.cancel();

        boolean target = !player.getItemCooldownManager().isCoolingDown(player.getMainHandStack().getItem())
                && (holdingBountyPistol
                ? BountyPistolItem.getGunTarget(player) instanceof EntityHitResult
                : BountyDerringerItem.getGunTarget(player) instanceof EntityHitResult);

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2F, context.getScaledWindowHeight() / 2F, 0.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
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
