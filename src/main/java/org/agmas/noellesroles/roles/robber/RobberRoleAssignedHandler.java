package org.agmas.noellesroles.roles.robber;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;

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
        if (!role.equals(NoellesRoleRegistry.ROBBER)) {
            return;
        }

        /*
         * 先移除中途转职前可能残留的旧冷却，再写入本职业开局冷却。
         * tooltip 直接读取这里建立的真实条目，不再需要额外的强盗玩家组件同步来源。
         */
        player.getItemCooldownManager().remove(ModItems.THROWING_AXE);
        player.getItemCooldownManager().remove(ModItems.ROBBER_PISTOL);
        player.getItemCooldownManager().set(ModItems.THROWING_AXE, RobberConstants.START_COOLDOWN_TICKS);
        player.getItemCooldownManager().set(ModItems.ROBBER_PISTOL, RobberConstants.START_COOLDOWN_TICKS);
        player.giveItemStack(ModItems.ROBBER_PISTOL.getDefaultStack());
        player.giveItemStack(WatheItems.CROWBAR.getDefaultStack());
    }
}
