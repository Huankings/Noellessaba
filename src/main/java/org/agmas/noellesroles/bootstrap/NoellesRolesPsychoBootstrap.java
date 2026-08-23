package org.agmas.noellesroles.bootstrap;

import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPsychoHandler;
import org.agmas.noellesroles.roles.cook.CookPsychoHandler;
import org.agmas.noellesroles.roles.jason.JasonPsychoHandler;
import org.agmas.noellesroles.roles.jester.JesterPsychoHandler;
import org.agmas.noellesroles.roles.muzzler.MuzzlerPsychoHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererPsychoShieldHandler;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapPsychoHandler;

/**
 * NoellesRoles 的疯魔 API 接入分发器。
 *
 * <p>这里只负责按职业调用各自的注册方法，不承载具体玩法判断。
 * 后续如果某个职业要接入新的疯魔 profile / 护盾规则 / 声音规则，
 * 应继续放回对应 {@code roles/<role>/} 包内，避免重新堆成一个难维护的大类。</p>
 */
public final class NoellesRolesPsychoBootstrap {
    private static boolean initialized = false;

    private NoellesRolesPsychoBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        JesterPsychoHandler.init();
        BountyHunterPsychoHandler.init();
        MuzzlerPsychoHandler.init();
        RemembererPsychoShieldHandler.init();
        SpringTrapPsychoHandler.init();
        CookPsychoHandler.init();
        JasonPsychoHandler.init();
    }
}
