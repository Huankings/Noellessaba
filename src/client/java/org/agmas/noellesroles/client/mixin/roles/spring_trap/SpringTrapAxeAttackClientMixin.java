package org.agmas.noellesroles.client.mixin.roles.spring_trap;

import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.packet.item.BloodAxeKnockbackC2SPacket;
import org.agmas.noellesroles.packet.item.ColorfulAxeAttackC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 弹簧陷阱斧类武器左键客户端入口。
 *
 * <p>左键命中玩家时不走原版 attack 流程：
 * 血斧要无冷却击退，彩虹斧要绕过原版攻击速度直接击杀。
 * 客户端只发目标 id，服务端包会重新做权威校验。</p>
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class SpringTrapAxeAttackClientMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void noellesroles$handleSpringTrapAxeAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (!(target instanceof PlayerEntity targetPlayer)
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
            return;
        }

        if (player.getMainHandStack().isOf(ModItems.BLOOD_AXE)) {
            ClientPlayNetworking.send(new BloodAxeKnockbackC2SPacket(target.getId()));
            player.swingHand(Hand.MAIN_HAND, true);
            ci.cancel();
            return;
        }

        if (player.getMainHandStack().isOf(ModItems.COLORFUL_AXE)) {
            ClientPlayNetworking.send(new ColorfulAxeAttackC2SPacket(target.getId()));
            player.swingHand(Hand.MAIN_HAND, true);
            ci.cancel();
        }
    }
}
