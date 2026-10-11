package org.agmas.noellesroles.client.bootstrap;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import org.agmas.noellesroles.NoellesRolesParticles;
import org.agmas.noellesroles.client.particle.StarstruckSparkleParticle;
import org.agmas.noellesroles.client.tray.NoellesRolesTrayParticles;

/** 客户端粒子工厂和托盘粒子的统一注册入口。 */
public final class NoellesClientParticleBootstrap {
    private NoellesClientParticleBootstrap() {
    }

    public static void register() {
        ParticleFactoryRegistry.getInstance().register(
                NoellesRolesParticles.STARSTRUCK_SPARKLE,
                StarstruckSparkleParticle.Provider::new
        );
        NoellesRolesTrayParticles.init();
    }
}
