package org.agmas.noellesroles.client.ui.roles.party_animal;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.agmas.noellesroles.packet.role.party_animal.PartyAnimalTypeC2SPacket;
import dev.doctor4t.wathe.util.ShopEntry;

/** 派对狂升调/降调图标按钮。 */
final class PartyAnimalTypeWidget extends ButtonWidget {
    private final ItemStack icon;
    private final Text tooltip;

    PartyAnimalTypeWidget(LimitedInventoryScreen screen, int x, int y, ItemStack icon, int type, Text tooltip) {
        super(x, y, 16, 16, Text.empty(), button -> ClientPlayNetworking.send(new PartyAnimalTypeC2SPacket(type)), DEFAULT_NARRATION_SUPPLIER);
        this.icon = icon;
        this.tooltip = tooltip;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Wathe 标准商店槽位背景：30×30，围绕 16×16 物品图标向外扩展 7 格。
        context.drawGuiTexture(ShopEntry.Type.TOOL.getTexture(), getX() - 7, getY() - 7, 30, 30);
        context.drawItem(icon, getX(), getY());
        if (isHovered()) {
            drawShopSlotHighlight(context, getX(), getY());
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private static void drawShopSlotHighlight(DrawContext context, int x, int y) {
        int color = -1862287543;
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, 0);
    }
}
