package org.agmas.noellesroles.client.mixin.roles.coward;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import org.agmas.noellesroles.client.roles.coward.CowardClientEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class CowardFovMixin {
    @WrapOperation(
            method = "updateFovMultiplier",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getFovMultiplier()F"
            )
    )
    private float noellesroles$applyCowardFovFromGameRenderer(AbstractClientPlayerEntity player, Operation<Float> original) {
        float originalFovMultiplier = original.call(player);
        return originalFovMultiplier * CowardClientEffects.getFovPulseMultiplier(1.0f);
    }
}
