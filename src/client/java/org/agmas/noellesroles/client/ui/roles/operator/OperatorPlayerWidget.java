package org.agmas.noellesroles.client.ui.roles.operator;

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
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;
import org.agmas.noellesroles.packet.role.operator.OperatorC2SPacket;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.UUID;

/**
 * 接线员背包页里的玩家头像按钮。
 *
 * <p>交互规则刻意与交换者保持一致：</p>
 * <p>1. 第一次点击只记录第一名玩家；</p>
 * <p>2. 第二次点击才真正向服务端发包；</p>
 * <p>3. 如果两次点的是同一人，服务端会把它解释成“广播”。</p>
 */
public class OperatorPlayerWidget extends ButtonWidget {
    public static UUID firstChoice = null;

    public final LimitedInventoryScreen screen;
    public final UUID targetUuid;
    @Nullable
    public final PlayerListEntry targetPlayerEntry;

    public OperatorPlayerWidget(LimitedInventoryScreen screen, int x, int y, UUID targetUuid, @Nullable PlayerListEntry targetPlayerEntry) {
        super(x, y, 16, 16, Text.literal(""), button -> {
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
            if (ability.cooldown > 0) {
                return;
            }

            if (firstChoice != null) {
                ClientPlayNetworking.send(new OperatorC2SPacket(firstChoice, targetUuid));
                firstChoice = null;
            } else {
                firstChoice = targetUuid;
            }
        }, DEFAULT_NARRATION_SUPPLIER);
        this.screen = screen;
        this.targetUuid = targetUuid;
        this.targetPlayerEntry = targetPlayerEntry;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        boolean usable = ability.cooldown == 0;

        if (!usable) {
            context.setShaderColor(0.25f, 0.25f, 0.25f, 0.5f);
        }

        context.drawGuiTexture(ShopEntry.Type.TOOL.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        PlayerSkinDrawer.draw(
                context,
                PlayerHeadTextureHelper.resolveStableSkinTextures(this.targetUuid, this.targetPlayerEntry).texture(),
                this.getX(),
                this.getY(),
                16
        );

        boolean selectedAsFirst = this.targetUuid.equals(firstChoice);
        if (selectedAsFirst || this.isHovered()) {
            this.drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
            Text tooltip = this.targetPlayerEntry != null
                    ? Text.literal(this.targetPlayerEntry.getProfile().getName())
                    : Text.literal(this.targetUuid.toString().substring(0, 8));
            context.drawTooltip(
                    MinecraftClient.getInstance().textRenderer,
                    tooltip,
                    this.getX() - 4 - MinecraftClient.getInstance().textRenderer.getWidth(tooltip) / 2,
                    this.getY() - 9
            );
        }

        if (!usable) {
            context.setShaderColor(1f, 1f, 1f, 1f);
            context.drawText(
                    MinecraftClient.getInstance().textRenderer,
                    ability.cooldown / 20 + "",
                    this.getX(),
                    this.getY(),
                    Color.RED.getRGB(),
                    true
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
