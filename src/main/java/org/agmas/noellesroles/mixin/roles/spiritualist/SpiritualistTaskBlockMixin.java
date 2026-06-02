package org.agmas.noellesroles.mixin.roles.spiritualist;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistHostComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 灵术师附身相关的任务完成拦截。
 *
 * <p>用户要求两边都不能在附身期间推进任务：</p>
 * <p>1. 灵术师自己附身时，无法借宿主身体偷偷做任务；</p>
 * <p>2. 被附身者也不能在完全失去操作权时被系统判成“完成了自己的任务”。</p>
 */
@Mixin(PlayerMoodComponent.class)
public abstract class SpiritualistTaskBlockMixin {

    @Shadow
    @Final
    private PlayerEntity player;

    @Inject(method = "completeTask", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockSpiritualistTaskCompletion(
            PlayerMoodComponent.Task taskType,
            boolean rewardMood,
            CallbackInfo ci
    ) {
        if (SpiritualistPlayerComponent.KEY.get(this.player).isPossessing()) {
            ci.cancel();
            return;
        }

        if (SpiritualistHostComponent.KEY.get(this.player).possessed) {
            ci.cancel();
        }
    }
}
