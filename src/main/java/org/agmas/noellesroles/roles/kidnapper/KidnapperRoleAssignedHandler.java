package org.agmas.noellesroles.roles.kidnapper;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 绑匪职业分配初始化。
 */
public final class KidnapperRoleAssignedHandler {
    private KidnapperRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != Noellesroles.KIDNAPPER) {
            return;
        }

        /*
         * 绑匪开局自带一瓶迷药，但仍然有 30 秒开局冷却。
         * 这样既保留 kinssaba 的“自带迷药”，也遵循 NoellesRoles 对专属物品的开局保护格式。
         */
        KidnapperComponent kidnapperComponent = KidnapperComponent.KEY.get(player);
        kidnapperComponent.resetAll();
        kidnapperComponent.startRoundCooldowns();

        player.giveItemStack(ModItems.KNOCKOUT_DRUG.getDefaultStack());
        player.getItemCooldownManager().set(ModItems.KNOCKOUT_DRUG, KidnapperConstants.START_COOLDOWN_TICKS);
    }
}
