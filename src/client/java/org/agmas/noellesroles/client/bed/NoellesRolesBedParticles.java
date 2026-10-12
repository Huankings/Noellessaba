package org.agmas.noellesroles.client.bed;

import org.agmas.noellesroles.client.roles.bomber.BomberBedParticleHandler;

/**
 * NoellesRoles 床粒子注册聚合入口。
 *
 * <p>这里只负责保持初始化顺序和幂等性，具体床效果仍按职业拆分到
 * {@code client.roles.<role>} 下，避免未来所有床粒子继续堆进客户端主入口。</p>
 */
public final class NoellesRolesBedParticles {
    private static boolean initialized;

    private NoellesRolesBedParticles() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        BomberBedParticleHandler.init();
    }
}
