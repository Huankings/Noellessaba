package org.agmas.noellesroles.client.mixin.roles.magician;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.agmas.noellesroles.item.SniperRifleItem;
import org.agmas.noellesroles.roles.rememberer.RemembererConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让狙击枪准星高亮也能对播放体生效。
 */
@Mixin(SniperRifleItem.class)
public abstract class MagicianSniperVisibleTargetMixin {

    @Inject(method = "getVisibleTarget", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$allowPlaybackTarget(@NotNull PlayerEntity user, CallbackInfoReturnable<HitResult> cir) {
        HitResult hitResult = ProjectileUtil.getCollision(
                user,
                entity -> (entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player))
                        || entity instanceof MagicianPlaybackEntity,
                (float) RemembererConstants.SNIPER_RANGE_BLOCKS
        );

        if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof MagicianPlaybackEntity) {
            cir.setReturnValue(hitResult);
        }
    }
}
