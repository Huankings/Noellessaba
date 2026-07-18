package org.agmas.noellesroles.mixin.roles.assassin;

import dev.doctor4t.wathe.cca.PlayerGrenadeComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.GrenadeThrowModePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让 Wathe 原版的“左键切手雷模式”链路也能识别 noellesroles 自己的扩展手雷。
 *
 * <p>目前需要接入的有：
 * 1. 无声手雷；
 * 2. 假手雷。</p>
 */
@Mixin(GrenadeThrowModePayload.Receiver.class)
public abstract class AssassinGrenadeThrowModeMixin {

    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/GrenadeThrowModePayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$allowSilentGrenadeModeSwitch(GrenadeThrowModePayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        boolean isHoldingSilentGrenade = context.player().getMainHandStack().isOf(ModItems.SILENT_GRENADE);
        boolean isHoldingFakeGrenade = context.player().getMainHandStack().isOf(ModItems.FAKE_GRENADE);
        if (!isHoldingSilentGrenade && !isHoldingFakeGrenade) {
            return;
        }

        /*
         * 这是 NoellesRoles 扩展手雷的服务端入口，必须和 Wathe 原版手雷的收包规则同步。
         * 非存活旁观玩家死亡后如果手里残留扩展手雷，客户端应当放行左键用于 spectator 附身；
         * 即使异常客户端仍然发来了切模式包，服务端也不允许它修改玩家的手雷投掷偏好。
         */
        if (!GameFunctions.isPlayerAliveAndSurvival(context.player())) {
            ci.cancel();
            return;
        }

        PlayerGrenadeComponent.KEY.get(context.player())
                .setThrowMode(PlayerGrenadeComponent.ThrowMode.fromDirectThrow(payload.directThrow()));
        ci.cancel();
    }
}
