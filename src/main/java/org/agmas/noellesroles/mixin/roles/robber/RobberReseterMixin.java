package org.agmas.noellesroles.mixin.roles.robber;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.robber.RobberPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 回合结束重置玩家时，同时把强盗的开局冷却状态清干净。
 */
@Mixin(GameFunctions.class)
public class RobberReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void resetRobber(ServerPlayerEntity player, CallbackInfo ci) {
        RobberPlayerComponent.KEY.get(player).reset();
    }
}
