package org.agmas.noellesroles.client.mixin.roles.waiter;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.roles.waiter.WaiterConstants;
import org.agmas.noellesroles.roles.waiter.WaiterServiceItems;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 服务员物品的准星提示。
 *
 * <p>当服务员手持可服务物品时，直接把 Wathe 默认准星替换成自定义图标：
 * 没有玩家目标时显示普通图标，锁定到可服务玩家时显示 target 图标。
 * 这样玩家能在第一人称下直观看到“现在按右键会递给谁”。</p>
 */
@Mixin(CrosshairRenderer.class)
public class WaiterCrosshairMixin {
    @Unique private static final Identifier CROSSHAIR = Identifier.of("wathe", "hud/crosshair");
    @Unique private static final Identifier CROSSHAIR_TARGET = Identifier.of("wathe", "hud/crosshair_target");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$renderWaiterCrosshair(
            @NotNull MinecraftClient client,
            @NotNull ClientPlayerEntity player,
            @NotNull DrawContext context,
            @NotNull RenderTickCounter tickCounter,
            @NotNull CallbackInfo ci
    ) {
        // 只有第一人称、存活服务员、且手里拿的是服务物品时才接管准星渲染。
        if (!client.options.getPerspective().isFirstPerson()
                || !WaiterServiceItems.isServiceStack(player.getMainHandStack())
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.WAITER)) {
            return;
        }

        ci.cancel();

        boolean target = false;
        // 同一套原版攻击距离判定，只是拿来判断准星是否已经对准一个活着的玩家。
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity targetPlayer && GameFunctions.isPlayerAliveAndSurvival(targetPlayer),
                WaiterConstants.INTERACTION_RANGE
        );
        if (hitResult instanceof EntityHitResult) {
            target = true;
        }

        // 复用 Wathe 的 HUD 准星资源，避免重新做一套风格不一致的图标。
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
