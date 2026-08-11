package org.agmas.noellesroles.mixin.roles.spring_trap;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapSoundRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PlayerEntity 自身的移动音兜底。
 *
 * <p>Entity 层会取消多数脚步音；这里再处理 PlayerEntity 覆写的脚步方法，并把 MoveEffect 降级为只发事件不发声音。</p>
 */
@Mixin(PlayerEntity.class)
public abstract class SpringTrapPlayerMoveEffectMixin {
    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipSpringTrapPlayerStepSound(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (SpringTrapSoundRules.shouldSuppressStepSounds((PlayerEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "getMoveEffect", at = @At("RETURN"), cancellable = true)
    private void noellesroles$suppressSpringTrapMovementSounds(CallbackInfoReturnable<Entity.MoveEffect> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        cir.setReturnValue(SpringTrapSoundRules.suppressMovementSounds(
                cir.getReturnValue(),
                SpringTrapSoundRules.shouldSuppressSounds(player)
        ));
    }
}
