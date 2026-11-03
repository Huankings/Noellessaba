package org.agmas.noellesroles.roles.magician;

import dev.doctor4t.wathe.api.combat.GrenadeApi;
import dev.doctor4t.wathe.api.combat.GrenadeExplosionContext;
import dev.doctor4t.wathe.api.combat.GrenadeExplosionResult;
import dev.doctor4t.wathe.game.GameConstants;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/** 普通/无声手雷爆炸对魔术师播放体的公开 API 接入。 */
public final class MagicianGrenadeHandler {
    private static final int PRIORITY = 4_000;
    private static boolean initialized;

    private MagicianGrenadeHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GrenadeApi.registerExplosionHandler(
                NoellesRolesCore.id("magician/grenade_playback_hit"),
                PRIORITY,
                MagicianGrenadeHandler::handleExplosion
        );
    }

    private static @NotNull GrenadeExplosionResult handleExplosion(@NotNull GrenadeExplosionContext context) {
        if (context.sourceStack().isOf(ModItems.FAKE_GRENADE)) {
            return GrenadeExplosionResult.PASS;
        }
        MagicianServerHooks.stopPlaybackInExplosion(
                context.world(),
                context.explosionBox(GrenadeExplosionContext.DEFAULT_RADIUS),
                context.playerOwner(),
                GameConstants.DeathReasons.GRENADE,
                MagicianServerHooks.getWeaponName(context.sourceStack())
        );
        return GrenadeExplosionResult.PASS;
    }
}
