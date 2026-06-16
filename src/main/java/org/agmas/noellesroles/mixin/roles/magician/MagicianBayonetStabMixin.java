package org.agmas.noellesroles.mixin.roles.magician;

import dev.doctor4t.wathe.game.GameConstants;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.packet.item.BayonetStabC2SPacket;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 刺刀右键刺杀对魔术师播放体的兼容。
 */
@Mixin(value = BayonetStabC2SPacket.Receiver.class, priority = 1100)
public abstract class MagicianBayonetStabMixin {

    @Inject(method = "receive", at = @At("HEAD"), cancellable = true)
    private void noellesroles$recordAndBreakPlayback(BayonetStabC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        if (!player.getMainHandStack().isOf(ModItems.BAYONET)) {
            return;
        }

        MagicianServerHooks.recordBayonetStab(player);

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
