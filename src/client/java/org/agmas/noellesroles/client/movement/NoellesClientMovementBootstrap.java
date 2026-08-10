package org.agmas.noellesroles.client.movement;

import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityMovementHandler;

/**
 * NoellesRoles 客户端移动 API 接入入口。
 *
 * <p>只注册依赖客户端状态的移动规则；服务端和双方共享的规则都在 common bootstrap。</p>
 */
public final class NoellesClientMovementBootstrap {
    private NoellesClientMovementBootstrap() {
    }

    public static void init() {
        DualPersonalityMovementHandler.init();
    }
}
