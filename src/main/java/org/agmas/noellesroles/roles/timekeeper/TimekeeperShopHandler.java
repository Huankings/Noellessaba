package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * 时停者专属商店。
 */
public final class TimekeeperShopHandler {
    private TimekeeperShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();
        entries.add(ShopEntry.action(
                ModItems.DYING_WATCH_PROTECT.getDefaultStack(),
                TimekeeperConstants.REWIND_PROTECTION_PRICE,
                ShopEntry.Type.WEAPON,
                TimekeeperShopHandler::tryBuyRewindProtection,
                false
        ));
        return entries;
    }

    public static boolean tryBuyRewindProtection(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }

        TimekeeperWorldComponent worldComponent = TimekeeperWorldComponent.KEY.get(serverPlayer.getServerWorld());
        if (worldComponent.isRewinding()) {
            TimekeeperAbility.sendActionbar(serverPlayer, Text.translatable("message.noellesroles.timekeeper.protect_buy_rewinding"));
            return false;
        }

        TimekeeperPlayerComponent component = TimekeeperPlayerComponent.KEY.get(serverPlayer);
        if (component.hasRewindProtectionPurchased()) {
            TimekeeperAbility.sendActionbar(serverPlayer, Text.translatable("message.noellesroles.timekeeper.protect_buy_duplicate"));
            return false;
        }

        return component.tryMarkRewindProtectionPurchased();
    }
}
