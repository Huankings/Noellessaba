package org.agmas.noellesroles.roles.corpsemaker;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

/**
 * 造尸怪职业分配处理器。
 */
public final class CorpsemakerRoleAssignedHandler {

    private CorpsemakerRoleAssignedHandler() {
    }

    /**
     * 造尸怪开局没有能力冷却，所以这里会把通用冷却改回 0 并立刻同步。
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.CORPSEMAKER)) {
            return;
        }

        AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(player);
        abilityPlayerComponent.cooldown = 0;
        abilityPlayerComponent.sync();
    }
}
