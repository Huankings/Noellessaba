package org.agmas.noellesroles.mixin.roles.magician;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 匕首刺杀对魔术师播放体的兼容。
 */
@Mixin(value = KnifeStabPayload.Receiver.class, priority = 1100)
public abstract class MagicianKnifeStabMixin {

    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/KnifeStabPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$recordAndBreakPlayback(KnifeStabPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        if (!player.getMainHandStack().isOf(WatheItems.KNIFE)) {
            return;
        }

        MagicianServerHooks.recordKnifeStab(player);

        Entity target = player.getServerWorld().getEntityById(payload.target());
        if (MagicianServerHooks.stopPlaybackByWeaponTarget(
                target,
                player,
                GameConstants.DeathReasons.KNIFE,
                MagicianServerHooks.getWeaponName(player.getMainHandStack())
        )) {
            ci.cancel();
        }
    }
}
