package org.agmas.noellesroles.mixin.roles.bounty_hunter;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.GunShootPayload;
import dev.doctor4t.wathe.util.ShootMuzzleS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterConstants;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 赏金枪械的服务端开火结算。
 *
 * <p>客户端仍发送 Wathe 的 GunShootPayload，这里只在主手是赏金手枪/赏金德林加时接管。
 * 接管后必须在服务端重新校验射程和目标存活，不能相信客户端传来的实体 id。</p>
 */
@Mixin(GunShootPayload.Receiver.class)
public class BountyGunShootMixin {
    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$handleBountyGuns(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        ItemStack mainHandStack = player.getMainHandStack();
        boolean bountyPistol = mainHandStack.isOf(ModItems.BOUNTY_PISTOL);
        boolean bountyDerringer = mainHandStack.isOf(ModItems.BOUNTY_DERRINGER);
        if (!bountyPistol && !bountyDerringer) {
            return;
        }

        ci.cancel();

        Item gunItem = mainHandStack.getItem();
        if (player.getItemCooldownManager().isCoolingDown(gunItem)) {
            return;
        }

        playGunClick(player);

        ShotOutcome outcome = tryShootTarget(player, mainHandStack, payload, bountyPistol
                ? BountyHunterConstants.BOUNTY_PISTOL_RANGE_BLOCKS
                : BountyHunterConstants.BOUNTY_DERRINGER_RANGE_BLOCKS);

        playGunShot(player);
        sendMuzzle(player);

        if (player.isCreative()) {
            return;
        }

        if (bountyPistol) {
            applyBountyPistolOutcome(player, outcome);
        } else {
            player.getItemCooldownManager().set(ModItems.BOUNTY_DERRINGER, BountyHunterConstants.BOUNTY_DERRINGER_COOLDOWN_TICKS);
        }
    }

    @Unique
    private static ShotOutcome tryShootTarget(ServerPlayerEntity player, ItemStack mainHandStack, GunShootPayload payload, float range) {
        if (!(player.getServerWorld().getEntityById(payload.target()) instanceof PlayerEntity target)
                || !GameFunctions.isPlayerAliveAndSurvival(target)
                || target.distanceTo(player) > range) {
            return ShotOutcome.missed();
        }

        BountyHunterPlayerComponent bountyHunter = BountyHunterPlayerComponent.KEY.get(player);
        boolean wasBountyTarget = bountyHunter.isCurrentBountyTarget(target);
        boolean targetWasAlive = GameFunctions.isPlayerAliveAndSurvival(target);

        if (target instanceof ServerPlayerEntity serverTarget) {
            /*
             * 赏金枪绕过了 Wathe 原版 GunShootPayload 的默认处理，
             * 因此这里必须补一条 item hit 记录，回放才知道本次枪击来自哪把具体武器。
             */
            GameRecordManager.recordItemHit(
                    player,
                    mainHandStack,
                    GameConstants.DeathReasons.GUN,
                    serverTarget,
                    null
            );
        }

        GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.GUN);
        boolean killed = targetWasAlive && !GameFunctions.isPlayerAliveAndSurvival(target);
        return new ShotOutcome(killed, wasBountyTarget);
    }

    @Unique
    private static void applyBountyPistolOutcome(ServerPlayerEntity player, ShotOutcome outcome) {
        BountyHunterPlayerComponent bountyHunter = BountyHunterPlayerComponent.KEY.get(player);
        if (outcome.killed() && outcome.wasBountyTarget()) {
            bountyHunter.setBountyPistolCooldownTotalTicks(BountyHunterConstants.BOUNTY_PISTOL_TARGET_COOLDOWN_TICKS);
            player.getItemCooldownManager().set(ModItems.BOUNTY_PISTOL, BountyHunterConstants.BOUNTY_PISTOL_TARGET_COOLDOWN_TICKS);
            return;
        }

        /*
         * 未命中、被护盾挡住或击杀非悬赏目标都算失败冷却。
         * 只有“确实击杀了非悬赏目标”时才清掉一把赏金手枪，避免盾挡/空枪也吞武器。
         */
        bountyHunter.setBountyPistolCooldownTotalTicks(BountyHunterConstants.BOUNTY_PISTOL_FAILED_COOLDOWN_TICKS);
        player.getItemCooldownManager().set(ModItems.BOUNTY_PISTOL, BountyHunterConstants.BOUNTY_PISTOL_FAILED_COOLDOWN_TICKS);
        if (outcome.killed()) {
            player.getInventory().remove(stack -> stack.isOf(ModItems.BOUNTY_PISTOL), 1, player.getInventory());
            player.playerScreenHandler.sendContentUpdates();
        }
    }

    @Unique
    private static void playGunClick(ServerPlayerEntity player) {
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
    }

    @Unique
    private static void playGunShot(ServerPlayerEntity player) {
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
    }

    @Unique
    private static void sendMuzzle(ServerPlayerEntity player) {
        for (ServerPlayerEntity tracking : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(tracking, new ShootMuzzleS2CPayload(player.getUuidAsString()));
        }
        ServerPlayNetworking.send(player, new ShootMuzzleS2CPayload(player.getUuidAsString()));
    }

    @Unique
    private record ShotOutcome(boolean killed, boolean wasBountyTarget) {
        private static ShotOutcome missed() {
            return new ShotOutcome(false, false);
        }
    }
}
