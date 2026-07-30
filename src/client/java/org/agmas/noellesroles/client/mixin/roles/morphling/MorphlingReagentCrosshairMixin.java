package org.agmas.noellesroles.client.mixin.roles.morphling;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.morphling.MorphlingConstants;
import org.agmas.noellesroles.roles.morphling.MorphlingReagentService;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

/**
 * 变形试剂准心提示。
 *
 * <p>未采样时允许瞄准活玩家或尸体；已有样本后只允许瞄准活玩家标记。
 * 如果目标正好就是样本 UUID，准心保持普通形态，对应服务端“不消耗试剂”的失败规则。</p>
 */
@Mixin(CrosshairRenderer.class)
public abstract class MorphlingReagentCrosshairMixin {
    @Unique private static final Identifier CROSSHAIR = Identifier.of("wathe", "hud/crosshair");
    @Unique private static final Identifier CROSSHAIR_TARGET = Identifier.of("wathe", "hud/crosshair_target");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$renderMorphlingReagentCrosshair(
            @NotNull MinecraftClient client,
            @NotNull ClientPlayerEntity player,
            @NotNull DrawContext context,
            @NotNull RenderTickCounter tickCounter,
            @NotNull CallbackInfo ci
    ) {
        ItemStack stack = player.getMainHandStack();
        if (!client.options.getPerspective().isFirstPerson()
                || !stack.isOf(ModItems.MORPH_REAGENT)
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.MORPHLING)) {
            return;
        }

        ci.cancel();

        boolean hasSample = MorphlingReagentService.hasSample(stack);
        Optional<UUID> sampleUuid = MorphlingReagentService.sampleUuid(stack);
        boolean target = ProjectileUtil.getCollision(
                player,
                entity -> {
                    if (entity instanceof PlayerEntity targetPlayer) {
                        if (targetPlayer == player
                                || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                            return false;
                        }
                        return !hasSample || sampleUuid.map(uuid -> !uuid.equals(targetPlayer.getUuid())).orElse(true);
                    }
                    return !hasSample && entity instanceof PlayerBodyEntity;
                },
                MorphlingConstants.REAGENT_TARGET_RANGE
        ) instanceof EntityHitResult;

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2F, context.getScaledWindowHeight() / 2F, 0.0F);
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
