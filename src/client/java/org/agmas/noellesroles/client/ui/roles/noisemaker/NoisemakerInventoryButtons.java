package org.agmas.noellesroles.client.ui.roles.noisemaker;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class NoisemakerInventoryButtons {
    private NoisemakerInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("noisemaker", NoisemakerInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), NoellesRoleRegistry.NOISEMAKER) ? new Extension() : null;
    }

    private static final class Extension extends NoellesInventoryButtonSupport.PagedExtension<NoisemakerPlayerWidget> {
        private Extension() {
            super("noisemaker");
        }

        @Override
        protected void populate(@NotNull InventoryButtonContext context, @NotNull LimitedInventoryScreen screen, @NotNull ClientPlayerEntity player) {
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            List<UUID> uuids = NoellesInventoryButtonSupport.onlineUuids(player);
            for (int i = 0; i < uuids.size(); i++) {
                UUID targetUuid = uuids.get(i);
                this.buttons.addWidget(context, new NoisemakerPlayerWidget(screen, 0, y, targetUuid, NoellesInventoryButtonSupport.entry(player, targetUuid), i));
            }
        }
    }
}
