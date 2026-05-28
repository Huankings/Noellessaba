package org.agmas.noellesroles.mixin.roles.stalker;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.stalker.StalkerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public class StalkerReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void resetStalker(ServerPlayerEntity player, CallbackInfo ci) {
        StalkerPlayerComponent.KEY.get(player).reset();
    }
}