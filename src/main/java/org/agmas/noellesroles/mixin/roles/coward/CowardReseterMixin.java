package org.agmas.noellesroles.mixin.roles.coward;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.coward.CowardPlayerComponent;
import org.agmas.noellesroles.roles.coward.SedativePlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class CowardReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void noellesroles$resetCoward(ServerPlayerEntity player, CallbackInfo ci) {
        CowardPlayerComponent.KEY.get(player).reset();
        SedativePlayerComponent.KEY.get(player).reset();
    }
}
