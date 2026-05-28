package org.agmas.noellesroles.mixin.roles.stalker;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.stalker.StalkerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public class StalkerKillCountMixin {

    @Inject(method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V", at = @At("HEAD"))
    private static void onKillPlayerForStalker(PlayerEntity victim, boolean spawnBody, PlayerEntity killer, Identifier deathReason, CallbackInfo ci) {
        if (killer == null) return;
        if (!deathReason.equals(GameConstants.DeathReasons.KNIFE)) return;

        var gameWorld = dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(killer.getWorld());
        if (!gameWorld.isRole(killer, Noellesroles.STALKER)) return;

        StalkerPlayerComponent comp = StalkerPlayerComponent.KEY.get(killer);
        if (comp.isActiveStalker() && comp.phase >= 2) {
            comp.addKill();
        }
    }
}