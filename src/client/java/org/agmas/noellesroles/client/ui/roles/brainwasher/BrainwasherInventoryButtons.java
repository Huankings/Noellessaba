package org.agmas.noellesroles.client.ui.roles.brainwasher;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class BrainwasherInventoryButtons {
    private BrainwasherInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("brainwasher", BrainwasherInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), NoellesRoleRegistry.BRAINWASHER) ? new Extension() : null;
    }

    private static final class Extension extends NoellesInventoryButtonSupport.PagedExtension<BrainwasherPlayerWidget> {
        private Extension() {
            super("brainwasher");
        }

        @Override
        protected void populate(@NotNull InventoryButtonContext context, @NotNull LimitedInventoryScreen screen, @NotNull ClientPlayerEntity player) {
            List<UUID> uuids = NoellesInventoryButtonSupport.onlineUuids(player);
            uuids.removeIf(uuid -> uuid.equals(player.getUuid()));
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            for (UUID targetUuid : uuids) {
                PlayerListEntry entry = NoellesInventoryButtonSupport.entry(player, targetUuid);
                if (entry != null) {
                    this.buttons.addWidget(context, new BrainwasherPlayerWidget(screen, 0, y, targetUuid, entry));
                }
            }
        }

        @Override
        public void render(@NotNull InventoryButtonContext context, @NotNull DrawContext drawContext, int mouseX, int mouseY, float delta) {
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.requirePlayer());
            Text message = ability.cooldown > 0
                    ? Text.translatable("hud.brainwasher.cooldown", ability.cooldown / 20)
                    : Text.translatable("hud.brainwasher.select_player");
            drawContext.drawTextWithShadow(context.textRenderer(), message, context.width() / 2 - context.textRenderer().getWidth(message) / 2, (context.height() - 32) / 2 + 40, 0xFF69B4);
        }
    }
}
