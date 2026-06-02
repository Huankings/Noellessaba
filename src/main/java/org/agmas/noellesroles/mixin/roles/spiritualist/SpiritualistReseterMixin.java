package org.agmas.noellesroles.mixin.roles.spiritualist;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistHostComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 灵术师相关组件在玩家重置时统一清理。
 */
@Mixin(GameFunctions.class)
public abstract class SpiritualistReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void noellesroles$resetSpiritualist(ServerPlayerEntity player, CallbackInfo ci) {
        SpiritualistPlayerComponent.KEY.get(player).reset();
        SpiritualistHostComponent.KEY.get(player).reset();
    }
}
