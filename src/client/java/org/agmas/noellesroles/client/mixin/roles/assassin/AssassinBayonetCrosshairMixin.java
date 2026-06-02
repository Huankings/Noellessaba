package org.agmas.noellesroles.client.mixin.roles.assassin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import dev.doctor4t.wathe.item.KnifeItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.BayonetItem;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 刺刀专用准心。
 *
 * <p>Wathe 原版准心只会识别 {@code wathe:knife}，
 * 因此这里单独为刺刀补一套和匕首相同的冷却/锁定显示。</p>
 */
@Mixin(CrosshairRenderer.class)
public abstract class AssassinBayonetCrosshairMixin {

    @Unique private static final Identifier CROSSHAIR = Wathe.id("hud/crosshair");
    @Unique private static final Identifier CROSSHAIR_TARGET = Wathe.id("hud/crosshair_target");
    @Unique private static final Identifier KNIFE_ATTACK = Wathe.id("hud/knife_attack");
    @Unique private static final Identifier KNIFE_PROGRESS = Wathe.id("hud/knife_progress");
    @Unique private static final Identifier KNIFE_BACKGROUND = Wathe.id("hud/knife_background");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$renderBayonetCrosshair(
            @NotNull MinecraftClient client,
            @NotNull ClientPlayerEntity player,
            @NotNull DrawContext context,
            @NotNull RenderTickCounter tickCounter,
            @NotNull CallbackInfo ci
    ) {
        ItemStack mainHandStack = player.getMainHandStack();
        if (!client.options.getPerspective().isFirstPerson() || !mainHandStack.isOf(ModItems.BAYONET)) {
            return;
        }

        ci.cancel();

        boolean target = false;
        ItemCooldownManager manager = player.getItemCooldownManager();
        if (!manager.isCoolingDown(ModItems.BAYONET) && BayonetItem.getBayonetTarget(player) instanceof EntityHitResult) {
            target = true;
        }

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2F, context.getScaledWindowHeight() / 2F, 0.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        if (target) {
            context.drawGuiTexture(KNIFE_ATTACK, -5, 5, 10, 7);
        } else {
            float progress = 1.0F - manager.getCooldownProgress(ModItems.BAYONET, tickCounter.getTickDelta(true));
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
