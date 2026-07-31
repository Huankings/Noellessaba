package org.agmas.noellesroles.visibility;

import org.agmas.noellesroles.roles.assassin.AssassinTargetVisibilityHandler;

/**
 * NoellesRoles 接入 Wathe 目标可见性 API 的总入口。
 *
 * <p>这里只负责按职业 / 词条调用各自的 handler。具体规则必须继续拆在对应职业或词条包里，
 * 避免以后把所有玩家、尸体隐藏规则塞成一个难维护的大类。</p>
 */
public final class NoellesTargetVisibilityHandlers {
    private static boolean initialized = false;

    private NoellesTargetVisibilityHandlers() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        AssassinTargetVisibilityHandler.init();
        NoellesPlayerTargetVisibilityHandlers.init();
    }
}
