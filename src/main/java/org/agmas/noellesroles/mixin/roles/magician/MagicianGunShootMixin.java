package org.agmas.noellesroles.mixin.roles.magician;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让所有枪械都能：
 * 1. 被魔术师录制成一次明确的“枪击动作”；
 * 2. 命中皮套时直接强制结束播放。
 */
@Mixin(value = GunShootPayload.Receiver.class, priority = 1100)
public abstract class MagicianGunShootMixin {

    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$recordAndBreakPlayback(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        ItemStack stack = player.getMainHandStack();
        boolean isSupportedGun = stack.isIn(WatheItemTags.GUNS)
                || stack.isOf(ModItems.ROBBER_PISTOL)
                || stack.isOf(ModItems.SILENCED_REVOLVER);
        if (!isSupportedGun) {
            return;
        }

        if (!player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            MagicianServerHooks.recordGunShoot(player);
        }

        if (payload.target() < 0) {
            return;
        }

        Entity target = player.getServerWorld().getEntityById(payload.target());
        if (MagicianServerHooks.stopPlaybackByWeaponTarget(
                target,
                player,
                GameConstants.DeathReasons.GUN,
                MagicianServerHooks.getWeaponName(stack)
        )) {
            ci.cancel();
        }
    }
}
