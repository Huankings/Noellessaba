package org.agmas.noellesroles.mixin;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.death.DeathProcessComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class GameFunctionsDeathProcessMixin {

    @Inject(method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("RETURN"))
    private static void resetDeathProcessing(PlayerEntity victim, boolean spawnBody, PlayerEntity killer,
                                             Identifier deathReason, CallbackInfo ci) {
        DeathProcessComponent.KEY.get(victim).setProcessing(false);
    }
}