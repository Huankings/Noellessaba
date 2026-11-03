package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.api.combat.GrenadeApi;
import dev.doctor4t.wathe.api.combat.GrenadeExplosionContext;
import dev.doctor4t.wathe.api.combat.GrenadeExplosionResult;
import dev.doctor4t.wathe.index.WatheItems;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/** 假手雷的物品注册与“只播放表现、不造成伤害”爆炸规则。 */
public final class FakeGrenadeExplosionHandler {
    private static final int PRIORITY = 3_000;
    private static boolean initialized;

    private FakeGrenadeExplosionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GrenadeApi.registerThrowableItem(NoellesRolesCore.id("fake_grenade"), ModItems.FAKE_GRENADE);
        GrenadeApi.registerExplosionHandler(
                NoellesRolesCore.id("fake_grenade/explosion"),
                PRIORITY,
                FakeGrenadeExplosionHandler::handleExplosion
        );
    }

    private static @NotNull GrenadeExplosionResult handleExplosion(@NotNull GrenadeExplosionContext context) {
        if (!context.sourceStack().isOf(ModItems.FAKE_GRENADE)) {
            return GrenadeExplosionResult.PASS;
        }
        context.playDefaultSound();
        /* 旧实现使用普通“已投掷手雷”碎片，继续保留相同视觉，不暴露这是道具手雷。 */
        context.spawnDefaultParticles(WatheItems.THROWN_GRENADE.getDefaultStack());
        return GrenadeExplosionResult.HANDLED;
    }
}
