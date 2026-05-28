package org.agmas.noellesroles.mixin.roles.prophet;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.prophet.ProphetPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 回合结束时清理先知相关状态。
 *
 * <p>这里会一起清掉：
 * 1. 先知自己的水晶球标记；
 * 2. 任意玩家身上的“巫毒免伤”状态。
 */
@Mixin(GameFunctions.class)
public abstract class ProphetReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void prophetReset(ServerPlayerEntity player, CallbackInfo ci) {
        ProphetPlayerComponent.KEY.get(player).reset();
    }
}
