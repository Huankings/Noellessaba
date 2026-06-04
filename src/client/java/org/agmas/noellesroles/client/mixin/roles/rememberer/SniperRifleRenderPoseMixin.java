package org.agmas.noellesroles.client.mixin.roles.rememberer;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.CrossbowPosing;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在玩家模型已经算完基础动作后，再把双臂修正到“举枪托举”的姿态。
 *
 * <p>只改 {@code PlayerEntityRenderer#getArmPose} 在装了 ETF / EMF 和自定义动作资源包后
 * 可能会被新的玩家模型动画覆盖掉，所以这里改成更靠后的渲染阶段兜底：
 * 只要第三人称正在渲染一个主手拿狙击枪的玩家，就直接复用原版
 * {@link CrossbowPosing#hold} 的双手持械姿态，把枪重新架起来。</p>
 */
@Mixin(LivingEntityRenderer.class)
public abstract class SniperRifleRenderPoseMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow
    protected M model;

    @Inject(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void noellesroles$applySniperHoldPose(LivingEntity entity,
                                                  float entityYaw,
                                                  float tickDelta,
                                                  MatrixStack matrices,
                                                  VertexConsumerProvider vertexConsumers,
                                                  int light,
                                                  CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) {
            return;
        }
        if (!player.getMainHandStack().isOf(ModItems.SNIPER_RIFLE)) {
            return;
        }
        if (!(this.model instanceof BipedEntityModel<?> bipedModel)) {
            return;
        }

        boolean rightArmed = player.getMainArm() == Arm.RIGHT;
        if (rightArmed) {
            CrossbowPosing.hold(bipedModel.rightArm, bipedModel.leftArm, bipedModel.head, true);
        } else {
            CrossbowPosing.hold(bipedModel.leftArm, bipedModel.rightArm, bipedModel.head, false);
        }
    }
}
