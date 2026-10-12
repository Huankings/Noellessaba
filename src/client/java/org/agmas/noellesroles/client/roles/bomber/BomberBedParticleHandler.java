package org.agmas.noellesroles.client.roles.bomber;

import dev.doctor4t.wathe.api.bed.BedParticleRegistry;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.particle.ParticleTypes;
import org.agmas.noellesroles.registry.NoellesEventIds;

/**
 * 炸弹客职业的定时炸弹床黑烟粒子注册。
 *
 * <p>床效果已经由 NoellesRolesBedEffects 注册到 Wathe 的服务端床接口；
 * 这里仅负责把原 TimedBombBedViewMixin 的客户端视觉迁移到公开床粒子 API。
 * 粒子不会改变床效果的真实触发、所有权或回放状态。</p>
 */
public final class BomberBedParticleHandler {
    private BomberBedParticleHandler() {
    }

    public static void init() {
        BedParticleRegistry.registerProvider(
                NoellesEventIds.TIMED_BOMB_BED_EMBEDDED_EVENT.toString(),
                100,
                context -> {
                    if (!NoellesEventIds.TIMED_BOMB_BED_EMBEDDED_EVENT.toString()
                            .equals(context.bed().getBedEffect())) {
                        return false;
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.world());
                    if (gameWorld == null || !gameWorld.canUseKillerFeatures(context.viewer())) {
                        // 已识别该床效果，但当前观察者没有杀手能力可见权限。
                        return true;
                    }

                    // 沿用旧 Mixin 的稀疏刷新策略，避免床粒子变成每 tick 的高频开销。
                    if (context.world().getRandom().nextBetween(0, 20) < 17) {
                        return true;
                    }

                    context.world().addParticle(
                            ParticleTypes.SMOKE,
                            context.pos().getX() + 0.5,
                            context.pos().getY() + 0.5,
                            context.pos().getZ() + 0.5,
                            0.0,
                            0.04,
                            0.0
                    );
                    return true;
                }
        );
    }
}
