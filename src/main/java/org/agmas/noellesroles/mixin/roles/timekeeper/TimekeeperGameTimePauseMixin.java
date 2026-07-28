package org.agmas.noellesroles.mixin.roles.timekeeper;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时间回溯期间暂停局内倒计时。
 *
 * <p>Wathe 的局内时间由 GameTimeComponent.tick 每 tick 递减。
 * 回溯播放本身已经在应用历史快照，若这里继续递减，会导致目标快照的时间值刚恢复就被消耗，
 * 玩家看到的“局内剩余时间”也会在回溯期间继续流动。
 * 因此只在 TimekeeperWorldComponent 标记 rewinding 时取消 tick。</p>
 */
@Mixin(GameTimeComponent.class)
public abstract class TimekeeperGameTimePauseMixin {
    @Shadow
    public World world;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void noellesroles$pauseGameTimeDuringRewind(CallbackInfo ci) {
        if (TimekeeperWorldComponent.KEY.get(this.world).isRewinding()) {
            ci.cancel();
        }
    }
}
