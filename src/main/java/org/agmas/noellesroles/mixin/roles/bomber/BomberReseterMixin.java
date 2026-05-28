package org.agmas.noellesroles.mixin.roles.bomber;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 回合结束重置玩家时，同步清空炸弹客相关的炸弹状态。
 */
@Mixin(GameFunctions.class)
public class BomberReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void resetBomber(ServerPlayerEntity player, CallbackInfo ci) {
        BomberPlayerComponent.KEY.get(player).reset();
    }
}
