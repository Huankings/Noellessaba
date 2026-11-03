package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.api.combat.GrenadeApi;
import dev.doctor4t.wathe.api.combat.GrenadeExplosionContext;
import dev.doctor4t.wathe.api.combat.GrenadeExplosionResult;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/** 真手雷与无声手雷爆炸对小偷悦灵的公开 API 接入。 */
public final class ThiefAllayGrenadeHandler {
    private static final int PRIORITY = 4_100;
    private static boolean initialized;

    private ThiefAllayGrenadeHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GrenadeApi.registerExplosionHandler(
                NoellesRolesCore.id("thief_allay/grenade_hit"),
                PRIORITY,
                ThiefAllayGrenadeHandler::handleExplosion
        );
    }

    private static @NotNull GrenadeExplosionResult handleExplosion(@NotNull GrenadeExplosionContext context) {
        ItemStack weapon = context.sourceStack();
        if (weapon.isOf(ModItems.FAKE_GRENADE)) {
            return GrenadeExplosionResult.PASS;
        }
        for (ThiefAllayEntity allay : context.world().getEntitiesByType(
                NoellesRolesEntities.THIEF_ALLAY_ENTITY_TYPE,
                entity -> entity.isOperationallyAlive()
                        && context.explosionBox(ThiefConstants.ALLAY_GRENADE_RADIUS)
                        .intersects(entity.getBoundingBox())
        )) {
            allay.killByWeapon(context.playerOwner(), weapon, GameConstants.DeathReasons.GRENADE);
        }
        return GrenadeExplosionResult.PASS;
    }
}
