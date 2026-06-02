package org.agmas.noellesroles.mixin.roles.assassin;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.assassin.BayonetKnockbackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 刺刀普通左键只保留击退，不造成普通挥击伤害。
 */
@Mixin(PlayerEntity.class)
public abstract class AssassinBayonetAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void noellesroles$bayonetOnlyKnockback(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!BayonetKnockbackHandler.canKnockback(self, target) || !(target instanceof PlayerEntity targetPlayer)) {
            return;
        }

        /*
         * 这里继续保留服务端兜底：
         * 如果有别的系统直接调用了 player.attack(...)，刺刀也仍然只会击退而不会造成普通伤害。
         */
        BayonetKnockbackHandler.applyKnockback(self, targetPlayer);
        self.swingHand(Hand.MAIN_HAND, true);
        ci.cancel();
    }
}
