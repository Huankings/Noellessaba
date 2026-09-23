package org.agmas.noellesroles.roles.myers;

import dev.doctor4t.wathe.api.collision.PlayerCollisionApi;
import dev.doctor4t.wathe.api.collision.PlayerCollisionMode;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 迈尔斯屠刀冲刺期间的玩家碰撞规则。
 *
 * <p>Wathe 的默认 SOLID 会把存活玩家作为实体墙，原版 move 因此可能先把冲刺位移裁成零，
 * 让冲刺逻辑误以为撞到了方块。冲刺期间这里临时取消双方玩家的物理碰撞，
 * 玩家命中完全由 MyersPlayerComponent 的服务端路径检测处理。</p>
 */
public final class MyersPlayerCollisionHandler {
    private static boolean initialized;

    private MyersPlayerCollisionHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        PlayerCollisionApi.registerRule(
                NoellesRolesCore.id("collision/myers_butcher_dash"),
                MyersConstants.DASH_SPEED_PRIORITY + 100,
                context -> MyersPlayerComponent.KEY.get(context.self()).isDashing()
                        || MyersPlayerComponent.KEY.get(context.other()).isDashing()
                        ? PlayerCollisionMode.NO_COLLISION
                        : PlayerCollisionMode.PASS
        );
    }
}
