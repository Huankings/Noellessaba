package org.agmas.noellesroles.client.ui.roles.magician;

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
import org.agmas.noellesroles.roles.magician.MagicianPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 魔术师背包里的头像选择按钮。
 *
 * <p>这里直接沿用风灵师那套头像交互思路：
 * 点击头像就切换当前皮套目标，本身不附带任何额外冷却。</p>
 */
public class MagicianPlayerWidget extends ButtonWidget {
    public final LimitedInventoryScreen screen;
    public final UUID targetUuid;
    @Nullable
    public final PlayerListEntry targetPlayerEntry;

    public MagicianPlayerWidget(
            LimitedInventoryScreen screen,
            int x,
            int y,
            UUID targetUuid,
            @Nullable PlayerListEntry targetPlayerEntry
    ) {
        super(
                x,
                y,
                16,
                16,
                Text.literal(""),
                button -> ClientPlayNetworking.send(new MorphC2SPacket(targetUuid)),
                DEFAULT_NARRATION_SUPPLIER
        );
        this.screen = screen;
        this.targetUuid = targetUuid;
        this.targetPlayerEntry = targetPlayerEntry;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);

        MagicianPlayerComponent component = MagicianPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
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
            // 只保留高亮，不额外叠文字，避免遮挡背包界面原有物品说明。
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
