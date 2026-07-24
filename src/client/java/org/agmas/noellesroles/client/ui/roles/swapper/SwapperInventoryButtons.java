package org.agmas.noellesroles.client.ui.roles.swapper;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class SwapperInventoryButtons {
    private SwapperInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("swapper", SwapperInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), NoellesRoleRegistry.SWAPPER) ? new Extension() : null;
    }

    private static final class Extension extends NoellesInventoryButtonSupport.PagedExtension<SwapperPlayerWidget> {
        private Extension() {
            super("swapper");
        }

        @Override
        protected void populate(@NotNull InventoryButtonContext context, @NotNull LimitedInventoryScreen screen, @NotNull ClientPlayerEntity player) {
            SwapperPlayerWidget.playerChoiceOne = null;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) {
                return;
            }
            List<AbstractClientPlayerEntity> players = new ArrayList<>(client.world.getPlayers());
            if (!players.contains(player)) {
                players.add(player);
            }
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            for (int i = 0; i < players.size(); i++) {
                this.buttons.addWidget(context, new SwapperPlayerWidget(screen, 0, y, players.get(i), i));
            }
        }

        @Override
        public void render(@NotNull InventoryButtonContext context, @NotNull DrawContext drawContext, int mouseX, int mouseY, float delta) {
            int y = (context.height() - 32) / 2;
            int x = context.width() / 2;
            Text name = SwapperPlayerWidget.playerChoiceOne == null
                    ? Text.translatable("hud.swapper.first_player_selection")
                    : Text.translatable("hud.swapper.second_player_selection");
            int color = SwapperPlayerWidget.playerChoiceOne == null ? Color.CYAN.getRGB() : Color.RED.getRGB();
            drawContext.drawTextWithShadow(context.textRenderer(), name, x - context.textRenderer().getWidth(name) / 2, y + 40, color);
        }
    }
}
