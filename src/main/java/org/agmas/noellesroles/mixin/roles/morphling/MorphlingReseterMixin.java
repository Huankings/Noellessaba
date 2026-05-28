package org.agmas.noellesroles.mixin.roles.morphling;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.Noisemaker.NoisemakerPlayerComponent;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
import org.agmas.noellesroles.roles.executioner.ExecutionerPlayerComponent;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.roles.recaller.RecallerPlayerComponent;
import org.agmas.noellesroles.roles.voodoo.VoodooPlayerComponent;
import org.agmas.noellesroles.roles.vulture.VulturePlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class MorphlingReseterMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void jesterWrite(ServerPlayerEntity player, CallbackInfo ci) {
        MorphlingPlayerComponent.KEY.get(player).reset();
        VoodooPlayerComponent.KEY.get(player).reset();
        (RecallerPlayerComponent.KEY.get(player)).reset();
        (VulturePlayerComponent.KEY.get(player)).reset();
        (ExecutionerPlayerComponent.KEY.get(player)).reset();
        (NoisemakerPlayerComponent.KEY.get(player)).reset();
        (ControllerPlayerComponent.KEY.get(player)).reset();
        (ControlledPlayerComponent.KEY.get(player)).reset();
    }



}
