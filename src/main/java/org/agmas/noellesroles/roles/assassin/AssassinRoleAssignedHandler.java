package org.agmas.noellesroles.roles.assassin;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 刺客职业分配处理器。
 */
public final class AssassinRoleAssignedHandler {

    private AssassinRoleAssignedHandler() {
    }

    /**
     * 刺客在回合开始时，需要让刺刀和无声左轮先进入 30 秒开局冷却。
     *
     * <p>这里故意不处理无声手雷，因为用户需求只要求这两个开局先锁住，
     * 并且无声手雷本身已经有独立的长冷却与一次性消耗语义。</p>
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.ASSASSIN)) {
            return;
        }

        AssassinPlayerComponent assassinComponent = AssassinPlayerComponent.KEY.get(player);
        assassinComponent.reset();
        assassinComponent.startRoundCooldowns();

        player.getItemCooldownManager().set(ModItems.BAYONET, AssassinPlayerComponent.ASSASSIN_START_COOLDOWN_TICKS);
        player.getItemCooldownManager().set(ModItems.SILENCED_REVOLVER, AssassinPlayerComponent.ASSASSIN_START_COOLDOWN_TICKS);
    }
}
