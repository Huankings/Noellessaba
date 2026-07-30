package org.agmas.noellesroles.roles.robber;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.GunShotContext;
import dev.doctor4t.wathe.api.combat.GunShotResult;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 强盗手枪的服务端开火接管。
 *
 * <p>它替代旧的 RobberGunShootMixin：
 * 只识别强盗手枪，完整负责声音、命中回放、击杀、枪口同步、冷却和击杀后的概率掉枪。
 * 由于返回 HANDLED，Wathe 默认左轮逻辑不会再对这把枪追加反火或默认掉枪。</p>
 */
public final class RobberGunHandler {
    private static boolean initialized = false;

    private RobberGunHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerShotHandler(
                NoellesRolesCore.id("robber_pistol"),
                GunShotApi.DEFAULT_PRIORITY,
                RobberGunHandler::handleShot
        );
    }

    private static GunShotResult handleShot(GunShotContext context) {
        // 非强盗手枪必须 PASS，避免抢走 Wathe 原版左轮或其他扩展枪械。
        if (!context.stack().isOf(ModItems.ROBBER_PISTOL)) {
            return GunShotResult.PASS;
        }
        if (context.isCoolingDown()) {
            return GunShotResult.HANDLED;
        }

        context.playDefaultClickSound();
        /*
         * 强盗枪保持旧版严格小于 65 格的命中范围。
         * alivePlayerTarget 同时要求目标仍处于 Wathe 玩法存活状态。
         */
        PlayerEntity target = context.alivePlayerTarget(65.0F, false);
        if (target != null) {
            context.recordGunHit(target);
            if (context.killTarget(target) && !context.isCreative()) {
                handlePostKillOutcome(context.shooter(), target);
            }
        }
        context.playDefaultShootSound();
        context.sendMuzzle();
        if (!context.isCreative()) {
            context.applyCooldown(GameConstants.ITEM_COOLDOWNS.getOrDefault(ModItems.ROBBER_PISTOL, 0));
        }
        return GunShotResult.HANDLED;
    }

    /**
     * 只有杀死无辜阵营目标时才进行保枪 / 掉普通左轮 / 消失的概率结算。
     */
    private static void handlePostKillOutcome(ServerPlayerEntity player, PlayerEntity target) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(target.getWorld());
        if (!gameWorld.isInnocent(target)) {
            return;
        }

        int roll = player.getRandom().nextInt(100);
        if (roll < 20) {
            // 20%：保留强盗手枪。
            return;
        }

        player.getInventory().remove(stack -> stack.isOf(ModItems.ROBBER_PISTOL), 1, player.getInventory());
        player.playerScreenHandler.sendContentUpdates();
        if (roll < 50) {
            // 30%：强盗手枪损失，但掉出一把 Wathe 普通左轮。
            ItemEntity droppedGun = player.dropItem(WatheItems.REVOLVER.getDefaultStack(), false, false);
            if (droppedGun != null) {
                droppedGun.setPickupDelay(10);
                droppedGun.setThrower(player);
            }
        }
        // 50%：强盗手枪直接消失，不额外掉出普通左轮。
    }
}
