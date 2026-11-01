package org.agmas.noellesroles.client.mixin.roles.thief;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.KnifeItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.roles.thief.ThiefAllayEntity;
import org.agmas.noellesroles.roles.thief.ThiefConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 让 Wathe 匕首以及复用匕首射线的刺刀能够把小偷悦灵作为目标。 */
@Mixin(value = KnifeItem.class, priority = ThiefConstants.ALLAY_KNIFE_TARGET_MIXIN_PRIORITY)
public abstract class ThiefAllayKnifeTargetMixin {
    @Inject(method = "getKnifeTarget", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$targetThiefAllay(
            @NotNull PlayerEntity user,
            CallbackInfoReturnable<HitResult> cir
    ) {
        HitResult hit = ProjectileUtil.getCollision(
                user,
                entity -> (entity instanceof PlayerEntity player
                        && GameFunctions.isPlayerAliveAndSurvival(player)
                        && TargetVisibilityApi.canTargetPlayer(user, player))
                        || (entity instanceof ThiefAllayEntity allay
                        && allay.isOperationallyAlive()
                        && TargetVisibilityApi.canAttackEntity(user, allay)),
                ThiefConstants.ALLAY_KNIFE_HIT_RANGE
        );
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof ThiefAllayEntity) {
            cir.setReturnValue(hit);
        }
    }
}
