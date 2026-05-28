package org.agmas.noellesroles.client.ui.roles.corpsemaker;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;

import java.awt.*;
import java.util.UUID;

public class CorpsemakerPlayerWidget extends ButtonWidget {
    private final LimitedInventoryScreen screen;
    private final UUID targetUuid;
    private final PlayerListEntry entry;

    public CorpsemakerPlayerWidget(LimitedInventoryScreen screen, int x, int y,
                                   UUID targetUuid, PlayerListEntry entry) {
        super(x, y, 16, 16, Text.literal(entry.getProfile().getName()),
                (button) -> {
                    if (MinecraftClient.getInstance().player == null) return;
                    AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
                    if (ability.cooldown <= 0) {
                        CorpsemakerState.selectedPlayerUuid = targetUuid;
                        CorpsemakerState.phase = CorpsemakerPhase.DEATH_REASON;
                    } else {
                        MinecraftClient.getInstance().player.sendMessage(
                                Text.translatable("tip.noellesroles.cooldown", ability.cooldown / 20), true);
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
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(client.player);
        boolean cooling = ability.cooldown > 0;

        if (cooling) {
            context.setShaderColor(0.25f, 0.25f, 0.25f, 0.5f);
        }

        context.drawGuiTexture(ShopEntry.Type.WEAPON.getTexture(), getX() - 7, getY() - 7, 30, 30);
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
