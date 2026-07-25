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
         * 开局发放赏金手枪，同时写入 30 秒开局冷却。
         * 组件里的开局冷却标记只服务于客户端 tooltip 总时长显示；
         * 真正阻止开枪的仍然是 Minecraft 的 ItemCooldownManager。
         */
        BountyHunterPlayerComponent bountyHunter = BountyHunterPlayerComponent.KEY.get(player);
        bountyHunter.reset();
        bountyHunter.startRoundCooldowns();
        player.getItemCooldownManager().set(ModItems.BOUNTY_PISTOL, BountyHunterConstants.START_COOLDOWN_TICKS);
        player.giveItemStack(ModItems.BOUNTY_PISTOL.getDefaultStack());
    }
}
