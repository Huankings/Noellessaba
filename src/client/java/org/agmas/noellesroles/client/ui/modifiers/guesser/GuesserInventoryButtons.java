package org.agmas.noellesroles.client.ui.modifiers.guesser;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GuesserInventoryButtons {
    private GuesserInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("guesser", GuesserInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        ClientPlayerEntity player = context.requirePlayer();
        return WorldModifierComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.GUESSER) ? new Extension() : null;
    }

    private static final class Extension extends NoellesInventoryButtonSupport.PagedExtension<GuesserPlayerWidget> {
        private GuesserRoleWidget roleWidget;

        private Extension() {
            super("guesser");
        }

        @Override
        protected boolean selectionVisible(ClientPlayerEntity player) {
            return GuesserPlayerWidget.selectedPlayer == null;
        }

        @Override
        protected void populate(@NotNull InventoryButtonContext context, @NotNull LimitedInventoryScreen screen, @NotNull ClientPlayerEntity player) {
            GuesserPlayerWidget.selectedPlayer = null;
            GuesserRoleWidget.stopClosing = false;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) {
                return;
            }
            int y = (context.height() - 32) / 2 + 105;
            List<AbstractClientPlayerEntity> targets = new ArrayList<>(client.world.getPlayers());
            targets.removeIf(target -> target.getUuid().equals(player.getUuid()));
            for (AbstractClientPlayerEntity target : targets) {
                GuesserPlayerWidget widget = new GuesserPlayerWidget(screen, 0, y, target);
                widget.visible = false;
                widget.active = false;
                this.buttons.addWidget(context, widget);
            }
            this.roleWidget = context.addWidget(NoellesInventoryButtonSupport.id("inventory_group/guesser_role"), new GuesserRoleWidget(screen, context.textRenderer(), context.width() / 2 - 100, y));
            this.roleWidget.visible = false;
            this.roleWidget.active = false;
        }

        @Override
        public void tick(@NotNull InventoryButtonContext context) {
            super.tick(context);
            this.refreshRoleWidget();
        }

        @Override
        public void render(@NotNull InventoryButtonContext context, @NotNull DrawContext drawContext, int mouseX, int mouseY, float delta) {
            this.refreshRoleWidget();
        }

        @Override
        public boolean allowInventoryKeyClose(@NotNull InventoryButtonContext context, int keyCode, int scanCode) {
            return GuesserPlayerWidget.selectedPlayer == null;
        }

        @Override
        public void close(@NotNull InventoryButtonContext context) {
            GuesserRoleWidget.stopClosing = false;
        }

        private void refreshRoleWidget() {
            if (this.roleWidget != null) {
                boolean rolePhase = GuesserPlayerWidget.selectedPlayer != null;
                GuesserRoleWidget.stopClosing = rolePhase;
                this.roleWidget.visible = rolePhase;
                this.roleWidget.active = rolePhase;
            }
        }
    }
}
