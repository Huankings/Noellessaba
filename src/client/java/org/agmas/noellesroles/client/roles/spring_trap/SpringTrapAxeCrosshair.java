package org.agmas.noellesroles.client.roles.spring_trap;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 弹簧陷阱斧类准心。
 */
public final class SpringTrapAxeCrosshair {
    private SpringTrapAxeCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/spring_trap_axes"),
                CrosshairHudApi.DEFAULT_PRIORITY + 50,
                SpringTrapAxeCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (context.mainHandStack().isOf(ModItems.BLOOD_AXE)) {
            boolean coolingDown = context.player().getItemCooldownManager().isCoolingDown(ModItems.BLOOD_AXE)
                    && !GameFunctions.isPlayerSpectatingOrCreative(context.player());
            HitResult hitResult = ProjectileUtil.getCollision(
                    context.player(),
                    entity -> entity instanceof PlayerEntity target
                            && GameFunctions.isPlayerAliveAndSurvival(target)
                            && TargetVisibilityApi.canTargetPlayer(context.player(), target),
                    SpringTrapConstants.BLOOD_AXE_TARGET_RANGE
            );
            boolean target = !coolingDown && hitResult instanceof EntityHitResult;
            float progress = context.player().isUsingItem() && context.player().getActiveItem().isOf(ModItems.BLOOD_AXE)
                    ? Math.min(1.0F, context.player().getItemUseTime() / (float) SpringTrapConstants.BLOOD_AXE_MIN_USE_TICKS)
                    : 1.0F - context.player().getItemCooldownManager().getCooldownProgress(ModItems.BLOOD_AXE, context.tickDelta());
            CrosshairHudApi.renderKnifeProgressCrosshair(context, target, target && progress >= 1.0F, progress);
            return CrosshairHudApi.Result.HANDLED;
        }

        if (context.mainHandStack().isOf(ModItems.COLORFUL_AXE)) {
            HitResult hitResult = ProjectileUtil.getCollision(
                    context.player(),
                    entity -> entity instanceof PlayerEntity target
                            && GameFunctions.isPlayerAliveAndSurvival(target)
                            && TargetVisibilityApi.canTargetPlayer(context.player(), target),
                    SpringTrapConstants.COLORFUL_AXE_TARGET_RANGE
            );
            boolean target = hitResult instanceof EntityHitResult;
            CrosshairHudApi.renderBatProgressCrosshair(context, target, target, 1.0F);
            return CrosshairHudApi.Result.HANDLED;
        }
        return CrosshairHudApi.Result.PASS;
    }
}
