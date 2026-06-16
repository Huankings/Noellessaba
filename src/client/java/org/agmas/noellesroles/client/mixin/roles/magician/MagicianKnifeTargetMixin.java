package org.agmas.noellesroles.client.mixin.roles.magician;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.KnifeItem;
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
 * 让匕首/刺刀的客户端目标检测能识别播放体。
 *
 * <p>刺刀内部直接复用 KnifeItem#getKnifeTarget，所以补这一处即可同时覆盖两条链。</p>
 */
@Mixin(KnifeItem.class)
public abstract class MagicianKnifeTargetMixin {

    @Inject(method = "getKnifeTarget", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$allowPlaybackTarget(@NotNull PlayerEntity user, CallbackInfoReturnable<HitResult> cir) {
        HitResult hitResult = ProjectileUtil.getCollision(
                user,
                entity -> (entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player))
                        || entity instanceof MagicianPlaybackEntity,
                3.0F
        );

        if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof MagicianPlaybackEntity) {
            cir.setReturnValue(hitResult);
        }
    }
}
