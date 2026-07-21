package org.agmas.noellesroles;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * NoellesRoles 自定义粒子注册入口。
 *
 * <p>星界使者从 StarryExpress 搬入后，粒子资源也改到了 noellesroles 命名空间。
 * 服务端只发送粒子类型 ID，客户端再按资源包里的 particle JSON 找到具体贴图。</p>
 */
public final class NoellesRolesParticles {
    public static final SimpleParticleType STARSTRUCK_SPARKLE = FabricParticleTypes.simple();

    private NoellesRolesParticles() {
    }

    public static void init() {
        Registry.register(
                Registries.PARTICLE_TYPE,
                Identifier.of(Noellesroles.MOD_ID, "starstruck_sparkle"),
                STARSTRUCK_SPARKLE
        );
    }
}
