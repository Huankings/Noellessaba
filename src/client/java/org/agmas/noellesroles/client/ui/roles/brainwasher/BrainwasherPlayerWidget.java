package org.agmas.noellesroles.client.ui.roles.brainwasher;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;
import org.agmas.noellesroles.packet.role.brainwasher.BrainwasherC2SPacket;

import java.awt.*;
import java.util.UUID;

public class BrainwasherPlayerWidget extends ButtonWidget {
    private final LimitedInventoryScreen screen;
    private final UUID targetUuid;
    private final PlayerListEntry entry;

    public BrainwasherPlayerWidget(LimitedInventoryScreen screen, int x, int y, UUID targetUuid, PlayerListEntry entry) {
        super(x, y, 16, 16, Text.literal(entry.getProfile().getName()),
                button -> {
                    var client = MinecraftClient.getInstance();
                    if (client.player == null) return;
                    var ability = AbilityPlayerComponent.KEY.get(client.player);
                    if (ability.cooldown == 0) {
                        ClientPlayNetworking.send(new BrainwasherC2SPacket(targetUuid));
                    }
                },
                DEFAULT_NARRATION_SUPPLIER);
        this.screen = screen;
        this.targetUuid = targetUuid;
        this.entry = entry;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;
        var ability = AbilityPlayerComponent.KEY.get(client.player);
        boolean cooling = ability.cooldown > 0;

        if (cooling) {
            context.setShaderColor(0.25f, 0.25f, 0.25f, 0.5f);
        }
        context.drawGuiTexture(ShopEntry.Type.POISON.getTexture(), getX() - 7, getY() - 7, 30, 30);
        PlayerSkinDrawer.draw(context, PlayerHeadTextureHelper.resolveStableSkinTextures(targetUuid, entry).texture(), getX(), getY(), 16);

        if (cooling) {
            context.setShaderColor(1f, 1f, 1f, 1f);
            String seconds = String.valueOf(ability.cooldown / 20);
            context.drawText(client.textRenderer, seconds, getX(), getY(), Color.RED.getRGB(), true);
        }

        if (isHovered()) {
            drawHighlight(context);
            context.drawTooltip(client.textRenderer, getMessage(),
                    getX() - 4 - client.textRenderer.getWidth(getMessage()) / 2, getY() - 9);
        }
    }

    private void drawHighlight(DrawContext context) {
        int color = -1862287543;
        int x = getX(), y = getY();
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, 0);
    }
}
