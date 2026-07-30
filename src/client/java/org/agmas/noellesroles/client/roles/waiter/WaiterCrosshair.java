package org.agmas.noellesroles.client.roles.waiter;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.waiter.WaiterConstants;
import org.agmas.noellesroles.roles.waiter.WaiterServiceItems;
import org.jetbrains.annotations.NotNull;

/**
 * 服务员物品的准星提示。
 *
 * <p>当服务员手持可服务物品时，直接把 Wathe 默认准星替换成自定义状态：
 * 没有玩家目标时显示普通图标，锁定到可服务玩家时显示 target 图标。
 * 这样玩家能在第一人称下直观看到“现在按右键会递给谁”。</p>
 */
public final class WaiterCrosshair {
    private WaiterCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/waiter/service_item"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                WaiterCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        ClientPlayerEntity player = context.player();
        // 只有存活服务员、且手里拿的是服务物品时才接管准星渲染。
        if (!WaiterServiceItems.isServiceStack(context.mainHandStack())
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.WAITER)) {
            return CrosshairHudApi.Result.PASS;
        }

        // 同一套原版攻击距离判定，只是拿来判断准星是否已经对准一个活着的玩家。
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity targetPlayer && GameFunctions.isPlayerAliveAndSurvival(targetPlayer),
                WaiterConstants.INTERACTION_RANGE
        );
        boolean target = hitResult instanceof EntityHitResult;

        // 复用 Wathe 的 HUD 准星资源，避免重新做一套风格不一致的图标。
        CrosshairHudApi.renderStandardCrosshair(context, target);
        return CrosshairHudApi.Result.HANDLED;
    }
}
