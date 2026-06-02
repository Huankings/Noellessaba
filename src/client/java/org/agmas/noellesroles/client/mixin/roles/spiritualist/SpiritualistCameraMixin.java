package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualPossessionCamera;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualProjectionCamera;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灵魂相机切换时的眼高与浸没层修正。
 */
@Mixin(Camera.class)
public abstract class SpiritualistCameraMixin {

    @Shadow private Entity focusedEntity;
    @Shadow private float cameraY;
    @Shadow private float lastCameraY;

    @Inject(method = "update", at = @At("HEAD"))
    private void noellesroles$fixProjectionEyeHeight(
            BlockView area,
            Entity newFocusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (newFocusedEntity == null) {
            return;
        }

        /*
         * 自定义相机切入 / 切出那一帧，仍然要把 Camera 内部缓存的 eye height
         * 直接重置到新相机实体的当前眼高。
         *
         * 但不能再像旧实现那样“每一帧都把 cameraY / lastCameraY 强行写死”：
         * 那会直接抹掉原版相机的眼高平滑，表现出来就是
         * 1. 蹲下、起身、进一格高姿态时镜头瞬间跳变；
         * 2. 附身宿主在低姿态里右键时，一旦姿态状态有一帧抖动，镜头也会跟着突兀抽动。
         *
         * 所以这里只在“真正切换到 / 离开自定义相机实体”的边沿重置一次缓存，
         * 后续眼高过渡继续交给原版 Camera 自己平滑处理。
         */
        if (this.focusedEntity == null || newFocusedEntity.equals(this.focusedEntity)) {
            return;
        }

        if (newFocusedEntity instanceof SpiritualProjectionCamera
                || this.focusedEntity instanceof SpiritualProjectionCamera
                || newFocusedEntity instanceof SpiritualPossessionCamera
                || this.focusedEntity instanceof SpiritualPossessionCamera) {
            this.lastCameraY = this.cameraY = newFocusedEntity.getStandingEyeHeight();
        }
    }

    @Inject(method = "getSubmersionType", at = @At("HEAD"), cancellable = true)
    private void noellesroles$hideProjectionSubmersion(CallbackInfoReturnable<CameraSubmersionType> cir) {
        if (SpiritualistClientController.isProjectionActive()) {
            cir.setReturnValue(CameraSubmersionType.NONE);
        }
    }

    @Inject(method = "getFocusedEntity", at = @At("HEAD"), cancellable = true)
    private void noellesroles$keepHostAsFocusedEntity(CallbackInfoReturnable<Entity> cir) {
        if (SpiritualistClientController.isUsingHostPossessionCamera()
                && SpiritualistClientController.getPossessionHost() != null) {
            cir.setReturnValue(SpiritualistClientController.getPossessionHost());
        }
    }
}
