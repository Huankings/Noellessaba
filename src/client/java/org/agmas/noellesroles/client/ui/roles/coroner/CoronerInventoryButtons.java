package org.agmas.noellesroles.client.ui.roles.coroner;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.UUID;

public final class CoronerInventoryButtons {
    private CoronerInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("coroner", CoronerInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), NoellesRoleRegistry.CORONER) ? new Extension() : null;
    }

    private static final class Extension extends NoellesInventoryButtonSupport.PagedExtension<CoronerPlayerWidget> {
        private Extension() {
            super("coroner");
        }

        @Override
        protected void populate(@NotNull InventoryButtonContext context, @NotNull LimitedInventoryScreen screen, @NotNull ClientPlayerEntity player) {
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            List<UUID> uuids = NoellesInventoryButtonSupport.onlineUuids(player);
            for (int i = 0; i < uuids.size(); i++) {
                UUID targetUuid = uuids.get(i);
                this.buttons.addWidget(context, new CoronerPlayerWidget(
                        screen,
                        0,
                        y,
                        targetUuid,
                        NoellesInventoryButtonSupport.entry(player, targetUuid),
                        NoellesInventoryButtonSupport.clientEntity(targetUuid),
                        i,
                        targetUuid.equals(player.getUuid())
                ));
            }
        }

        @Override
        public void render(@NotNull InventoryButtonContext context, @NotNull DrawContext drawContext, int mouseX, int mouseY, float delta) {
            ClientPlayerEntity player = context.requirePlayer();
            CoronerPlayerComponent coroner = CoronerPlayerComponent.KEY.get(player);
            if (coroner.getMorphTicks() == 0) {
                return;
            }
            int x = context.width() / 2;
            int y = (context.height() - 32) / 2;
            if (coroner.getMorphTicks() > 0) {
                Text status = Text.translatable("ui.coroner.morphing", coroner.getMorphTicks() / 20);
                drawContext.drawTextWithShadow(context.textRenderer(), status, x - context.textRenderer().getWidth(status) / 2, y + 40, Color.GREEN.getRGB());
                if (coroner.disguise != null) {
                    PlayerEntity disguisePlayer = player.getWorld().getPlayerByUuid(coroner.disguise);
                    String disguiseName = disguisePlayer != null ? disguisePlayer.getName().getString() : "Unknown";
                    Text disguiseText = Text.translatable("ui.coroner.disguised_as", disguiseName);
                    drawContext.drawTextWithShadow(context.textRenderer(), disguiseText, x - context.textRenderer().getWidth(disguiseText) / 2, y + 55, Color.YELLOW.getRGB());
                }
            } else {
                Text cooldown = Text.translatable("ui.coroner.cooldown", -coroner.getMorphTicks() / 20);
                drawContext.drawTextWithShadow(context.textRenderer(), cooldown, x - context.textRenderer().getWidth(cooldown) / 2, y + 40, Color.RED.getRGB());
            }
        }
    }
}
