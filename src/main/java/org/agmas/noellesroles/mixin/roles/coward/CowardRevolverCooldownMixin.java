package org.agmas.noellesroles.mixin.roles.coward;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.coward.CowardConstants;
import org.agmas.noellesroles.roles.coward.SedativePlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GunShootPayload.Receiver.class)
public abstract class CowardRevolverCooldownMixin {

    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("TAIL")
    )
    private void noellesroles$adjustCowardRevolverCooldown(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        if (!player.getMainHandStack().isOf(WatheItems.REVOLVER) || player.isCreative()) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        boolean coward = gameWorld.isRole(player, Noellesroles.COWARD);
        boolean sedative = SedativePlayerComponent.KEY.get(player).isActive();
        if (!coward && !sedative) {
            return;
        }

        int cooldown = GameConstants.getRevolverCooldown(player);
        float factor = 1.0f;
        if (coward) {
            factor *= CowardConstants.REVOLVER_COOLDOWN_FACTOR;
        }
        if (sedative) {
            factor *= CowardConstants.SEDATIVE_REVOLVER_COOLDOWN_FACTOR;
        }

        int adjustedCooldown = Math.max(1, Math.round(cooldown * factor));
        player.getItemCooldownManager().set(WatheItems.REVOLVER, adjustedCooldown);
    }
}
