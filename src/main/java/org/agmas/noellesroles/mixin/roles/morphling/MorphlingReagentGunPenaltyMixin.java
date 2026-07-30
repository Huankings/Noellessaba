package org.agmas.noellesroles.mixin.roles.morphling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.morphling.MorphlingReagentService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 变形试剂伪装中的左轮误伤惩罚豁免。
 *
 * <p>当前 Wathe 版本没有 SparkStrength66 使用的 ShouldPunishGunShooter 公开事件，
 * 左轮惩罚直接从 GunShootPayload 里读取“目标是否好人”。因此这里只包住那一个判断：
 * 受害者如果正被试剂伪装且真实身份是好人，就让这次射击不触发好人误伤惩罚。
 * 目标死亡、回放和枪击命中仍继续走 Wathe 原逻辑。</p>
 */
@Mixin(GunShootPayload.Receiver.class)
public class MorphlingReagentGunPenaltyMixin {
    @WrapOperation(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isInnocent(Lnet/minecraft/entity/player/PlayerEntity;)Z", ordinal = 0)
    )
    private boolean noellesroles$morphlingReagentCancelsInnocentPenalty(
            GameWorldComponent instance,
            PlayerEntity target,
            Operation<Boolean> original,
            GunShootPayload payload,
            ServerPlayNetworking.Context context
    ) {
        if (MorphlingReagentService.shouldCancelRevolverPenalty(target)) {
            return false;
        }
        return original.call(instance, target);
    }
}
