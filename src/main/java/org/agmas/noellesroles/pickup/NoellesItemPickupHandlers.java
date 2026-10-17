package org.agmas.noellesroles.pickup;

import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityItemPickupHandler;
import org.agmas.noellesroles.roles.executioner.ExecutionerItemPickupHandler;
import org.agmas.noellesroles.roles.jason.JasonItemPickupHandler;
import org.agmas.noellesroles.roles.jester.JesterItemPickupHandler;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenItemPickupHandler;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainItemPickupHandler;

/**
 * NoellesRoles 掉落物拾取 API 的聚合注册器。
 *
 * <p>这里只维护注册顺序，不写任何职业判定。每个职业或词条的拾取规则都在自己的 handler 里，
 * 避免所有物品限制积压成一个无法维护的大类。</p>
 */
public final class NoellesItemPickupHandlers {
    private static boolean initialized;

    private NoellesItemPickupHandlers() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        /*
         * 先列出强制禁拾状态，再列出仅覆盖 Wathe 默认枪械限制的仇杀客。
         * 真正决定顺序由 ItemPickupApi 的 priority 统一保证，这里的顺序只是让源码易读。
         */
        DualPersonalityItemPickupHandler.init();
        JasonItemPickupHandler.init();
        KillerQueenItemPickupHandler.init();
        JesterItemPickupHandler.init();
        LicensedVillainItemPickupHandler.init();
        ExecutionerItemPickupHandler.init();
    }
}
