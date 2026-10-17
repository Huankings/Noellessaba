package org.agmas.noellesroles.client.input;

import dev.doctor4t.wathe.api.client.input.ClientInputApi;
import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperRewindInputLock;
import org.agmas.noellesroles.packet.item.BayonetKnockbackC2SPacket;
import org.agmas.noellesroles.packet.item.BloodAxeKnockbackC2SPacket;
import org.agmas.noellesroles.packet.item.ColorfulAxeAttackC2SPacket;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapConstants;

/** Noelles 自定义左键武器在 TARGET 隐藏、ATTACK 允许时的最终动作接管。 */
public final class NoellesWeaponActionRules {
    private static final int PRIORITY = 4000;

    private NoellesWeaponActionRules() {
    }

    public static void register() {
        ClientInputApi.registerActionRule(NoellesRolesCore.id("input/custom_weapon_hidden_target"), PRIORITY, context -> {
            if (context.action() != ClientInputApi.ClientAction.ATTACK) {
                return ClientInputApi.ActionResult.pass();
            }

            ClientPlayerEntity localPlayer = context.client().player;
            if (localPlayer == null || !GameFunctions.isPlayerAliveAndSurvival(localPlayer)
                    || SpiritualistClientController.isProjectionActive()
                    || SpiritualistClientController.isPossessionViewActive()
                    || TimekeeperRewindInputLock.isInputLocked(context.client())) {
                return ClientInputApi.ActionResult.pass();
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
                return ClientInputApi.ActionResult.pass();
            }

            EntityHitResult hitResult = WeaponTargetingApi.getAttackableAlivePlayerTarget(localPlayer, range);
            if (hitResult == null || !(hitResult.getEntity() instanceof PlayerEntity target)
                    || TargetVisibilityApi.canTargetPlayer(localPlayer, target)
                    || !isEntityHitBeforeBlockingBlock(localPlayer, hitResult, range)) {
                return ClientInputApi.ActionResult.pass();
            }

            /*
             * 这里只补发三种自定义左键包。服务端仍会重新验证物品、距离、存活与 ATTACK 权限；
             * 返回 handled(true) 会阻止原版 doAttack 重复处理，同时保留“本次点击已成功处理”的返回值。
             */
            if (stack.isOf(ModItems.BAYONET)) {
                ClientPlayNetworking.send(new BayonetKnockbackC2SPacket(target.getId()));
            } else if (stack.isOf(ModItems.BLOOD_AXE)) {
                ClientPlayNetworking.send(new BloodAxeKnockbackC2SPacket(target.getId()));
            } else {
                ClientPlayNetworking.send(new ColorfulAxeAttackC2SPacket(target.getId()));
            }
            localPlayer.swingHand(Hand.MAIN_HAND, true);
            return ClientInputApi.ActionResult.handled(true);
        });
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
        double entityDistanceSquared = player.getEyePos().squaredDistanceTo(entityHit.getPos());
        double blockDistanceSquared = player.getEyePos().squaredDistanceTo(blockHit.getPos());
        return entityDistanceSquared <= blockDistanceSquared + 1.0E-4D;
    }
}
