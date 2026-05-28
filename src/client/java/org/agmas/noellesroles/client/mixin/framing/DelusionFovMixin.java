package org.agmas.noellesroles.client.mixin.framing;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 把幻觉试剂的视角脉冲叠加到客户端 FOV 上。
 *
 * <p>wathe 原本只会读取真实中毒组件，因此幻觉试剂脱离毒药系统以后，
 * 需要在 noellesroles 这边单独补上一层客户端视觉效果。</p>
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class DelusionFovMixin extends PlayerEntity {
    public DelusionFovMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void noellesroles$applyDelusionFov(CallbackInfoReturnable<Float> cir) {
        float original = cir.getReturnValue();
        cir.setReturnValue(original * DelusionPlayerComponent.getFovMultiplier(1f, DelusionPlayerComponent.KEY.get(this)));
    }
}
