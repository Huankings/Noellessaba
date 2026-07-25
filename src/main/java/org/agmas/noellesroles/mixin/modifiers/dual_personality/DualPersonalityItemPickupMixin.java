package org.agmas.noellesroles.mixin.modifiers.dual_personality;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class DualPersonalityItemPickupMixin {

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockInnocentDoubleActiveRevolverPickup(PlayerEntity player, CallbackInfo ci) {
        /*
         * 左轮是实体拾取触发，不一定经过“使用物品”类回调。
         * 直接拦 ItemEntity#onPlayerCollision 可以覆盖地上捡枪、死亡掉落再捡回等情况。
         */
        if (player instanceof ServerPlayerEntity serverPlayer
                && DualPersonalityManager.shouldBlockRevolverPickup(serverPlayer, (ItemEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
