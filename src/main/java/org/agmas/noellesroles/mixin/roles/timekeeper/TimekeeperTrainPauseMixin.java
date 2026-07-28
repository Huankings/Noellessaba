package org.agmas.noellesroles.mixin.roles.timekeeper;

import dev.doctor4t.wathe.cca.TrainWorldComponent;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 时间回溯期间让列车表现为停止。
 *
 * <p>Wathe 的 TrainWorldComponent 一方面通过 getSpeed 给 HUD/逻辑读取当前速度，
 * 另一方面用内部 tickTime 计数驱动行驶时间。
 * 回溯播放中这两者都应暂停：对外速度显示为 0，内部行驶计数也不增长。</p>
 */
@Mixin(TrainWorldComponent.class)
public abstract class TimekeeperTrainPauseMixin {
    @Shadow
    private World world;

    @Inject(method = "getSpeed", at = @At("HEAD"), cancellable = true)
    private void noellesroles$forceZeroSpeedDuringRewind(CallbackInfoReturnable<Integer> cir) {
        if (TimekeeperWorldComponent.KEY.get(this.world).isRewinding()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "tickTime", at = @At("HEAD"), cancellable = true)
    private void noellesroles$pauseTrainTimeDuringRewind(CallbackInfo ci) {
        if (TimekeeperWorldComponent.KEY.get(this.world).isRewinding()) {
            ci.cancel();
        }
    }
}
