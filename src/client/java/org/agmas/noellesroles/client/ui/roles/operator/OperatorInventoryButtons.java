package org.agmas.noellesroles.client.ui.roles.operator;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class OperatorInventoryButtons {
    private OperatorInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("operator", OperatorInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), Noellesroles.OPERATOR) ? new Extension() : null;
    }

    private static final class Extension extends NoellesInventoryButtonSupport.PagedExtension<OperatorPlayerWidget> {
        private Extension() {
            super("operator");
        }

        @Override
        protected void populate(@NotNull InventoryButtonContext context, @NotNull LimitedInventoryScreen screen, @NotNull ClientPlayerEntity player) {
            OperatorPlayerWidget.firstChoice = null;
            Set<UUID> uuids = new LinkedHashSet<>(NoellesInventoryButtonSupport.onlineUuids(player));
            uuids.add(player.getUuid());
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            for (UUID targetUuid : uuids) {
                this.buttons.addWidget(context, new OperatorPlayerWidget(screen, 0, y, targetUuid, NoellesInventoryButtonSupport.entry(player, targetUuid)));
            }
        }

        @Override
        public void render(@NotNull InventoryButtonContext context, @NotNull DrawContext drawContext, int mouseX, int mouseY, float delta) {
            Text text = OperatorPlayerWidget.firstChoice == null
                    ? Text.translatable("hud.operator.first_player_selection")
                    : Text.translatable("hud.operator.second_player_selection");
            drawContext.drawTextWithShadow(context.textRenderer(), text, context.width() / 2 - context.textRenderer().getWidth(text) / 2, (context.height() - 32) / 2 + 40, Noellesroles.OPERATOR.color());
        }
    }
}
