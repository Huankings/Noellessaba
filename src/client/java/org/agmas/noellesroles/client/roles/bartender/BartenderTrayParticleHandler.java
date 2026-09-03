package org.agmas.noellesroles.client.roles.bartender;

import dev.doctor4t.wathe.api.tray.TrayParticleRegistry;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.particle.ParticleTypes;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 酒保职业的防御试剂托盘粒子。
 *
 * <p>粒子仅是客户端提示，真实护盾效果仍由服务端 TrayEffectHandler 处理；
 * 这里通过 Wathe 同步的 tray_effect 判断托盘状态，避免重新引入方块实体 mixin。</p>
 */
public final class BartenderTrayParticleHandler {
    private BartenderTrayParticleHandler() {
    }

    public static void init() {
        TrayParticleRegistry.registerProvider("noellesroles:defense_vial", 100, context -> {
            if (!NoellesEventIds.DEFENSE_TRAY_EFFECT.toString().equals(context.plate().getTrayEffect())
                    || !GameWorldComponent.KEY.get(context.world()).isRole(context.viewer(), NoellesRoleRegistry.BARTENDER)) {
                return false;
            }
            // 保留旧 DefenseVialViewMixin 的稀疏刷新，避免托盘数量较多时产生过多粒子。
            if (context.world().getRandom().nextBetween(0, 20) < 17) {
                return true;
            }
            context.world().addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    context.pos().getX() + 0.5,
                    context.pos().getY() + 0.5,
                    context.pos().getZ() + 0.5,
                    0.0, 0.15, 0.0
            );
            return true;
        });
    }
}
