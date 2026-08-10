package org.agmas.noellesroles.bootstrap;

import org.agmas.noellesroles.roles.convener.ConvenerMovementHandler;
import org.agmas.noellesroles.roles.hunter.HunterMovementHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererMovementHandler;
import org.agmas.noellesroles.roles.starstruck.StarstruckMovementHandler;

/**
 * NoellesRoles 的玩家移动速度 API 接入入口。
 *
 * <p>这里只负责调用各职业自己的 handler，具体判断仍保留在职业包内，
 * 避免后续新增移速职业时把逻辑继续堆进一个难维护的大类。</p>
 */
public final class NoellesRolesMovementBootstrap {
    private NoellesRolesMovementBootstrap() {
    }

    public static void init() {
        HunterMovementHandler.init();
        ConvenerMovementHandler.init();
        StarstruckMovementHandler.init();
        RemembererMovementHandler.init();
    }
}
