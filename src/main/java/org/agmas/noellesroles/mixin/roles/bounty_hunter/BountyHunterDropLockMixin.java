package org.agmas.noellesroles.mixin.roles.bounty_hunter;

import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 防止赏金模式期间把模式给予的德林加丢到地上。
 *
 * <p>模式结束只会清理带标记的那把德林加；如果允许玩家按 Q 丢出，就会把临时武器变成可拾取实体。
 * 因此这里只拦截“赏金模式给予的德林加”，普通赏金德林加不受影响。</p>
 */
@Mixin(ServerPlayerEntity.class)
public abstract class BountyHunterDropLockMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void noellesroles$preventBountyModeDerringerDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (BountyHunterPlayerComponent.KEY.get(player).isBountyModeActive()
                && BountyHunterPlayerComponent.isModeGrantedDerringer(player.getMainHandStack())) {
            cir.setReturnValue(false);
        }
    }
}
