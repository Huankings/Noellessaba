package org.agmas.noellesroles.client.mixin.roles.vulture;

import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.agmas.noellesroles.roles.coroner.BodyDeathReasonComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerBodyEntityRenderer.class)
public abstract class VultureEatenBodyMixin {

    @Inject(method = "renderBody", at = @At("TAIL"), cancellable = true)
    public void vultureSkeletonOnly(PlayerBodyEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, float alpha, CallbackInfo ci) {
        BodyDeathReasonComponent bodyDeathReasonComponent = BodyDeathReasonComponent.KEY.get(livingEntity);
        if (bodyDeathReasonComponent.vultured) {
            ci.cancel();
        }
    }
}
