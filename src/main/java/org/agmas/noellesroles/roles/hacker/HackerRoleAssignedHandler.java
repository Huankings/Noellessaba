package org.agmas.noellesroles.roles.hacker;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 黑客职业分配初始化。
 */
public final class HackerRoleAssignedHandler {
    private HackerRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());

        if (role.equals(Noellesroles.HACKER)) {
            HackerComponent.KEY.get(player).reset();
            HackerPhoneComponent.KEY.get(player).reset();
            givePhoneIfMissing(player);
            givePhoneToCurrentKillers(player);
            return;
        }

        /*
         * Harpy 会先分配中立，再替换杀手职业。
         * 如果 Hacker 已经拿到了身份，后续新替换出来的扩展杀手也应该获得手机；
         * 这里做一次补发，同时用背包检查避免重复发给原版杀手位。
         */
        if (role.canUseKiller()) {
            for (ServerPlayerEntity possibleHacker : player.getServer().getPlayerManager().getPlayerList()) {
                if (gameWorld.isRole(possibleHacker, Noellesroles.HACKER) && GameFunctions.isPlayerAliveAndSurvival(possibleHacker)) {
                    givePhoneIfMissing(player);
                    break;
                }
            }
        }
    }

    private static void givePhoneToCurrentKillers(PlayerEntity hacker) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(hacker.getWorld());
        for (ServerPlayerEntity serverPlayer : hacker.getServer().getPlayerManager().getPlayerList()) {
            if (gameWorld.canUseKillerFeatures(serverPlayer)) {
                givePhoneIfMissing(serverPlayer);
            }
        }
    }

    private static void givePhoneIfMissing(PlayerEntity player) {
        if (player.getInventory().contains(stack -> stack.isOf(ModItems.PHONE))) {
            return;
        }
        player.giveItemStack(HackerPhoneComponent.KEY.get(player).createPhoneStack());
    }
}
