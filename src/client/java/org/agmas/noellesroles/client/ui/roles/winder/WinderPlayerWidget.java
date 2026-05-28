package org.agmas.noellesroles.client.ui.roles.winder;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;
import org.agmas.noellesroles.packet.role.morphling.MorphC2SPacket;
import org.agmas.noellesroles.roles.winder.WinderPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 风灵师背包里的头像按钮。
 * 整体交互和巫毒师尽量保持一致，但不会有选择冷却。
 */
public class WinderPlayerWidget extends ButtonWidget {
    public final LimitedInventoryScreen screen;
    public final UUID targetUuid;
    @Nullable
    public final PlayerListEntry targetPlayerEntry;

    public WinderPlayerWidget(LimitedInventoryScreen screen, int x, int y, UUID targetUuid, @Nullable PlayerListEntry targetPlayerEntry) {
        super(x, y, 16, 16, Text.literal(""), button -> {
            ClientPlayNetworking.send(new MorphC2SPacket(targetUuid));
        }, DEFAULT_NARRATION_SUPPLIER);
        this.screen = screen;
        this.targetUuid = targetUuid;
        this.targetPlayerEntry = targetPlayerEntry;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);

        WinderPlayerComponent component = WinderPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        boolean selected = component.getSelectedTarget().equals(this.targetUuid);

        context.drawGuiTexture(ShopEntry.Type.TOOL.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        PlayerSkinDrawer.draw(
                context,
                PlayerHeadTextureHelper.resolveStableSkinTextures(this.targetUuid, this.targetPlayerEntry).texture(),
                this.getX(),
                this.getY(),
                16
        );

        if (selected) {
            // 这里不再额外渲染“已选中”提示文本。
            // 原因是 Wathe/KinsWathe 的物品说明是在界面固定位置统一绘制，
            // 若这里再由头像组件单独绘制 tooltip，会因为渲染层级更靠前而遮挡物品描述。
            // 因此只保留选中时的金色覆盖高亮，避免影响玩家查看商店物品说明。
            this.drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
        } else if (this.isHovered()) {
            Text name = this.targetPlayerEntry != null
                    ? Text.literal(this.targetPlayerEntry.getProfile().getName())
                    : Text.literal("未知玩家");
            this.drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
            context.drawTooltip(
                    MinecraftClient.getInstance().textRenderer,
                    name,
                    this.getX() - 4 - MinecraftClient.getInstance().textRenderer.getWidth(name) / 2,
                    this.getY() - 9
            );
        }
    }

    private void drawShopSlotHighlight(DrawContext context, int x, int y, int z) {
        int color = -1862287543;
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, z);
    }

    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
    }
}
