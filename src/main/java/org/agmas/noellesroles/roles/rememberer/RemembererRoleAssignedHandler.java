package org.agmas.noellesroles.roles.rememberer;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.ModItems;

/**
 * 追忆者职业分配处理器。
 */
public final class RemembererRoleAssignedHandler {

    private RemembererRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(NoellesRoleRegistry.REMEMBERER)) {
            return;
        }

        RemembererPlayerComponent remembererComponent = RemembererPlayerComponent.KEY.get(player);
        remembererComponent.reset();
        remembererComponent.startRoundCooldowns();
        RemembererReplayBookBuilder.removeOldMemoryBooks(player);

        AbilityPlayerComponent.KEY.get(player).setCooldown(RemembererConstants.RECALL_START_COOLDOWN_TICKS);
        player.getItemCooldownManager().set(ModItems.SNIPER_RIFLE, RemembererConstants.SNIPER_START_COOLDOWN_TICKS);
        player.giveItemStack(ModItems.SNIPER_RIFLE.getDefaultStack());
    }
}
