package org.agmas.noellesroles.mixin.roles.assassin;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.GunDropPayload;
import dev.doctor4t.wathe.util.GunShootPayload;
import dev.doctor4t.wathe.util.ShootMuzzleS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 无声左轮的服务端开火结算。
 *
 * <p>客户端仍发送 Wathe 原版 GunShootPayload，
 * 这里只在主手是无声左轮时完整接管原版逻辑：</p>
 * <p>1. 保留命中、击杀、枪口粒子与后坐体验；</p>
 * <p>2. 去掉左轮 click / shoot 声音；</p>
 * <p>3. 若真正击杀了无辜者，则让无声左轮掉落。</p>
 */
@Mixin(GunShootPayload.Receiver.class)
public abstract class SilencedRevolverGunShootMixin {

    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$handleSilencedRevolver(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        ItemStack mainHandStack = player.getMainHandStack();
        if (!mainHandStack.isOf(ModItems.SILENCED_REVOLVER)) {
            return;
        }

        ci.cancel();

        if (player.getItemCooldownManager().isCoolingDown(ModItems.SILENCED_REVOLVER)) {
            return;
        }

        if (player.getServerWorld().getEntityById(payload.target()) instanceof PlayerEntity target
                && target.distanceTo(player) < 65.0F) {
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

            /*
             * 需求明确写的是“若击杀无辜者则会掉落”，
             * 因此这里只在目标真的死亡且目标属于无辜阵营时才触发掉枪。
             */
            if (!GameFunctions.isPlayerAliveAndSurvival(target) && !player.isCreative()) {
                dropSilencedRevolverAfterInnocentKill(player, target);
            }
        }

        for (ServerPlayerEntity tracking : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(tracking, new ShootMuzzleS2CPayload(player.getUuidAsString()));
        }
        ServerPlayNetworking.send(player, new ShootMuzzleS2CPayload(player.getUuidAsString()));

        if (!player.isCreative()) {
            player.getItemCooldownManager().set(
                    ModItems.SILENCED_REVOLVER,
                    GameConstants.ITEM_COOLDOWNS.getOrDefault(ModItems.SILENCED_REVOLVER, 0)
            );
        }
    }

    private static void dropSilencedRevolverAfterInnocentKill(ServerPlayerEntity player, PlayerEntity target) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(target.getWorld());
        if (!gameWorld.isInnocent(target)) {
            return;
        }

        player.getInventory().remove(stack -> stack.isOf(ModItems.SILENCED_REVOLVER), 1, player.getInventory());
        player.playerScreenHandler.sendContentUpdates();

        ItemEntity droppedGun = player.dropItem(ModItems.SILENCED_REVOLVER.getDefaultStack(), false, false);
        if (droppedGun != null) {
            droppedGun.setPickupDelay(10);
            droppedGun.setThrower(player);
        }
        ServerPlayNetworking.send(player, new GunDropPayload());
    }
}
