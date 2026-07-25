package org.agmas.noellesroles.client.modifiers.dual_personality;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.TimeRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 双重人格客户端状态工具。
 *
 * <p>客户端 HUD、皮肤、名字和输入拦截都会读取世界组件。
 * 这里统一使用 GameStatus.ACTIVE，而不是 Wathe 的 isRunning()，
 * 因为 isRunning() 在 STOPPING 结算阶段也会返回 true；如果继续按 isRunning 判定，
 * 双活倒计时和副人格外观就会残留到游戏结束画面。</p>
 */
public final class DualPersonalityClientState {

    private DualPersonalityClientState() {
    }

    public static boolean isActiveRound(@Nullable PlayerEntity player) {
        return player != null
                && player.getWorld() != null
                && isActiveRound(player.getWorld());
    }

    public static boolean isActiveRound(@Nullable World world) {
        return world != null
                && GameWorldComponent.KEY.get(world).getGameStatus() == GameWorldComponent.GameStatus.ACTIVE;
    }

    public static boolean hasRoundRenderState(@Nullable World world) {
        if (world == null) {
            return false;
        }

        GameWorldComponent.GameStatus status = GameWorldComponent.KEY.get(world).getGameStatus();
        /*
         * 皮肤/准星名字属于“本局身份展示”，不等同于技能和倒计时。
         * ACTIVE 时当然要显示；STOPPING 结算黑幕期间也要继续显示，
         * 等 Wathe finalizeGame 把玩家传回准备大厅并清组件后再消失。
         */
        return status == GameWorldComponent.GameStatus.ACTIVE
                || status == GameWorldComponent.GameStatus.STOPPING;
    }

    public static boolean isDormant(@Nullable PlayerEntity player) {
        return isActiveRound(player)
                && DualPersonalityComponent.KEY.get(player.getWorld()).isDormant(player.getUuid());
    }

    public static boolean isDoubleActive(@Nullable PlayerEntity player) {
        return isActiveRound(player)
                && DualPersonalityComponent.KEY.get(player.getWorld()).isDoubleActive(player.getUuid());
    }

    public static int getDoubleActiveTicks(@Nullable PlayerEntity player) {
        return isDoubleActive(player)
                ? DualPersonalityComponent.KEY.get(player.getWorld()).getDoubleActiveTicks(player.getUuid())
                : 0;
    }

    public static @Nullable UUID resolveSubAppearanceSource(@Nullable PlayerEntity player) {
        if (player == null) {
            return null;
        }

        return resolveSubAppearanceSource(player.getWorld(), player.getUuid());
    }

    public static @Nullable UUID resolveSubAppearanceSource(@Nullable World world, @Nullable UUID playerUuid) {
        if (!hasRoundRenderState(world) || playerUuid == null) {
            return null;
        }

        DualPersonalityComponent.PairState pair = DualPersonalityComponent.KEY.get(world).getPair(playerUuid);
        if (pair == null || !pair.isSub(playerUuid)) {
            return null;
        }
        return pair.main;
    }

    public static boolean isDormantBlockedKey(MinecraftClient client, int key, int scancode) {
        if (client == null || client.options == null) {
            return false;
        }

        /*
         * 休眠人格处于旁观模式时，Shift 会尝试脱离当前附身相机，
         * 数字键会尝试打开/选择旁观目标。这里按玩家实际键位配置判断，
         * 所以即使玩家改过蹲键或快捷栏键，也能一起拦住。
         */
        if (client.options.sneakKey.matchesKey(key, scancode)) {
            return true;
        }
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (hotbarKey.matchesKey(key, scancode)) {
                return true;
            }
        }
        return false;
    }

    public static void releaseDormantBlockedKeys(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }

        /*
         * 玩家可能在变成休眠人格之前就已经按住 Shift。
         * 单纯拦截后续 keyPress 不会释放这个“已按下”状态，所以每 tick 主动清掉。
         */
        client.options.sneakKey.setPressed(false);
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            hotbarKey.setPressed(false);
        }
    }

    public static void resetTransientRenderState() {
        /*
         * 双活倒计时现在通过 Wathe 的 TimeHudApi 接管顶部时间。
         * 停局/结算边界仍需要清理 TimeRenderer 的滚动数字状态，
         * 但这里不再直接碰 view/offsetDelta，而是调用 Wathe 暴露的重置入口。
         */
        TimeRenderer.resetTransientState();
    }
}
