package org.agmas.noellesroles.client.roles.hunter;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
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
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity targetPlayer && GameFunctions.isPlayerAliveAndSurvival(targetPlayer),
                HunterConstants.HUNTING_KNIFE_TARGET_RANGE
        );
        /*
         * 与物品 use()/服务端命中保持一致：创造/旁观语义玩家调试猎刀时忽略冷却。
         * 否则即使右键不被拦截，准星仍会因为客户端冷却显示成不可命中状态。
         */
        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        boolean target = (ignoresCooldown || !manager.isCoolingDown(ModItems.HUNTING_KNIFE))
                && hitResult instanceof EntityHitResult;
        float progress = 1.0F - manager.getCooldownProgress(ModItems.HUNTING_KNIFE, context.tickDelta());
        CrosshairHudApi.renderKnifeProgressCrosshair(context, target, target, progress);
        return CrosshairHudApi.Result.HANDLED;
    }
}
