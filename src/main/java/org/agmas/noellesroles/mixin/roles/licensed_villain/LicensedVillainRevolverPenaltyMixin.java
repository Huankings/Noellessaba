package org.agmas.noellesroles.mixin.roles.licensed_villain;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 执照恶棍使用左轮时不触发 Wathe 的“误伤好人惩罚”。
 */
@Mixin(GunShootPayload.Receiver.class)
public class LicensedVillainRevolverPenaltyMixin {
    @WrapOperation(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isInnocent(Lnet/minecraft/entity/player/PlayerEntity;)Z", ordinal = 0)
    )
    private boolean noellesroles$licensedVillainIgnoresRevolverPenalty(
            GameWorldComponent instance,
            PlayerEntity target,
            Operation<Boolean> original,
            GunShootPayload payload,
            ServerPlayNetworking.Context context
    ) {
        PlayerEntity shooter = context.player();
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(shooter.getWorld());
        if (gameWorld.isRole(shooter, NoellesRoleRegistry.LICENSED_VILLAIN)
                && GameFunctions.isPlayerAliveAndSurvival(shooter)) {
            /*
             * Wathe 的左轮惩罚入口先判断“目标是否 innocent”，然后同一分支里处理：
             * 1. 好人误伤反噬；
             * 2. 射中 innocent 后掉枪；
             * 3. 清空心情值。
             *
             * 如果只用 AllowPlayerDeath 拦反噬死亡，Wathe 局部 backfire 仍为 true，
             * 目标反而不会死亡；因此这里必须在判定源头把执照恶棍的目标视为“不会触发惩罚”。
             */
            return false;
        }
        return original.call(instance, target);
    }
}
