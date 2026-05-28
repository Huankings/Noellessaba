package org.agmas.noellesroles.mixin.roles.jester;

import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RevolverItem.class)
public abstract class FakeRevolverMixin {

    @Inject(method = "getGunTarget", at = @At("HEAD"), cancellable = true)
    private static void jesterFakeGun(PlayerEntity user, CallbackInfoReturnable<HitResult> cir) {
        if (user.getMainHandStack().isOf(ModItems.FAKE_REVOLVER)) {
            cir.setReturnValue(null);
        }
    }

}
