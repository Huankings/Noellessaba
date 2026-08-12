package org.agmas.noellesroles.client.mixin.roles.insane_damned_paranoid_killer;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 亡语杀手尸体伪装的模型角度收口。
 *
 * <p>只把会明显暴露“这是活玩家”的头部视角跟随和原版空闲手臂摆动归零。
 * 移动、持物和其它渲染层不在这里改，避免把渲染 mixin 扩成新的通用动画系统。</p>
 */
@Mixin(LivingEntityRenderer.class)
public class InsaneDamnedKillerCorpseAnglesMixin<T extends LivingEntity, M extends EntityModel<T>> {
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
    private void noellesroles$resetInsaneDamnedKillerCorpseAngles(T entity,
                                                                  float entityYaw,
                                                                  float tickDelta,
                                                                  MatrixStack matrices,
                                                                  VertexConsumerProvider vertexConsumers,
                                                                  int light,
                                                                  CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayerEntity player)
                || !InsaneDamnedKillerPlayerComponent.isActiveCorpseMode(player)
                || !(this.model instanceof PlayerEntityModel<?> playerModel)) {
            return;
        }

        /*
         * 头部固定在尸体姿态，避免其他玩家看到“尸体”跟着视角转头。
         */
        playerModel.head.pitch = 0.0F;
        playerModel.head.yaw = 0.0F;
        playerModel.hat.copyTransform(playerModel.head);

        /*
         * 原版 CrossbowPosing.swingArm 会给空闲手臂叠一层细微摆动。
         * spark 版通过反推公式把它抵消；这里沿用同样做法，让尸体不会呼吸般晃手。
         */
        float ageInTicks = entity.age + tickDelta;
        float swingRollBase = MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        float swingPitchBase = MathHelper.sin(ageInTicks * 0.067F) * 0.05F;

        playerModel.rightArm.roll -= swingRollBase;
        playerModel.rightArm.pitch -= swingPitchBase;
        playerModel.leftArm.roll += swingRollBase;
        playerModel.leftArm.pitch += swingPitchBase;

        playerModel.leftSleeve.copyTransform(playerModel.leftArm);
        playerModel.rightSleeve.copyTransform(playerModel.rightArm);
    }
}
