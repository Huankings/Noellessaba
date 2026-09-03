package org.agmas.noellesroles.client.roles.coward;

import dev.doctor4t.wathe.api.tray.TrayParticleRegistry;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.particle.ParticleTypes;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.coward.CowardConstants;

/** 胆小鬼职业的镇静试剂托盘粒子注册。 */
public final class CowardTrayParticleHandler {
    private CowardTrayParticleHandler() {
    }

    public static void init() {
        TrayParticleRegistry.registerProvider("noellesroles:sedative", 100, context -> {
            if (!NoellesEventIds.SEDATIVE_TRAY_EFFECT.toString().equals(context.plate().getTrayEffect())
                    || !GameWorldComponent.KEY.get(context.viewer().getWorld()).isRole(context.viewer(), NoellesRoleRegistry.COWARD)) {
                return false;
            }
            // 使用坐标和世界时间形成稳定节拍，沿用旧 mixin 的可调常量。
            if (Math.floorMod(context.pos().asLong() + context.world().getTime(), CowardConstants.SEDATIVE_TRAY_PARTICLE_INTERVAL_TICKS) != 0) {
                return true;
            }
            double angle = context.world().random.nextDouble() * Math.PI * 2.0;
            double radius = 0.08 + context.world().random.nextDouble() * 0.24;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double velocity = 0.012 + context.world().random.nextDouble() * 0.012;
            context.world().addParticle(
                    ParticleTypes.WAX_ON,
                    context.pos().getX() + 0.5 + offsetX,
                    context.pos().getY() + 0.2 + context.world().random.nextDouble() * 0.08,
                    context.pos().getZ() + 0.5 + offsetZ,
                    offsetX * velocity,
                    0.02 + context.world().random.nextDouble() * 0.03,
                    offsetZ * velocity
            );
            return true;
        });
    }
}
