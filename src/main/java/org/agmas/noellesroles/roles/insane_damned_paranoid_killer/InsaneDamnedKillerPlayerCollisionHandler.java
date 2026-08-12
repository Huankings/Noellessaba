package org.agmas.noellesroles.roles.insane_damned_paranoid_killer;

import dev.doctor4t.wathe.api.collision.PlayerCollisionApi;
import dev.doctor4t.wathe.api.collision.PlayerCollisionMode;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 亡语杀手尸体伪装的玩家碰撞规则。
 *
 * <p>尸体伪装中的玩家应像地上的尸体一样不参与玩家实体墙，也不触发原版轻微推挤。
 * 这里接入 Wathe 的 PlayerCollisionApi，让服务端移动裁剪和客户端预测共用结果，
 * 避免继续 mixin Entity#collidesWith / pushAwayFrom 这类底层方法。</p>
 */
public final class InsaneDamnedKillerPlayerCollisionHandler {
    private static boolean initialized = false;

    private InsaneDamnedKillerPlayerCollisionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PlayerCollisionApi.registerRule(
                NoellesRolesCore.id("collision/insane_damned_killer_corpse"),
                InsaneDamnedKillerConstants.CORPSE_COLLISION_PRIORITY,
                context -> {
                    /*
                     * 只要参与碰撞判断的任意一方处于尸体伪装，就完全取消这次玩家碰撞。
                     * PlayerCollisionApi.suppressesPush 会同时检查两个方向，所以返回 NO_COLLISION
                     * 既能去掉移动阻挡，也能去掉原版推挤速度。
                     */
                    if (InsaneDamnedKillerPlayerComponent.isActiveCorpseMode(context.self())
                            || InsaneDamnedKillerPlayerComponent.isActiveCorpseMode(context.other())) {
                        return PlayerCollisionMode.NO_COLLISION;
                    }
                    return PlayerCollisionMode.PASS;
                }
        );
    }
}
