package org.agmas.noellesroles.client.mixin.roles.spring_trap;

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
 * 彩虹斧第三人称托举姿势。
 *
 * <p>Wathe 的球棒姿势只识别疯魔近战武器；彩虹斧的击杀逻辑由 Noelles 自己发包处理，
 * 所以这里单独补一条渲染姿势，确保旁观者视角能看到它被举起来。</p>
 */
@Mixin(LivingEntityRenderer.class)
public abstract class SpringTrapColorfulAxeRenderPoseMixin<T extends LivingEntity, M extends EntityModel<T>> {
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
    private void noellesroles$applyColorfulAxePose(LivingEntity entity,
                                                   float entityYaw,
                                                   float tickDelta,
                                                   MatrixStack matrices,
                                                   VertexConsumerProvider vertexConsumers,
                                                   int light,
                                                   CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayerEntity player)
                || !player.getMainHandStack().isOf(ModItems.COLORFUL_AXE)
                || !(this.model instanceof BipedEntityModel<?> bipedModel)) {
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
