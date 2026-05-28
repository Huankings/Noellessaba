package org.agmas.noellesroles.mixin.roles.controller;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class ControllerReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void controllerReset(ServerPlayerEntity player, CallbackInfo ci) {
        // 这个Mixin已经被整合到MorphlingReseterMixin中
        // 保留这个文件是为了保持一致性，但实际逻辑在MorphlingReseterMixin中
    }
}
