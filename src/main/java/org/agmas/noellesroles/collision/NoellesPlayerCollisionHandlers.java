package org.agmas.noellesroles.collision;

import org.agmas.noellesroles.modifiers.feather.FeatherPlayerCollisionHandler;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerPlayerCollisionHandler;
import org.agmas.noellesroles.roles.jason.JasonPlayerCollisionHandler;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerCollisionHandler;

/**
 * NoellesRoles 接入 Wathe 玩家碰撞 API 的总入口。
 *
 * <p>这里只负责按职业 / 词条调用各自 handler。具体规则必须继续拆在对应包里，
 * 避免以后把所有玩家碰撞特判塞成一个难维护的大类。</p>
 */
public final class NoellesPlayerCollisionHandlers {
    private static boolean initialized = false;

    private NoellesPlayerCollisionHandlers() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        SpiritualistPlayerCollisionHandler.init();
        FeatherPlayerCollisionHandler.init();
        InsaneDamnedKillerPlayerCollisionHandler.init();
        JasonPlayerCollisionHandler.init();
    }
}
