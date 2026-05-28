package org.agmas.noellesroles.roles.bomber;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 炸弹客职业分配处理器。
 */
public final class BomberRoleAssignedHandler {

    private BomberRoleAssignedHandler() {
    }

    /**
     * 炸弹客在分配身份时需要做两件事：
     * 重置炸弹组件，并给定时炸弹施加开局冷却。
     *
     * <p>这样无论是正常开局，还是中途通过其它机制转职成炸弹客，
     * 都不会带着旧局或旧职业残留的炸弹状态进入新身份。</p>
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.BOMBER)) {
            return;
        }

        BomberPlayerComponent.KEY.get(player).reset();
        player.getItemCooldownManager().set(ModItems.TIMED_BOMB, BomberPlayerComponent.BOMBER_START_COOLDOWN_TICKS);
    }
}
