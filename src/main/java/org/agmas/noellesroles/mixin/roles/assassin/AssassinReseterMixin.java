package org.agmas.noellesroles.mixin.roles.assassin;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.assassin.AssassinPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 回合重置时清理刺客相关冷却。
 */
@Mixin(GameFunctions.class)
public abstract class AssassinReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void noellesroles$resetAssassin(ServerPlayerEntity player, CallbackInfo ci) {
        AssassinPlayerComponent.KEY.get(player).reset();
    }
}
