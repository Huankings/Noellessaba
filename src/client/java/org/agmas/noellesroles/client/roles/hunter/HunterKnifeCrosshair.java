package org.agmas.noellesroles.client.roles.hunter;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.hunter.HunterConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 猎刀专用准星。
 *
 * <p>Wathe 原版准星只识别自己的匕首；猎刀需要单独补同款锁定/冷却显示。</p>
 */
public final class HunterKnifeCrosshair {
    private HunterKnifeCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/hunter/hunting_knife"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                HunterKnifeCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (!context.mainHandStack().isOf(ModItems.HUNTING_KNIFE)) {
            return CrosshairHudApi.Result.PASS;
        }

        ClientPlayerEntity player = context.player();
        ItemCooldownManager manager = player.getItemCooldownManager();
        /*
         * VISIBLE 决定准心能否暴露目标，ATTACK 决定松手时是否真的允许发送命中。
         * 只有两条语义命中同一实体才点亮准心，避免目标可见但处于不可攻击状态时
         * 给玩家错误的“已经锁定”反馈。
         */
        HitResult visibleTarget = WeaponTargetingApi.getVisibleAlivePlayerTarget(
                player,
                HunterConstants.HUNTING_KNIFE_TARGET_RANGE
        );
        HitResult attackTarget = WeaponTargetingApi.getAttackableAlivePlayerTarget(
                player,
                HunterConstants.HUNTING_KNIFE_TARGET_RANGE
        );
        /*
         * 与物品 use()/服务端命中保持一致：创造/旁观语义玩家调试猎刀时忽略冷却。
         * 否则即使右键不被拦截，准星仍会因为客户端冷却显示成不可命中状态。
         */
        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        boolean target = (ignoresCooldown || !manager.isCoolingDown(ModItems.HUNTING_KNIFE))
                && WeaponTargetingApi.isSameEntityTarget(visibleTarget, attackTarget);
        float progress = 1.0F - manager.getCooldownProgress(ModItems.HUNTING_KNIFE, context.tickDelta());
        CrosshairHudApi.renderKnifeProgressCrosshair(context, target, target, progress);
        return CrosshairHudApi.Result.HANDLED;
    }
}
