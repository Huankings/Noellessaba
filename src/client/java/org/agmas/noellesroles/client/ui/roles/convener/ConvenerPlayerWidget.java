package org.agmas.noellesroles.client.ui.roles.convener;

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
import org.agmas.noellesroles.client.roles.convener.ConvenerDisguiseResolver;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;
import org.agmas.noellesroles.roles.convener.ConvenerDisguiseComponent;
import org.agmas.noellesroles.packet.role.convener.ConvenerMorphC2SPacket;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 召集者背包里的伪装头像按钮。
 */
public class ConvenerPlayerWidget extends ButtonWidget {
    private static final int SLOT_HIGHLIGHT = 0x90FFBF49;
    private static final int SELF_BORDER = 0xC0F2B95B;
    private static final int CURRENT_BORDER = 0xD05734E5;

    private final LimitedInventoryScreen screen;
    private final UUID targetUuid;
    private final boolean self;
    private final @Nullable PlayerListEntry playerListEntry;

    public ConvenerPlayerWidget(
            LimitedInventoryScreen screen,
            int x,
            int y,
            UUID targetUuid,
            boolean self,
            @Nullable PlayerListEntry playerListEntry
    ) {
        super(x, y, 16, 16, Text.empty(), button -> ClientPlayNetworking.send(new ConvenerMorphC2SPacket(targetUuid)), DEFAULT_NARRATION_SUPPLIER);
        this.screen = screen;
        this.targetUuid = targetUuid;
        this.self = self;
        this.playerListEntry = playerListEntry;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(client.player);
        boolean selected = disguise.isDisguised() && this.targetUuid.equals(disguise.getDisguiseUuid());

        context.drawGuiTexture(
                this.self ? ShopEntry.Type.TOOL.getTexture() : ShopEntry.Type.POISON.getTexture(),
                this.getX() - 7,
                this.getY() - 7,
                30,
                30
        );
        PlayerSkinDrawer.draw(
                context,
                PlayerHeadTextureHelper.resolveStableSkinTextures(this.targetUuid, this.playerListEntry).texture(),
                this.getX(),
                this.getY(),
                16
        );

        if (this.isHovered()) {
            drawSlotHighlight(context);
            Text hoverText = getDisplayName();
            context.drawTooltip(
                    client.textRenderer,
                    hoverText,
                    this.getX() - 4 - client.textRenderer.getWidth(hoverText) / 2,
                    this.getY() - 10
            );
        }

        if (this.self) {
            drawBorder(context, SELF_BORDER);
        }
        if (selected) {
            drawBorder(context, CURRENT_BORDER);
        }
    }

    private Text getDisplayName() {
        if (MinecraftClient.getInstance().player != null) {
            Text resolved = ConvenerDisguiseResolver.resolveDisguiseName(MinecraftClient.getInstance().player, this.targetUuid);
            if (resolved != null) {
                return resolved;
            }
        }
        return Text.literal(this.targetUuid.toString());
    }

    private void drawBorder(DrawContext context, int color) {
        int x = this.getX();
        int y = this.getY();
        context.fill(x - 2, y - 2, x + 18, y, color);
        context.fill(x - 2, y + 16, x + 18, y + 18, color);
        context.fill(x - 2, y - 2, x, y + 18, color);
        context.fill(x + 16, y - 2, x + 18, y + 18, color);
    }

    private void drawSlotHighlight(DrawContext context) {
        int x = this.getX();
        int y = this.getY();
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, 0);
    }
}
