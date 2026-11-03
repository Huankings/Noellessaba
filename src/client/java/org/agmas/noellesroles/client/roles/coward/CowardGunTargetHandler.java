package org.agmas.noellesroles.client.roles.coward;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.GunTargetResult;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 胆小鬼左轮抖动射线的客户端 GunShotApi 接入。 */
public final class CowardGunTargetHandler {
    private static boolean initialized;

    private CowardGunTargetHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerTargetRule(
                NoellesRolesCore.id("coward/revolver_shake_target"),
                3_000,
                context -> {
                    if (!context.stack().isOf(WatheItems.REVOLVER)) {
                        return GunTargetResult.pass();
                    }
                    HitResult offsetTarget = CowardClientEffects.getOffsetRevolverTarget(context.user());
                    return offsetTarget == null
                            ? GunTargetResult.pass()
                            : GunTargetResult.target(offsetTarget);
                }
        );
    }
}
