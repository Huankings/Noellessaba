package org.agmas.noellesroles.client.ui.roles.morphling;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class MorphlingInventoryButtons {
    private MorphlingInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("morphling", MorphlingInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), NoellesRoleRegistry.MORPHLING) ? new Extension() : null;
    }

    private static final class Extension extends NoellesInventoryButtonSupport.PagedExtension<MorphlingPlayerWidget> {
        private Extension() {
            super("morphling");
        }

        @Override
        protected boolean selectionVisible(ClientPlayerEntity player) {
            /*
             * 变形开始后，玩家头像和翻页按钮必须一起隐藏。
             * PagedExtension 每 tick 都用这个条件刷新所有分页控件，避免只隐藏头像而留下翻页按钮。
             */
            return MorphlingPlayerComponent.KEY.get(player).getMorphTicks() <= 0;
        }

        @Override
        protected void populate(@NotNull InventoryButtonContext context, @NotNull LimitedInventoryScreen screen, @NotNull ClientPlayerEntity player) {
            List<UUID> uuids = NoellesInventoryButtonSupport.onlineUuids(player);
            uuids.removeIf(uuid -> uuid.equals(player.getUuid()));
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            for (int i = 0; i < uuids.size(); i++) {
                UUID targetUuid = uuids.get(i);
                this.buttons.addWidget(context, new MorphlingPlayerWidget(
                        screen,
                        0,
                        y,
                        targetUuid,
                        NoellesInventoryButtonSupport.entry(player, targetUuid),
                        NoellesInventoryButtonSupport.clientEntity(targetUuid),
                        i
                ));
            }
        }
    }
}
