package org.agmas.noellesroles.roles.drugmaker;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;

/**
 * 制毒师职业分配初始化。
 */
public final class DrugmakerRoleAssignedHandler {
    private DrugmakerRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != NoellesRoleRegistry.DRUGMAKER) {
            return;
        }

        /*
         * 按 NoellesRoles 专属武器的现有格式，只给当前职业会使用的物品写开局冷却。
         * 这样不会影响其它职业从掉落、调试或后续机制拿到同名物品时的开局状态。
         */
        DrugmakerPlayerComponent drugmakerComponent = DrugmakerPlayerComponent.KEY.get(player);
        drugmakerComponent.reset();
        drugmakerComponent.startRoundCooldowns();

        player.getItemCooldownManager().set(ModItems.BLOWGUN, DrugmakerConstants.START_COOLDOWN_TICKS);
        player.getItemCooldownManager().set(ModItems.POISON_INJECTOR, DrugmakerConstants.START_COOLDOWN_TICKS);
    }
}
