package org.agmas.noellesroles.roles.bounty_hunter;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 赏金猎人职业分配处理器。
 */
public final class BountyHunterRoleAssignedHandler {
    private BountyHunterRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(NoellesRoleRegistry.BOUNTY_HUNTER)) {
            return;
        }

        /*
         * 开局发放赏金手枪，同时写入 30 秒真实冷却。
         * Wathe tooltip API 读取的正是同一个 ItemCooldownManager 条目。
         */
        BountyHunterPlayerComponent bountyHunter = BountyHunterPlayerComponent.KEY.get(player);
        bountyHunter.reset();
        bountyHunter.startRoundCooldowns();
        player.giveItemStack(ModItems.BOUNTY_PISTOL.getDefaultStack());
    }
}
