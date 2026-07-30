package org.agmas.noellesroles.client.roles.robber;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/**
 * 让 Wathe 左轮的“准星锁定高亮”也能识别扩展枪械。
 *
 * <p>旧实现只 Wrap 了 Wathe 左轮判断；迁移到 CrosshairHudApi 后，
 * 这里仍然只处理强盗手枪和无声左轮，不去碰 Wathe 原本的匕首/短枪逻辑。</p>
 */
public final class RobberGunCrosshair {
    private RobberGunCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/robber/guns"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                RobberGunCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (!context.mainHandStack().isOf(ModItems.ROBBER_PISTOL)
                && !context.mainHandStack().isOf(ModItems.SILENCED_REVOLVER)) {
            return CrosshairHudApi.Result.PASS;
        }

        boolean target = !context.player().getItemCooldownManager().isCoolingDown(context.mainHandStack().getItem())
                && RevolverItem.getGunTarget(context.player()) instanceof EntityHitResult;
        CrosshairHudApi.renderStandardCrosshair(context, target);
        return CrosshairHudApi.Result.HANDLED;
    }
}
