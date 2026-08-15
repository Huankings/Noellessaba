package org.agmas.noellesroles.visibility;

import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerTargetVisibilityHandler;
import org.agmas.noellesroles.roles.jason.JasonTargetVisibilityHandler;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistTargetVisibilityHandler;

/**
 * 玩家实体隐藏 / 不可选中规则的入口。
 *
 * <p>具体逻辑写在对应 {@code roles/<role>/} 或词条包里，
 * 这里只补一行 {@code XxxTargetVisibilityHandler.init()}，避免规则堆在公共大类里。</p>
 */
public final class NoellesPlayerTargetVisibilityHandlers {
    private static boolean initialized = false;

    private NoellesPlayerTargetVisibilityHandlers() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        SpiritualistTargetVisibilityHandler.init();
        InsaneDamnedKillerTargetVisibilityHandler.init();
        JasonTargetVisibilityHandler.init();
    }
}
