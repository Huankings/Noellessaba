package org.agmas.noellesroles.client.mixin.visibility;

import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.packet.item.BayonetKnockbackC2SPacket;
import org.agmas.noellesroles.packet.item.BloodAxeKnockbackC2SPacket;
import org.agmas.noellesroles.packet.item.ColorfulAxeAttackC2SPacket;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperRewindInputLock;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapConstants;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 处理 NoellesRoles 自定义左键武器遇到“只拒绝 TARGET”的伪装玩家时无法发包的问题。
 *
 * <p>原版 {@code MinecraftClient#doAttack()} 依赖 {@code crosshairTarget}。
 * 亡语杀手尸体伪装必须让这个准心目标保持为空，避免准心图标和名字变化暴露身份；
 * 但刺刀左键击退、血斧左键击退和彩虹斧左键击杀仍然应该能够命中。
 *
 * <p>因此这里不改 {@code crosshairTarget}，只在真实攻击射线找到“ATTACK 允许、TARGET 拒绝”
 * 的玩家时，直接发送对应的服务端 C2S 包。服务端包仍会再次验证手持物、距离、存活和 ATTACK
 * 判定，这个客户端 mixin 不能单独构成玩法权限。</p>
 */
@Mixin(MinecraftClient.class)
public abstract class TargetVisibilityWeaponAttackClientMixin {
    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void noellesroles$attackTargetVisibilityHiddenWeaponTarget(CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity localPlayer = this.player;
        if (localPlayer == null || !GameFunctions.isPlayerAliveAndSurvival(localPlayer)) {
            return;
        }
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (SpiritualistClientController.isProjectionActive()
                || SpiritualistClientController.isPossessionViewActive()
                || TimekeeperRewindInputLock.isInputLocked(client)) {
            /*
             * 灵术师脱体 / 附身和时停者回溯都有自己的输入拦截。
             * 这里主动避让，避免隐藏目标补包在这些特殊视角里抢先发出真实攻击。
             */
            return;
        }

        ItemStack stack = localPlayer.getMainHandStack();
        double range;
        if (stack.isOf(ModItems.BAYONET)) {
            range = 3.0D;
        } else if (stack.isOf(ModItems.BLOOD_AXE)) {
            range = SpringTrapConstants.BLOOD_AXE_TARGET_RANGE;
        } else if (stack.isOf(ModItems.COLORFUL_AXE)) {
            range = SpringTrapConstants.COLORFUL_AXE_TARGET_RANGE;
        } else {
            return;
        }

        EntityHitResult hitResult = WeaponTargetingApi.getAttackableAlivePlayerTarget(localPlayer, range);
        if (hitResult == null || !(hitResult.getEntity() instanceof PlayerEntity target)
                || TargetVisibilityApi.canTargetPlayer(localPlayer, target)
                || !isEntityHitBeforeBlockingBlock(localPlayer, hitResult, range)) {
            return;
        }

        /*
         * 只有三种自定义左键包需要在这里补发。
         * 右键刺刀、血斧蓄力和其它武器已经有各自独立的 ATTACK 目标入口，
         * 不应因为“左键 fallback”而重复触发。
         */
        if (stack.isOf(ModItems.BAYONET)) {
            ClientPlayNetworking.send(new BayonetKnockbackC2SPacket(target.getId()));
        } else if (stack.isOf(ModItems.BLOOD_AXE)) {
            ClientPlayNetworking.send(new BloodAxeKnockbackC2SPacket(target.getId()));
        } else {
            ClientPlayNetworking.send(new ColorfulAxeAttackC2SPacket(target.getId()));
        }

        localPlayer.swingHand(Hand.MAIN_HAND, true);
        cir.setReturnValue(true);
    }

    private static boolean isEntityHitBeforeBlockingBlock(
            ClientPlayerEntity player,
            EntityHitResult entityHit,
            double range
    ) {
        HitResult blockHit = player.raycast(range, 1.0F, false);
        if (blockHit == null || blockHit.getType() == HitResult.Type.MISS) {
            return true;
        }

        /*
         * 不能因为绕过 crosshairTarget 就顺便允许穿墙攻击。
         * 这里沿用原版“实体和方块谁更近谁优先”的规则，保留墙体、门和地板阻挡。
         */
        double entityDistanceSquared = player.getEyePos().squaredDistanceTo(entityHit.getPos());
        double blockDistanceSquared = player.getEyePos().squaredDistanceTo(blockHit.getPos());
        return entityDistanceSquared <= blockDistanceSquared + 1.0E-4D;
    }
}
