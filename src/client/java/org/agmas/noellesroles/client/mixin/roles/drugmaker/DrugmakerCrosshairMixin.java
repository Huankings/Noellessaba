package org.agmas.noellesroles.client.mixin.roles.drugmaker;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrosshairRenderer.class)
public abstract class DrugmakerCrosshairMixin {
    @Unique private static final Identifier CROSSHAIR = Identifier.of("wathe", "hud/crosshair");
    @Unique private static final Identifier CROSSHAIR_TARGET = Identifier.of("wathe", "hud/crosshair_target");
    @Unique private static final Identifier KNIFE_ATTACK = Identifier.of("wathe", "hud/knife_attack");
    @Unique private static final Identifier KNIFE_PROGRESS = Identifier.of("wathe", "hud/knife_progress");
    @Unique private static final Identifier KNIFE_BACKGROUND = Identifier.of("wathe", "hud/knife_background");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$renderDrugmakerCrosshair(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player, @NotNull DrawContext context, @NotNull RenderTickCounter tickCounter, @NotNull CallbackInfo ci) {
        if (!client.options.getPerspective().isFirstPerson()) {
            return;
        }

        ItemStack mainHandStack = player.getMainHandStack();
        if (!mainHandStack.isOf(ModItems.BLOWGUN) && !mainHandStack.isOf(ModItems.POISON_INJECTOR)) {
            return;
        }

        ci.cancel();
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2F, context.getScaledWindowHeight() / 2F, 0);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        ItemCooldownManager manager = player.getItemCooldownManager();
        boolean target = false;
        if (mainHandStack.isOf(ModItems.BLOWGUN)) {
            target = hasTarget(player, DrugmakerConstants.BLOWGUN_TARGET_RANGE)
                    && (ignoresCooldown || !manager.isCoolingDown(mainHandStack.getItem()));
        } else {
            target = hasTarget(player, DrugmakerConstants.POISON_INJECTOR_TARGET_RANGE)
                    && (ignoresCooldown || !manager.isCoolingDown(mainHandStack.getItem()));
            if (target) {
                context.drawGuiTexture(KNIFE_ATTACK, -5, 5, 10, 7);
            } else {
                float progress = ignoresCooldown ? 1.0F : 1.0F - manager.getCooldownProgress(mainHandStack.getItem(), tickCounter.getTickDelta(true));
                context.drawGuiTexture(KNIFE_BACKGROUND, -5, 5, 10, 7);
                context.drawGuiTexture(KNIFE_PROGRESS, 10, 7, 0, 0, -5, 5, (int) (progress * 10.0F), 7);
            }
        }

        renderCrosshair(context, target);
    }

    @Unique
    private static boolean hasTarget(@NotNull PlayerEntity player, float range) {
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity targetPlayer && GameFunctions.isPlayerAliveAndSurvival(targetPlayer),
                range
        );
        return hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity;
    }

    @Unique
    private static void renderCrosshair(@NotNull DrawContext context, boolean target) {
        context.getMatrices().push();
        context.getMatrices().translate(-1.5F, -1.5F, 0);
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
