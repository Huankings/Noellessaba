package org.agmas.noellesroles.client.ui.roles.voodoo;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.UUID;

public final class VoodooInventoryButtons {
    private VoodooInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("voodoo", VoodooInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), Noellesroles.VOODOO) ? new Extension() : null;
    }

    private static final class Extension extends NoellesInventoryButtonSupport.PagedExtension<VoodooPlayerWidget> {
        private Extension() {
            super("voodoo");
        }

        @Override
        protected void populate(@NotNull InventoryButtonContext context, @NotNull LimitedInventoryScreen screen, @NotNull ClientPlayerEntity player) {
            List<UUID> uuids = NoellesInventoryButtonSupport.onlineUuids(player);
            uuids.removeIf(uuid -> uuid.equals(player.getUuid()));
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            World world = player.getWorld();
            for (int i = 0; i < uuids.size(); i++) {
                UUID targetUuid = uuids.get(i);
                this.buttons.addWidget(context, new VoodooPlayerWidget(screen, 0, y, targetUuid, NoellesInventoryButtonSupport.entry(player, targetUuid), world, i));
            }
        }

        @Override
        public void render(@NotNull InventoryButtonContext context, @NotNull DrawContext drawContext, int mouseX, int mouseY, float delta) {
            ClientPlayerEntity player = context.requirePlayer();
            ConfigWorldComponent config = ConfigWorldComponent.KEY.get(player.getWorld());
            if (!config.naturalVoodoosAllowed) {
                Text text = Text.translatable("hud.voodoo.player_deaths_only");
                drawContext.drawTextWithShadow(context.textRenderer(), text, context.width() / 2 - context.textRenderer().getWidth(text) / 2, (context.height() - 32) / 2 + 40, Color.RED.getRGB());
            }
        }
    }
}
