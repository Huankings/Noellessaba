package org.agmas.noellesroles.roles.bounty_hunter;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.GunShotContext;
import dev.doctor4t.wathe.api.combat.GunShotResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 赏金手枪和赏金德林加的服务端开火接管。
 *
 * <p>自定义枪械不需要加入 Wathe 的 {@code wathe:guns} 标签：
 * Wathe 会先询问 GunShotApi，因此本职业只处理自己的差异，
 * 通用枪声、枪口同步、命中回放和死亡入口仍复用 Wathe API 上下文。</p>
 */
public final class BountyHunterGunHandler {
    private static boolean initialized = false;

    private BountyHunterGunHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        GunShotApi.registerShotHandler(
                NoellesRolesCore.id("bounty_hunter_guns"),
                GunShotApi.DEFAULT_PRIORITY,
                BountyHunterGunHandler::handleShot
        );
    }

    private static GunShotResult handleShot(GunShotContext context) {
        ItemStack stack = context.stack();
        boolean bountyPistol = stack.isOf(ModItems.BOUNTY_PISTOL);
        boolean bountyDerringer = stack.isOf(ModItems.BOUNTY_DERRINGER);
        // 不是赏金猎人自己的两把枪时返回 PASS，让 Wathe 或其他扩展继续处理本次开火。
        if (!bountyPistol && !bountyDerringer) {
            return GunShotResult.PASS;
        }

        /*
         * 冷却中也返回 HANDLED。
         * 这表示“本处理器已经识别到自己的枪，但本次不能开火”，
         * 防止 Wathe 默认枪械逻辑继续把它当普通左轮处理。
         */
        if (context.isCoolingDown()) {
            return GunShotResult.HANDLED;
        }

        context.playDefaultClickSound();
        ShotOutcome outcome = tryShootTarget(
                context,
                bountyPistol
                        ? BountyHunterConstants.BOUNTY_PISTOL_RANGE_BLOCKS
                        : BountyHunterConstants.BOUNTY_DERRINGER_RANGE_BLOCKS
        );
        context.playDefaultShootSound();
        context.sendMuzzle();

        if (context.isCreative()) {
            return GunShotResult.HANDLED;
        }

        if (bountyPistol) {
            applyBountyPistolOutcome(context, outcome);
        } else {
            context.applyCooldown(BountyHunterConstants.BOUNTY_DERRINGER_COOLDOWN_TICKS);
        }
        return GunShotResult.HANDLED;
    }

    private static ShotOutcome tryShootTarget(GunShotContext context, float range) {
        /*
         * 赏金枪沿用旧实现的范围边界：目标距离 <= range 视为命中。
         * 这里用 alivePlayerTarget，护盾/免死导致未确认死亡时会由 killTarget 返回 false。
         */
        PlayerEntity target = context.alivePlayerTarget(range);
        if (target == null) {
            return ShotOutcome.missed();
        }

        BountyHunterPlayerComponent bountyHunter = BountyHunterPlayerComponent.KEY.get(context.shooter());
        boolean wasBountyTarget = bountyHunter.isCurrentBountyTarget(target);

        context.recordGunHit(target);
        boolean killed = context.killTarget(target);
        return new ShotOutcome(killed, wasBountyTarget);
    }

    private static void applyBountyPistolOutcome(GunShotContext context, ShotOutcome outcome) {
        ServerPlayerEntity player = context.shooter();
        BountyHunterPlayerComponent bountyHunter = BountyHunterPlayerComponent.KEY.get(player);
        if (outcome.killed() && outcome.wasBountyTarget()) {
            /*
             * 成功击杀当前悬赏目标使用短冷却，并且保留手枪。
             * 真正发放赏金金币不在这里做，而是在 BountyHunterDeathHandler 里监听任意击杀来源，
             * 避免只有赏金手枪击杀才有奖励。
             */
            context.applyCooldown(BountyHunterConstants.BOUNTY_PISTOL_TARGET_COOLDOWN_TICKS);
            return;
        }

        /*
         * 未命中、护盾挡住或击杀非悬赏目标都使用失败冷却。
         * 只有“确实击杀非悬赏目标”才吞掉赏金手枪，避免空枪也丢武器。
         */
        context.applyCooldown(BountyHunterConstants.BOUNTY_PISTOL_FAILED_COOLDOWN_TICKS);
        if (outcome.killed()) {
            player.getInventory().remove(itemStack -> itemStack.isOf(ModItems.BOUNTY_PISTOL), 1, player.getInventory());
            player.playerScreenHandler.sendContentUpdates();
        }
    }

    private record ShotOutcome(boolean killed, boolean wasBountyTarget) {
        private static ShotOutcome missed() {
            return new ShotOutcome(false, false);
        }
    }
}
