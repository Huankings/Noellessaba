package org.agmas.noellesroles.client.mixin.roles.coward;

import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.client.roles.coward.CowardClientEffects;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RevolverItem.class)
public abstract class CowardRevolverTargetMixin {
    @Inject(method = "getGunTarget", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$applyCowardAimOffset(@NotNull PlayerEntity user, CallbackInfoReturnable<HitResult> cir) {
        HitResult offsetHit = CowardClientEffects.getOffsetRevolverTarget(user);
        if (offsetHit != null) {
            cir.setReturnValue(offsetHit);
        }
    }
}
