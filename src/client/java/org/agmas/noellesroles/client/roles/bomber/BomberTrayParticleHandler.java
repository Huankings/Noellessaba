package org.agmas.noellesroles.client.roles.bomber;

import dev.doctor4t.wathe.api.tray.TrayParticleRegistry;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.particle.ParticleTypes;
import org.agmas.noellesroles.registry.NoellesEventIds;

/** 炸弹客职业的定时炸弹托盘黑烟粒子注册。 */
public final class BomberTrayParticleHandler {
    private BomberTrayParticleHandler() {
    }

    public static void init() {
        TrayParticleRegistry.registerProvider("noellesroles:timed_bomb", 100, context -> {
            if (!NoellesEventIds.TIMED_BOMB_TRAY_EMBEDDED_EVENT.toString().equals(context.plate().getTrayEffect())
                    || !GameWorldComponent.KEY.get(context.world()).canUseKillerFeatures(context.viewer())) {
                return false;
            }
            // 定时炸弹黑烟沿用旧 TimedBombTrayViewMixin 的稀疏刷新策略。
            if (context.world().getRandom().nextBetween(0, 20) < 17) {
                return true;
            }
            context.world().addParticle(
                    ParticleTypes.SMOKE,
                    context.pos().getX() + 0.5,
                    context.pos().getY() + 0.5,
                    context.pos().getZ() + 0.5,
                    0.0, 0.04, 0.0
            );
            return true;
        });
    }
}
