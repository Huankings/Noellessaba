package org.agmas.noellesroles.client.roles.timekeeper;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.item.TimekeeperWatchItem;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchState;

/**
 * 时停者右下角能力 HUD。
 */
public final class TimekeeperStatusHud {
    private TimekeeperStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/timekeeper/status", NoellesRoleRegistry.TIMEKEEPER, context -> {
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(context.player());

            Text line;
            if (hasWatch(context.player(), TimekeeperWatchState.NORMAL)) {
                line = shop.balance >= TimekeeperConstants.UPGRADE_WATCH_PRICE
                        ? Text.translatable("tip.noellesroles.timekeeper.upgrade.ready", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText())
                        : Text.translatable("tip.noellesroles.timekeeper.upgrade.need_money", TimekeeperConstants.UPGRADE_WATCH_PRICE);
            } else if (hasWatch(context.player(), TimekeeperWatchState.BROKEN)) {
                line = shop.balance >= TimekeeperConstants.REPAIR_WATCH_PRICE
                        ? Text.translatable("tip.noellesroles.timekeeper.repair.ready", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText())
                        : Text.translatable("tip.noellesroles.timekeeper.repair.need_money", TimekeeperConstants.REPAIR_WATCH_PRICE);
            } else {
                line = Text.translatable("tip.noellesroles.timekeeper.no_watch");
            }

            NoellesHudSupport.drawBottomRightLine(context, line, TimekeeperConstants.ROLE_COLOR);
        });
    }

    private static boolean hasWatch(net.minecraft.entity.player.PlayerEntity player, TimekeeperWatchState state) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(ModItems.DYING_WATCH) && TimekeeperWatchItem.getState(stack) == state) {
                return true;
            }
        }
        return false;
    }
}
