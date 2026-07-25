package org.agmas.noellesroles.mixin.roles.executioner;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.minecraft.entity.player.PlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityManager;
import org.agmas.noellesroles.roles.executioner.ExecutionerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(GunShootPayload.Receiver.class)
public class NoTargetBackfireMixin {
    @WrapOperation(method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V", at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isInnocent(Lnet/minecraft/entity/player/PlayerEntity;)Z", ordinal = 0))
    private boolean noBackfire(GameWorldComponent instance, PlayerEntity player, Operation<Boolean> original, GunShootPayload payload, ServerPlayNetworking.Context context) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        for (UUID uuid : gameWorldComponent.getAllWithRole(NoellesRoleRegistry.EXECUTIONER)) {
            PlayerEntity executioner = player.getWorld().getPlayerByUuid(uuid);
            if (executioner == null) continue;
            ExecutionerPlayerComponent executionerPlayerComponent = ExecutionerPlayerComponent.KEY.get(executioner);
            if (executionerPlayerComponent.target.equals(player.getUuid())) {
                return false;
            }
        }
        if (gameWorldComponent.isRole(player, NoellesRoleRegistry.VOODOO) && NoellesRolesConfig.HANDLER.instance().voodooShotLikeEvil) {
            return false;
        }
        boolean targetNormallyInnocent = original.call(instance, player);
        /*
         * 双重人格双活阶段是该词条的最终杀戮窗口。
         * 如果一个好人射击“双活中的好人双重人格”，不应该被 Wathe 当作普通好人误伤好人来反噬。
         */
        if (DualPersonalityManager.shouldSuppressInnocentRevolverPenalty(context.player(), player, targetNormallyInnocent)) {
            return false;
        }
        return targetNormallyInnocent;
    }
}
