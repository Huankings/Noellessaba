package org.agmas.noellesroles.client.mixin.roles.magician;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.DerringerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让短枪客户端目标检测也能命中播放体。
 */
@Mixin(DerringerItem.class)
public abstract class MagicianDerringerTargetMixin {

    @Inject(method = "getGunTarget", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$allowPlaybackTarget(@NotNull PlayerEntity user, CallbackInfoReturnable<HitResult> cir) {
        HitResult hitResult = ProjectileUtil.getCollision(
                user,
                entity -> (entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player))
                        || entity instanceof MagicianPlaybackEntity,
                7.0F
        );

        if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof MagicianPlaybackEntity) {
            cir.setReturnValue(hitResult);
        }
    }
}
