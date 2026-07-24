package org.agmas.noellesroles.client.ui.roles.magician;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MagicianInventoryButtons {
    private MagicianInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("magician", MagicianInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), Noellesroles.MAGICIAN) ? new Extension() : null;
    }

    private static final class Extension extends NoellesInventoryButtonSupport.PagedExtension<MagicianPlayerWidget> {
        private Extension() {
            super("magician");
        }

        @Override
        protected void populate(@NotNull InventoryButtonContext context, @NotNull LimitedInventoryScreen screen, @NotNull ClientPlayerEntity player) {
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            for (UUID targetUuid : NoellesInventoryButtonSupport.onlineUuids(player)) {
                this.buttons.addWidget(context, new MagicianPlayerWidget(screen, 0, y, targetUuid, NoellesInventoryButtonSupport.entry(player, targetUuid)));
            }
        }
    }
}
