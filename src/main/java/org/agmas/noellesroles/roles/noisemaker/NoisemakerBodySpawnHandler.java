package org.agmas.noellesroles.roles.noisemaker;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 噪音制造者死亡后，尸体短暂发光。
 */
public final class NoisemakerBodySpawnHandler {
    private static boolean initialized = false;

    private NoisemakerBodySpawnHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerBodySpawn(
                NoellesRolesCore.id("noisemaker_body_glow"),
                DeathApi.DEFAULT_PRIORITY,
                context -> {
                    /*
                     * 大嗓门的死亡效果作用在“尸体实体”上，而不是死去的玩家身上。
                     * BodySpawn 阶段 body 还没进入世界，但已经可以安全添加状态效果；
                     * spawn 后所有客户端都会看到这具尸体短暂发光。
                     */
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.victim().getWorld());
                    if (gameWorld.isRole(context.victim(), NoellesRoleRegistry.NOISEMAKER)) {
                        context.body().addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 60, 0));
                    }
                }
        );
    }
}
