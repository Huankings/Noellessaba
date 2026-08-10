package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.api.collision.PlayerCollisionApi;
import dev.doctor4t.wathe.api.collision.PlayerCollisionMode;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 灵术师脱体本体的玩家碰撞 API 接入。
 */
public final class SpiritualistPlayerCollisionHandler {
    private static boolean initialized = false;

    private SpiritualistPlayerCollisionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PlayerCollisionApi.registerRule(
                NoellesRolesCore.id("collision/spiritualist_detached_body"),
                1000,
                context -> {
                    if (SpiritualistBodyRules.shouldIgnorePlayerBodyCollision(context.self(), context.other())) {
                        /*
                         * 出窍和附身时，灵术师真实本体都不能再作为玩家实体墙或原版推挤源。
                         * 这条规则返回 NO_COLLISION 后，Wathe 会同时处理 EntityView 移动碰撞 shape
                         * 和 Entity / LivingEntity 的原版推挤入口，扩展侧不再需要三处底层 mixin 兜底。
                         */
                        return PlayerCollisionMode.NO_COLLISION;
                    }
                    return PlayerCollisionMode.PASS;
                }
        );
    }
}
