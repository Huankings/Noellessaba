package org.agmas.noellesroles.client.roles.prophet;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.CrystalBallItem;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/**
 * 水晶球专用准心渲染。
 *
 * <p>这里单独做一个 handler，而不是把逻辑硬塞进其它准心扩展里，
 * 这样后续如果还要继续加“占卜类物品”的特殊准心，也更容易独立维护。</p>
 */
public final class ProphetCrystalBallCrosshair {
    private ProphetCrystalBallCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/prophet/crystal_ball"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                ProphetCrystalBallCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (!context.player().getMainHandStack().isOf(ModItems.CRYSTAL_BALL)
                && !context.player().getOffHandStack().isOf(ModItems.CRYSTAL_BALL)) {
            return CrosshairHudApi.Result.PASS;
        }

        HitResult hitResult = CrystalBallItem.getCrystalBallTarget(context.player());
        boolean target = hitResult instanceof EntityHitResult;
        float chargeProgress = CrystalBallItem.getChargeProgress(context.player());
        CrosshairHudApi.renderKnifeProgressCrosshair(context, target, chargeProgress >= 1.0F && target, chargeProgress);
        return CrosshairHudApi.Result.HANDLED;
    }
}
