package org.agmas.noellesroles.mixin.roles.spring_trap;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapSoundRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 弹簧陷阱状态下取消玩家脚步声音主路径。
 *
 * <p>这里覆盖 Entity 的普通脚步、组合脚步和第二层脚步音，避免地毯/特殊方块等声音从其他分支漏出。</p>
 */
@Mixin(Entity.class)
public abstract class SpringTrapEntitySoundMixin {
    @Inject(method = "playStepSounds", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipSpringTrapStepSounds(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (SpringTrapSoundRules.shouldSuppressStepSounds((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipSpringTrapDirectStepSound(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (SpringTrapSoundRules.shouldSuppressStepSounds((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "playCombinationStepSounds", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipSpringTrapCombinationStepSounds(BlockState primaryState, BlockState secondaryState, CallbackInfo ci) {
        if (SpringTrapSoundRules.shouldSuppressStepSounds((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "playSecondaryStepSound", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipSpringTrapSecondaryStepSound(BlockState state, CallbackInfo ci) {
        if (SpringTrapSoundRules.shouldSuppressStepSounds((Entity) (Object) this)) {
            ci.cancel();
        }
    }
}
