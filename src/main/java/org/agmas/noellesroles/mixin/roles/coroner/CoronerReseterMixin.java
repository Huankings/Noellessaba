package org.agmas.noellesroles.mixin.roles.coroner;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class CoronerReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void coronerReset(ServerPlayerEntity player, CallbackInfo ci) {
        CoronerPlayerComponent.KEY.get(player).reset();
    }
}
