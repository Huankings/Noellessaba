package org.agmas.noellesroles.mixin.roles.robber;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.GunShootPayload;
import dev.doctor4t.wathe.util.ShootMuzzleS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 强盗手枪的服务端开火结算。
 * 客户端仍然发送 Wathe 原版的 GunShootPayload，
 * 这里只在服务端检测到主手是强盗手枪时，完整接管原版左轮逻辑。
 */
@Mixin(GunShootPayload.Receiver.class)
public class RobberGunShootMixin {

    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$handleRobberPistol(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        ItemStack mainHandStack = player.getMainHandStack();
        if (!mainHandStack.isOf(ModItems.ROBBER_PISTOL)) {
            return;
        }

        ci.cancel();

        if (player.getItemCooldownManager().isCoolingDown(ModItems.ROBBER_PISTOL)) {
            return;
        }

        player.getWorld().playSound(
                null,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                WatheSounds.ITEM_REVOLVER_CLICK,
                SoundCategory.PLAYERS,
                0.5F,
                1.0F + player.getRandom().nextFloat() * 0.1F - 0.05F
        );

        if (player.getServerWorld().getEntityById(payload.target()) instanceof PlayerEntity target
                && target.distanceTo(player) < 65.0F) {
            /*
             * 强盗手枪会完全接管 Wathe 原版的枪击处理，
             * 因此这里也要补上 ITEM_HIT 记录，并显式标注这是“枪击类命中”，
             * 这样回放层才能复用枪击句式，同时显示真实物品名“强盗手枪”。
             */
            if (target instanceof ServerPlayerEntity serverTarget) {
                GameRecordManager.recordItemHit(
                        player,
                        mainHandStack,
                        GameConstants.DeathReasons.GUN,
                        serverTarget,
                        null
                );
            }
            GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.GUN);

            // 只有目标真正死亡时，才执行强盗手枪自己的掉枪概率。
            if (!GameFunctions.isPlayerAliveAndSurvival(target) && !player.isCreative()) {
                handlePostKillOutcome(player, target);
            }
        }

        player.getWorld().playSound(
                null,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                WatheSounds.ITEM_REVOLVER_SHOOT,
                SoundCategory.PLAYERS,
                5.0F,
                1.0F + player.getRandom().nextFloat() * 0.1F - 0.05F
        );

        for (ServerPlayerEntity tracking : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(tracking, new ShootMuzzleS2CPayload(player.getUuidAsString()));
        }
        ServerPlayNetworking.send(player, new ShootMuzzleS2CPayload(player.getUuidAsString()));

        if (!player.isCreative()) {
            player.getItemCooldownManager().set(
                    ModItems.ROBBER_PISTOL,
                    GameConstants.ITEM_COOLDOWNS.getOrDefault(ModItems.ROBBER_PISTOL, 0)
            );
        }
    }

    /**
     * 击杀成功后的概率结算：
     * 1. 如果击杀的不是平民阵营角色，则强盗手枪直接保留。
     * 2. 如果击杀的是平民阵营角色，才进入原本的概率分支：
     *    - 20% 保留强盗手枪。
     *    - 30% 移除强盗手枪并掉出一把 Wathe 原版左轮。
     *    - 50% 强盗手枪直接消失。
     */
    private static void handlePostKillOutcome(ServerPlayerEntity player, PlayerEntity target) {
        // 非平民阵营（例如杀手、中立、带特殊身份的非好人）被击杀时，
        // 强盗手枪固定保留，不触发任何掉枪或消失分支。
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(target.getWorld());
        if (!gameWorld.isInnocent(target)) {
            return;
        }

        int roll = player.getRandom().nextInt(100);
        if (roll < 20) {
            return;
        }

        removeOneRobberPistol(player);
        if (roll < 50) {
            ItemEntity droppedGun = player.dropItem(WatheItems.REVOLVER.getDefaultStack(), false, false);
            if (droppedGun != null) {
                droppedGun.setPickupDelay(10);
                droppedGun.setThrower(player);
            }
        }
    }

    /**
     * 从玩家背包里删掉一把强盗手枪，并立刻同步背包内容。
     */
    private static void removeOneRobberPistol(ServerPlayerEntity player) {
        player.getInventory().remove(stack -> stack.isOf(ModItems.ROBBER_PISTOL), 1, player.getInventory());
        player.playerScreenHandler.sendContentUpdates();
    }
}
