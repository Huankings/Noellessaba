package org.agmas.noellesroles.client.ui.roles.controller;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class ControllerInventoryButtons {
    private ControllerInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("controller", ControllerInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), NoellesRoleRegistry.CONTROLLER) ? new Extension() : null;
    }

    private static final class Extension extends NoellesInventoryButtonSupport.PagedExtension<ControllerWidget> {
        private Extension() {
            super("controller");
        }

        @Override
        protected void populate(@NotNull InventoryButtonContext context, @NotNull LimitedInventoryScreen screen, @NotNull ClientPlayerEntity player) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) {
                return;
            }
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            List<AbstractClientPlayerEntity> players = new ArrayList<>(client.world.getPlayers());
            for (int i = 0; i < players.size(); i++) {
                AbstractClientPlayerEntity target = players.get(i);
                this.buttons.addWidget(context, new ControllerWidget(screen, 0, y, target, i, target.getUuid().equals(player.getUuid())));
            }
        }

        @Override
        public void render(@NotNull InventoryButtonContext context, @NotNull DrawContext drawContext, int mouseX, int mouseY, float delta) {
            ClientPlayerEntity player = context.requirePlayer();
            ControllerPlayerComponent controller = ControllerPlayerComponent.KEY.get(player);
            if (controller.possessTicks == 0) {
                return;
            }
            int x = context.width() / 2;
            int y = (context.height() - 32) / 2;
            if (controller.possessTicks > 0) {
                Text status = Text.translatable("ui.controller.morphing", controller.possessTicks / 20);
                drawContext.drawTextWithShadow(context.textRenderer(), status, x - context.textRenderer().getWidth(status) / 2, y + 40, Color.MAGENTA.getRGB());
                if (controller.controlledTarget != null) {
                    PlayerEntity targetPlayer = player.getWorld().getPlayerByUuid(controller.controlledTarget);
                    String targetName = targetPlayer != null ? targetPlayer.getName().getString() : "Unknown";
                    Text targetText = Text.translatable("ui.controller.disguised_as", targetName);
                    drawContext.drawTextWithShadow(context.textRenderer(), targetText, x - context.textRenderer().getWidth(targetText) / 2, y + 55, Color.YELLOW.getRGB());
                }
            } else {
                Text cooldown = Text.translatable("ui.controller.cooldown", -controller.possessTicks / 20);
                drawContext.drawTextWithShadow(context.textRenderer(), cooldown, x - context.textRenderer().getWidth(cooldown) / 2, y + 40, Color.RED.getRGB());
            }
        }
    }
}
