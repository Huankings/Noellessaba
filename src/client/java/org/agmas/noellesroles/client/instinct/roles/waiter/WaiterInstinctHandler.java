package org.agmas.noellesroles.client.instinct.roles.waiter;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.waiter.WaiterPlayerComponent;

/**
 * 服务员被动透视的客户端 instinct 接入。
 *
 * <p>这个高亮不需要按本能键：服务端在玩家完成心情任务时给目标玩家写入 WaiterPlayerComponent 倒计时，
 * 客户端这里只负责“观看者是服务员，并且目标还在可见时间内”时返回服务员职业色。</p>
 */
public final class WaiterInstinctHandler {
    private WaiterInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("waiter_served"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            // 只高亮存活玩家；旁观、尸体和非玩家实体全部放行给其他 instinct 规则。
            if (!(target instanceof PlayerEntity targetPlayer)
                    || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                    || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (gameWorld.isRole(viewer, Noellesroles.WAITER)
                    && WaiterPlayerComponent.KEY.get(targetPlayer).isVisibleToWaiters()) {
                // 颜色复用职业注册色，也就是需求中的 RGB(225, 170, 40)。
                return InstinctApi.HighlightResult.color(Noellesroles.WAITER.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
