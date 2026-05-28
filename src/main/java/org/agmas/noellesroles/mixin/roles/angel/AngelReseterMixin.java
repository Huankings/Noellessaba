package org.agmas.noellesroles.mixin.roles.angel;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 天使的守护 / 安抚状态在回合重置时统一清理。
 */
@Mixin(GameFunctions.class)
public abstract class AngelReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void resetAngel(ServerPlayerEntity player, CallbackInfo ci) {
        AngelPlayerComponent.KEY.get(player).reset();
    }
}
