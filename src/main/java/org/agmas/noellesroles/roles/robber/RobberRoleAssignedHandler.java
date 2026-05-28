package org.agmas.noellesroles.roles.robber;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 强盗职业分配处理器。
 */
public final class RobberRoleAssignedHandler {

    private RobberRoleAssignedHandler() {
    }

    /**
     * 强盗在职业分配时要同时完成“状态重置、开局冷却、开局发物资”三件事。
     *
     * <p>这段逻辑不仅用于正式开局，也要兼容后续若存在的中途转职场景，
     * 因此这里仍然先 reset，再重新写入新身份应该拥有的冷却和物品。</p>
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.ROBBER)) {
            return;
        }

        RobberPlayerComponent robberComponent = RobberPlayerComponent.KEY.get(player);
        robberComponent.reset();
        robberComponent.startRoundCooldowns();

        player.getItemCooldownManager().set(ModItems.THROWING_AXE, RobberPlayerComponent.ROBBER_START_COOLDOWN_TICKS);
        player.getItemCooldownManager().set(ModItems.ROBBER_PISTOL, RobberPlayerComponent.ROBBER_START_COOLDOWN_TICKS);
        player.giveItemStack(ModItems.ROBBER_PISTOL.getDefaultStack());
        player.giveItemStack(WatheItems.CROWBAR.getDefaultStack());
    }
}
