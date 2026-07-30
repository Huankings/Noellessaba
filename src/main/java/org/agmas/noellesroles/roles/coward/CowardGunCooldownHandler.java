package org.agmas.noellesroles.roles.coward;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.GunCooldownContext;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 胆小鬼与镇静剂对 Wathe 原版左轮冷却的乘算修正。
 */
public final class CowardGunCooldownHandler {
    private static boolean initialized = false;

    private CowardGunCooldownHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerCooldownModifier(
                NoellesRolesCore.id("coward_revolver_cooldown"),
                GunShotApi.DEFAULT_PRIORITY,
                CowardGunCooldownHandler::modifyCooldown
        );
    }

    private static int modifyCooldown(GunCooldownContext context, int currentCooldown) {
        /*
         * 这里只修正 Wathe 普通左轮。
         * 德林加、赏金枪、强盗枪等都有自己的冷却语义，贸然套胆小鬼倍率会改变职业平衡。
         */
        if (!context.stack().isOf(WatheItems.REVOLVER) || context.shooter().isCreative()) {
            return currentCooldown;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.shooter().getWorld());
        boolean coward = gameWorld.isRole(context.shooter(), NoellesRoleRegistry.COWARD);
        boolean sedative = SedativePlayerComponent.KEY.get(context.shooter()).isActive();
        if (!coward && !sedative) {
            return currentCooldown;
        }

        float factor = 1.0F;
        if (coward) {
            // 胆小鬼本职：开枪更紧张，所以在 Wathe 当前基础冷却上乘以职业倍率。
            factor *= CowardConstants.REVOLVER_COOLDOWN_FACTOR;
        }
        if (sedative) {
            // 镇静剂是状态效果，可以和胆小鬼本职继续叠乘。
            factor *= CowardConstants.SEDATIVE_REVOLVER_COOLDOWN_FACTOR;
        }
        return Math.max(1, Math.round(currentCooldown * factor));
    }
}
