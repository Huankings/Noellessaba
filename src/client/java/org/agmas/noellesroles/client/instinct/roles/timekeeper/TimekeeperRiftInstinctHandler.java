package org.agmas.noellesroles.client.instinct.roles.timekeeper;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;

/**
 * 时间狭缝期间关闭本能透视。
 *
 * <p>时间狭缝里的玩家会被服务端维持在“特殊存活旁观”：
 * 对局判定上仍可能被 30 秒回溯复活，但客户端不能因此拿到普通旁观者或职业本能信息。
 * 所以这里用最高优先级 availability 直接返回 DISABLE。</p>
 */
public final class TimekeeperRiftInstinctHandler {
    private TimekeeperRiftInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(
                NoellesInstinctHandlers.id("timekeeper_rift_suppression"),
                NoellesInstinctHandlers.PRIORITY_TIMEKEEPER_RIFT_SUPPRESSION,
                viewer -> TimekeeperPlayerComponent.KEY.get(viewer).isInTimeRift()
                        ? InstinctApi.AvailabilityResult.DISABLE
                        : InstinctApi.AvailabilityResult.PASS
        );
    }
}
