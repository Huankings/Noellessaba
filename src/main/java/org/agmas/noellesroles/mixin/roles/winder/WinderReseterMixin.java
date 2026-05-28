package org.agmas.noellesroles.mixin.roles.winder;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.winder.WindMarkPlayerComponent;
import org.agmas.noellesroles.roles.winder.WinderPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 风灵师相关状态在 resetPlayer 时统一清理。
 *
 * <p>这样可以确保：
 * 1. 上一把的烙印不会残留到下一把；
 * 2. 上一把的选人状态与漂浮状态也不会带过去。
 */
@Mixin(GameFunctions.class)
public abstract class WinderReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void resetWinder(ServerPlayerEntity player, CallbackInfo ci) {
        WinderPlayerComponent.KEY.get(player).reset();
        WindMarkPlayerComponent.KEY.get(player).reset();
    }
}
