package org.agmas.noellesroles.roles.assassin;

import dev.doctor4t.wathe.api.combat.GrenadeApi;
import dev.doctor4t.wathe.api.combat.GrenadeExplosionContext;
import dev.doctor4t.wathe.api.combat.GrenadeExplosionResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/** 无声手雷的投掷模式注册与无声爆炸处理。 */
public final class AssassinGrenadeHandler {
    private static final int PRIORITY = 2_900;
    private static boolean initialized;

    private AssassinGrenadeHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GrenadeApi.registerThrowableItem(NoellesRolesCore.id("silent_grenade"), ModItems.SILENT_GRENADE);
        GrenadeApi.registerExplosionHandler(
                NoellesRolesCore.id("assassin/silent_grenade_explosion"),
                PRIORITY,
                AssassinGrenadeHandler::handleExplosion
        );
    }

    private static @NotNull GrenadeExplosionResult handleExplosion(@NotNull GrenadeExplosionContext context) {
        if (!context.sourceStack().isOf(ModItems.SILENT_GRENADE)) {
            return GrenadeExplosionResult.PASS;
        }
        /* 无声手雷只移除爆炸声；粒子、范围伤害和真实物品回放全部复用 Wathe 公共步骤。 */
        context.spawnDefaultParticles(context.sourceStack());
        context.damagePlayers(context.sourceStack(), GrenadeExplosionContext.DEFAULT_RADIUS);
        return GrenadeExplosionResult.HANDLED;
    }
}
