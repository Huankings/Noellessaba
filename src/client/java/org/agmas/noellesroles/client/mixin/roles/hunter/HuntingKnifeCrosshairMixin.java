package org.agmas.noellesroles.client.mixin.roles.hunter;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.hunter.HunterConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 猎刀专用准星。
 *
 * <p>Wathe 原版准星只识别自己的匕首；猎刀需要单独补同款锁定/冷却显示。</p>
 */
@Mixin(CrosshairRenderer.class)
public class HuntingKnifeCrosshairMixin {
    @Unique private static final Identifier CROSSHAIR = Wathe.id("hud/crosshair");
    @Unique private static final Identifier CROSSHAIR_TARGET = Wathe.id("hud/crosshair_target");
    @Unique private static final Identifier KNIFE_ATTACK = Wathe.id("hud/knife_attack");
    @Unique private static final Identifier KNIFE_PROGRESS = Wathe.id("hud/knife_progress");
    @Unique private static final Identifier KNIFE_BACKGROUND = Wathe.id("hud/knife_background");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$renderHuntingKnifeCrosshair(
            @NotNull MinecraftClient client,
            @NotNull ClientPlayerEntity player,
            @NotNull DrawContext context,
            @NotNull RenderTickCounter tickCounter,
            @NotNull CallbackInfo ci
    ) {
        if (!client.options.getPerspective().isFirstPerson() || !player.getMainHandStack().isOf(ModItems.HUNTING_KNIFE)) {
            return;
        }

        ci.cancel();

        boolean target = false;
        ItemCooldownManager manager = player.getItemCooldownManager();
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity targetPlayer && GameFunctions.isPlayerAliveAndSurvival(targetPlayer),
                HunterConstants.HUNTING_KNIFE_TARGET_RANGE
        );
        /*
         * 与物品 use()/服务端命中保持一致：创造/旁观语义玩家调试猎刀时忽略冷却。
         * 否则即使右键不被拦截，准星仍会因为客户端冷却显示成不可命中状态。
         */
        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        if ((ignoresCooldown || !manager.isCoolingDown(ModItems.HUNTING_KNIFE)) && hitResult instanceof EntityHitResult) {
            target = true;
        }

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2F, context.getScaledWindowHeight() / 2F, 0.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        if (target) {
            context.drawGuiTexture(KNIFE_ATTACK, -5, 5, 10, 7);
        } else {
            float progress = 1.0F - manager.getCooldownProgress(ModItems.HUNTING_KNIFE, tickCounter.getTickDelta(true));
            context.drawGuiTexture(KNIFE_BACKGROUND, -5, 5, 10, 7);
            context.drawGuiTexture(KNIFE_PROGRESS, 10, 7, 0, 0, -5, 5, (int) (progress * 10.0F), 7);
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
