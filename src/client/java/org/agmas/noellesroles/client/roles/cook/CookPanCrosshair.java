package org.agmas.noellesroles.client.roles.cook;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.cook.CookConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 平底锅蓄力准星。
 */
public final class CookPanCrosshair {
    private CookPanCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/cook/pan"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                CookPanCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (!context.mainHandStack().isOf(ModItems.PAN)) {
            return CrosshairHudApi.Result.PASS;
        }

        ClientPlayerEntity player = context.player();
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity targetPlayer && GameFunctions.isPlayerAliveAndSurvival(targetPlayer),
                CookConstants.PAN_TARGET_RANGE
        );
        boolean target = !player.getItemCooldownManager().isCoolingDown(ModItems.PAN)
                && hitResult instanceof EntityHitResult;

        float chargeProgress = 0.0F;
        if (player.isUsingItem() && player.getActiveItem().isOf(ModItems.PAN)) {
            int usedTicks = player.getItemUseTime();
            chargeProgress = Math.min(1.0F, usedTicks / (float) CookConstants.PAN_MIN_USE_TICKS);
        }

        CrosshairHudApi.renderKnifeProgressCrosshair(context, target, target && chargeProgress >= 1.0F, chargeProgress);
        return CrosshairHudApi.Result.HANDLED;
    }
}
