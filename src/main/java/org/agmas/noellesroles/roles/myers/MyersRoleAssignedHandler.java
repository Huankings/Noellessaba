package org.agmas.noellesroles.roles.myers;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.Nullable;

/** 迈尔斯职业分配后的货币和初始人数状态。 */
public final class MyersRoleAssignedHandler {
    private MyersRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, @Nullable Role role) {
        MyersPlayerComponent component = MyersPlayerComponent.KEY.get(player);
        component.reset();
        PlayerShopComponent.KEY.get(player).setCurrencyAmount(MyersConstants.MALICE_CURRENCY_ID, 0);
        if (role == NoellesRoleRegistry.MYERS) {
            int count = GameFunctions.getReadyPlayerCount(player.getWorld());
            if (count <= 0) {
                count = (int) player.getWorld().getPlayers().stream()
                        .filter(candidate -> GameFunctions.isPlayerAliveAndSurvival(candidate))
                        .count();
            }
            component.setInitialParticipantCount(count);
        }
    }
}
