package org.agmas.noellesroles.client.mixin.roles.assassin;

import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.packet.item.BayonetKnockbackC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 刺刀左键客户端拦截。
 *
 * <p>这里不再完全依赖原版“攻击实体”这条流水线去触发玩家互殴，
 * 因为真实玩家和 carpet 假人在实测里表现出了不同结果。
 * 现在当本地玩家拿着刺刀左键命中活着的玩家时，会直接发自定义 C2S 包给服务端统一处理。</p>
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class AssassinBayonetAttackClientMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void noellesroles$handleBayonetKnockback(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (!player.getMainHandStack().isOf(ModItems.BAYONET)
                || !(target instanceof PlayerEntity targetPlayer)
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
            return;
        }

        /*
         * 左键命中玩家时直接走我们自己的击退包，
         * 彻底绕开原版 attack 对不同“玩家实体类型”的差异处理。
         */
        ClientPlayNetworking.send(new BayonetKnockbackC2SPacket(target.getId()));
        player.swingHand(Hand.MAIN_HAND, true);
        ci.cancel();
    }
}
