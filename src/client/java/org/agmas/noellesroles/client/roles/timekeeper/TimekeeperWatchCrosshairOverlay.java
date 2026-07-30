package org.agmas.noellesroles.client.roles.timekeeper;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.TimekeeperWatchItem;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchMode;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 时停者怀表准心下方进度条。
 *
 * <p>需求明确写了“不渲染准心变化，只渲染下方指示条”。
 * 因此这里接入 CrosshairHudApi 的 overlay，不取消 Wathe 原准心，
 * 只在玩家手持怀表且正在蓄力或当前模式冷却中时，额外绘制一条短进度条。</p>
 */
public final class TimekeeperWatchCrosshairOverlay {
    private TimekeeperWatchCrosshairOverlay() {
    }

    public static void register() {
        CrosshairHudApi.registerOverlay(
                NoellesRolesCore.id("crosshair/timekeeper/watch_progress"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                TimekeeperWatchCrosshairOverlay::render
        );
    }

    private static void render(@NotNull CrosshairHudApi.Context context) {
        Float progress = getWatchProgress(context.player());
        if (progress == null) {
            return;
        }

        int width = TimekeeperConstants.WATCH_CROSSHAIR_BAR_WIDTH;
        int height = TimekeeperConstants.WATCH_CROSSHAIR_BAR_HEIGHT;
        int x = context.centerX() - width / 2;
        int y = context.centerY() + TimekeeperConstants.WATCH_CROSSHAIR_BAR_Y_OFFSET;
        int fillWidth = Math.max(0, Math.min(width, Math.round(width * progress)));

        DrawContext drawContext = context.drawContext();
        drawContext.fill(x, y, x + width, y + height, TimekeeperConstants.WATCH_CROSSHAIR_BAR_BACKGROUND_COLOR);
        if (fillWidth > 0) {
            drawContext.fill(x, y, x + fillWidth, y + height, TimekeeperConstants.WATCH_CROSSHAIR_BAR_FILL_COLOR);
        }
    }

    private static @Nullable Float getWatchProgress(@NotNull ClientPlayerEntity player) {
        if (!GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.TIMEKEEPER)) {
            return null;
        }

        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(ModItems.DYING_WATCH)) {
            return null;
        }

        TimekeeperWatchState state = TimekeeperWatchItem.getState(stack);
        if (state.isBroken()) {
            return null;
        }

        TimekeeperWatchMode mode = TimekeeperWatchItem.getMode(stack);
        TimekeeperPlayerComponent component = TimekeeperPlayerComponent.KEY.get(player);
        int cooldownTicks = component.getCooldownTicks(mode);
        if (cooldownTicks > 0) {
            /*
             * 冷却条表示“距离再次可用还差多少”：刚进入冷却时接近空，冷却结束时填满。
             * 冷却总长必须按当前怀表状态计算，精致怀表升级后的 40/120 秒会自然显示更快的进度。
             */
            return 1.0F - Math.min(1.0F, cooldownTicks / (float) getCooldownLimit(mode, state));
        }

        if (mode == TimekeeperWatchMode.REWIND
                && player.isUsingItem()
                && player.getActiveItem().isOf(ModItems.DYING_WATCH)) {
            /*
             * 蓄力条表示“已经按住多久”：达到 3 秒常量后服务端才会真正尝试发动回溯。
             * 这里只读客户端本地 use time，不额外发包，避免每 tick 同步造成网络噪音。
             */
            return Math.min(1.0F, player.getItemUseTime() / (float) TimekeeperConstants.REWIND_CHARGE_TICKS);
        }

        return null;
    }

    private static int getCooldownLimit(@NotNull TimekeeperWatchMode mode, @NotNull TimekeeperWatchState state) {
        return switch (mode) {
            case ITEM_ACCELERATE, ABILITY_ACCELERATE -> state.isElegant()
                    ? TimekeeperConstants.ELEGANT_ACCELERATE_COOLDOWN_TICKS
                    : TimekeeperConstants.NORMAL_ACCELERATE_COOLDOWN_TICKS;
            case REWIND -> state.isElegant()
                    ? TimekeeperConstants.ELEGANT_REWIND_COOLDOWN_TICKS
                    : TimekeeperConstants.NORMAL_REWIND_COOLDOWN_TICKS;
        };
    }
}
