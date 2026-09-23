package org.agmas.noellesroles.client.roles.myers;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.myers.MyersConstants;

/** 屠刀沿用 Wathe 匕首准心图标和蓄力进度。 */
public final class MyersButcherKnifeCrosshair {
    private MyersButcherKnifeCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(NoellesRolesCore.id("crosshair/myers_butcher_knife"),
                CrosshairHudApi.DEFAULT_PRIORITY + 60, MyersButcherKnifeCrosshair::render);
    }

    private static CrosshairHudApi.Result render(CrosshairHudApi.Context context) {
        if (!context.mainHandStack().isOf(ModItems.BUTCHER_KNIFE)) return CrosshairHudApi.Result.PASS;
        boolean cooling = context.player().getItemCooldownManager().isCoolingDown(ModItems.BUTCHER_KNIFE)
                && !GameFunctions.isPlayerSpectatingOrCreative(context.player());
        HitResult hit = ProjectileUtil.getCollision(context.player(),
                entity -> entity instanceof PlayerEntity target
                        && GameFunctions.isPlayerAliveAndSurvival(target)
                        && TargetVisibilityApi.canTargetPlayer(context.player(), target),
                MyersConstants.BUTCHER_KNIFE_TARGET_RANGE);
        boolean target = hit instanceof EntityHitResult;
        float progress = context.player().isUsingItem() && context.player().getActiveItem().isOf(ModItems.BUTCHER_KNIFE)
                ? Math.min(1.0F, context.player().getItemUseTime() / (float) MyersConstants.BUTCHER_KNIFE_MIN_CHARGE_TICKS)
                : 1.0F - context.player().getItemCooldownManager().getCooldownProgress(ModItems.BUTCHER_KNIFE, context.tickDelta());
        CrosshairHudApi.renderKnifeProgressCrosshair(context, target && !cooling, target && !cooling && progress >= 1.0F, progress);
        return CrosshairHudApi.Result.HANDLED;
    }
}
