package org.agmas.noellesroles.client.mixin.roles.coward;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.agmas.noellesroles.client.roles.coward.CowardClientEffects;
import org.agmas.noellesroles.client.roles.coward.CowardClientEffects.CowardRevolverOffset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CowardCameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Shadow protected abstract void setPos(Vec3d pos);

    @Shadow public abstract float getYaw();

    @Shadow public abstract float getPitch();

    @Shadow public abstract Vec3d getPos();

    @Inject(method = "update", at = @At("RETURN"))
    private void noellesroles$applyCowardRevolverShake(
            BlockView area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (thirdPerson || !(focusedEntity instanceof ClientPlayerEntity player)) {
            return;
        }
        if (player != MinecraftClient.getInstance().player) {
            return;
        }

        CowardRevolverOffset offset = CowardClientEffects.getRevolverOffset(player);
        if (offset.isZero()) {
            return;
        }

        this.setRotation(this.getYaw() + offset.yawDegrees(), this.getPitch() + offset.pitchDegrees());
        this.setPos(this.getPos().add(offset.positionOffset()));
    }
}
