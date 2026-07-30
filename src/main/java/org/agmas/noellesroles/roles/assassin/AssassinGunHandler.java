package org.agmas.noellesroles.roles.assassin;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.GunShotContext;
import dev.doctor4t.wathe.api.combat.GunShotResult;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.util.GunDropPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 无声左轮的服务端开火接管。
 *
 * <p>无声效果只是不调用 Wathe 的默认 click/shoot 声音；
 * 命中回放、枪口同步、击杀和冷却仍走 GunShotContext 的公共入口。</p>
 */
public final class AssassinGunHandler {
    private static boolean initialized = false;

    private AssassinGunHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerShotHandler(
                NoellesRolesCore.id("silenced_revolver"),
                GunShotApi.DEFAULT_PRIORITY,
                AssassinGunHandler::handleShot
        );
    }

    private static GunShotResult handleShot(GunShotContext context) {
        // 只接管刺客无声左轮；普通左轮仍回到 Wathe 默认逻辑。
        if (!context.stack().isOf(ModItems.SILENCED_REVOLVER)) {
            return GunShotResult.PASS;
        }
        if (context.isCoolingDown()) {
            return GunShotResult.HANDLED;
        }

        PlayerEntity target = context.alivePlayerTarget(65.0F, false);
        if (target != null) {
            /*
             * 无声左轮虽然不播放枪声，但仍应记录命中和走统一死亡入口。
             * 这样死亡保护、疯魔盾、赏金、时间狭缝和尸体回调都会照常触发。
             */
            context.recordGunHit(target);
            if (context.killTarget(target) && !context.isCreative()) {
                dropSilencedRevolverAfterInnocentKill(context.shooter(), target);
            }
        }

        context.sendMuzzle();
        if (!context.isCreative()) {
            context.applyCooldown(GameConstants.ITEM_COOLDOWNS.getOrDefault(ModItems.SILENCED_REVOLVER, 0));
        }
        return GunShotResult.HANDLED;
    }

    private static void dropSilencedRevolverAfterInnocentKill(ServerPlayerEntity player, PlayerEntity target) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(target.getWorld());
        if (!gameWorld.isInnocent(target)) {
            return;
        }

        /*
         * 刺客无声左轮的代价是：确认击杀无辜者后必掉自己的无声左轮。
         * 这里不走 Wathe 默认 GunShotApi 误伤惩罚，因为那段会播放普通枪声/掉普通左轮，
         * 和无声左轮的旧语义不一致。
         */
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
