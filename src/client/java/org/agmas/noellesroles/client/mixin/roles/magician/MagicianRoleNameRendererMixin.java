package org.agmas.noellesroles.client.mixin.roles.magician;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 给 Wathe 的中央准星名牌 HUD 补上“播放体显示名字”。
 */
@Mixin(dev.doctor4t.wathe.client.gui.RoleNameRenderer.class)
public abstract class MagicianRoleNameRendererMixin {

    @Unique private static float noellesroles$magicianNametagAlpha = 0.0F;
    @Unique private static Text noellesroles$magicianNametag = Text.empty();

    @Inject(method = "renderHud", at = @At("TAIL"))
    private static void noellesroles$renderPlaybackName(
            TextRenderer renderer,
            ClientPlayerEntity player,
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {
        if (player.getWorld().getLightLevel(LightType.BLOCK, BlockPos.ofFloored(player.getEyePos())) < 3
                && player.getWorld().getLightLevel(LightType.SKY, BlockPos.ofFloored(player.getEyePos())) < 10) {
            return;
        }

        float range = GameFunctions.isPlayerSpectatingOrCreative(player) ? 8.0F : 2.0F;
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> (entity instanceof PlayerEntity otherPlayer && GameFunctions.isPlayerAliveAndSurvival(otherPlayer))
                        || entity instanceof MagicianPlaybackEntity,
                range
        );

        if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof MagicianPlaybackEntity playbackEntity) {
            noellesroles$magicianNametagAlpha = MathHelper.lerp(tickCounter.getTickDelta(true) / 4.0F, noellesroles$magicianNametagAlpha, 1.0F);
            noellesroles$magicianNametag = Text.literal(playbackEntity.getDisguisePlayerName());
        } else {
            noellesroles$magicianNametagAlpha = MathHelper.lerp(tickCounter.getTickDelta(true) / 4.0F, noellesroles$magicianNametagAlpha, 0.0F);
        }

        if (noellesroles$magicianNametagAlpha <= 0.05F) {
            return;
        }

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2.0F, context.getScaledWindowHeight() / 2.0F + 6.0F, 0.0F);
        context.getMatrices().scale(0.6F, 0.6F, 1.0F);
        int nameWidth = renderer.getWidth(noellesroles$magicianNametag);
        context.drawTextWithShadow(
                renderer,
                noellesroles$magicianNametag,
                -nameWidth / 2,
                16,
                MathHelper.packRgb(1.0F, 1.0F, 1.0F) | ((int) (noellesroles$magicianNametagAlpha * 255.0F) << 24)
        );
        context.getMatrices().pop();
    }
}
