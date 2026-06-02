package org.agmas.noellesroles.mixin.roles.operator;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.operator.OperatorPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 接线员相关状态在 resetPlayer 时统一清理。
 */
@Mixin(GameFunctions.class)
public abstract class OperatorReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void resetOperator(ServerPlayerEntity player, CallbackInfo ci) {
        OperatorPlayerComponent.KEY.get(player).reset();
    }
}
