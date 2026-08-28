package org.agmas.noellesroles.client.roles.lich;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.lich.LichConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 巫妖蓄力物品的准心进度提示。
 *
 * <p>简易法杖和魔法屏障都是“右键蓄力，松手释放”的物品。
 * 客户端只负责把蓄力/冷却进度画在准心下方；服务端物品类会再次检查蓄力时长、存活状态和调试冷却豁免。</p>
 */
public final class LichStaffCrosshair {
    private LichStaffCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/lich/charge_items"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                LichStaffCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (context.mainHandStack().isOf(ModItems.ONCE_STAFF)) {
            renderChargeItem(context, ModItems.ONCE_STAFF, LichConstants.ONCE_STAFF_MIN_CHARGE_TICKS);
            return CrosshairHudApi.Result.HANDLED;
        }
        if (context.mainHandStack().isOf(ModItems.MAGIC_BARRIER)) {
            renderChargeItem(context, ModItems.MAGIC_BARRIER, LichConstants.MAGIC_BARRIER_MIN_CHARGE_TICKS);
            return CrosshairHudApi.Result.HANDLED;
        }
        return CrosshairHudApi.Result.PASS;
    }

    private static void renderChargeItem(@NotNull CrosshairHudApi.Context context, @NotNull Item item, int requiredChargeTicks) {
        boolean debugPlayer = GameFunctions.isPlayerSpectatingOrCreative(context.player());
        ItemCooldownManager manager = context.player().getItemCooldownManager();
        float progress;
        boolean readyIcon;

        if (context.player().isUsingItem() && context.player().getActiveItem().isOf(item)) {
            progress = Math.min(LichConstants.HUD_PROGRESS_FULL, context.player().getItemUseTime() / (float) requiredChargeTicks);
            readyIcon = progress >= LichConstants.HUD_PROGRESS_FULL;
        } else {
            progress = debugPlayer
                    ? LichConstants.HUD_PROGRESS_FULL
                    : LichConstants.HUD_PROGRESS_FULL - manager.getCooldownProgress(item, context.tickDelta());
            readyIcon = false;
        }

        /*
         * 复用 Wathe 的疯魔/球棒进度小图标，视觉上和“法杖是攻击性魔法武器”的语义更接近。
         * 这里不高亮目标，因为骷髅和屏障都是扇形/范围释放，不依赖准心正中那一个实体。
         */
        CrosshairHudApi.renderBatProgressCrosshair(context, false, readyIcon, progress);
    }
}
