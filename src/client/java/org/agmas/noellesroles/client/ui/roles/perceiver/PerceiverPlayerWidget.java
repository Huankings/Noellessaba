package org.agmas.noellesroles.client.ui.roles.perceiver;

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
import org.agmas.noellesroles.packet.role.perceiver.PerceiverSelectC2SPacket;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.UUID;

/** 感知者背包页中的单个玩家头像按钮。 */
public final class PerceiverPlayerWidget extends ButtonWidget {
    private final UUID targetUuid;
    @Nullable
    private final PlayerListEntry targetPlayerEntry;

    public PerceiverPlayerWidget(
            LimitedInventoryScreen screen,
            int x,
            int y,
            UUID targetUuid,
            @Nullable PlayerListEntry targetPlayerEntry
    ) {
        super(x, y, 16, 16, Text.empty(), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || AbilityPlayerComponent.KEY.get(client.player).cooldown > 0) {
                return;
            }
            ClientPlayNetworking.send(new PerceiverSelectC2SPacket(targetUuid));
        }, DEFAULT_NARRATION_SUPPLIER);
        this.targetUuid = targetUuid;
        this.targetPlayerEntry = targetPlayerEntry;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(client.player);
        boolean usable = ability.cooldown <= 0;

        // 先绘制 ButtonWidget 自己的基础状态；冷却灰度只作用于头像槽和玩家头像，
        // 这样不会被 ButtonWidget 内部绘制流程重置 shader，也与 Swapper 等按钮保持一致。
        super.renderWidget(context, mouseX, mouseY, delta);

        if (!usable) {
            context.setShaderColor(0.25F, 0.25F, 0.25F, 0.5F);
        }

        context.drawGuiTexture(ShopEntry.Type.TOOL.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        PlayerSkinDrawer.draw(
                context,
                PlayerHeadTextureHelper.resolveStableSkinTextures(this.targetUuid, this.targetPlayerEntry).texture(),
                this.getX(),
                this.getY(),
                16
        );

        // 后续 tooltip 和冷却数字必须恢复正常颜色，否则会继承按钮的灰色 shader。
        if (!usable) {
            context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (this.isHovered()) {
            drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
            Text tooltip = this.targetPlayerEntry == null
                    ? Text.literal(this.targetUuid.toString().substring(0, 8))
                    : Text.literal(this.targetPlayerEntry.getProfile().getName());
            context.drawTooltip(
                    client.textRenderer,
                    tooltip,
                    this.getX() - 4 - client.textRenderer.getWidth(tooltip) / 2,
                    this.getY() - 9
            );
        }

        if (!usable) {
            int remainingSeconds = (ability.cooldown + 19) / 20;
            context.drawText(client.textRenderer, remainingSeconds + "s", this.getX(), this.getY(), Color.RED.getRGB(), true);
        }
    }

    private static void drawShopSlotHighlight(DrawContext context, int x, int y, int z) {
        int color = -1862287543;
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, z);
    }
}
