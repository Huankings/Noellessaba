package org.agmas.noellesroles.roles.vulture;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/**
 * 秃鹫职业分配处理器。
 */
public final class VultureRoleAssignedHandler {

    private VultureRoleAssignedHandler() {
    }

    /**
     * 处理秃鹫在职业分配时的进度初始化。
     *
     * <p>这里必须保留旧公式：
     * {@code (人数 / 3f) - floor(人数 / 6f)}，
     * 因为它直接决定秃鹫需要吞噬多少尸体才能进化。
     * 本次迁移只搬运结构，不修改任何平衡数值或计算方式。</p>
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.VULTURE)) {
            return;
        }

        VulturePlayerComponent vulturePlayerComponent = VulturePlayerComponent.KEY.get(player);
        vulturePlayerComponent.reset();
        vulturePlayerComponent.bodiesRequired =
                (int) ((player.getWorld().getPlayers().size() / 3f) - Math.floor(player.getWorld().getPlayers().size() / 6f));
        vulturePlayerComponent.sync();
    }
}
