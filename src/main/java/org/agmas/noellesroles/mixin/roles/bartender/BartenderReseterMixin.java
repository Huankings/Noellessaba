package org.agmas.noellesroles.mixin.roles.bartender;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.bartender.BartenderPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class BartenderReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void jesterWrite(ServerPlayerEntity player, CallbackInfo ci) {
        ((BartenderPlayerComponent)BartenderPlayerComponent.KEY.get(player)).reset();
    }
}
