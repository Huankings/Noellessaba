package org.agmas.noellesroles.client.tray;

import org.agmas.noellesroles.client.roles.bartender.BartenderTrayParticleHandler;
import org.agmas.noellesroles.client.roles.bomber.BomberTrayParticleHandler;
import org.agmas.noellesroles.client.roles.coward.CowardTrayParticleHandler;

/** NoellesRoles 托盘粒子注册聚合入口；具体逻辑按职业拆分到 client.roles 下。 */
public final class NoellesRolesTrayParticles {
    private static boolean initialized;
    private NoellesRolesTrayParticles() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        BartenderTrayParticleHandler.init();
        CowardTrayParticleHandler.init();
        BomberTrayParticleHandler.init();
    }
}
