package org.agmas.noellesroles.client.ui.roles.controller;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
import org.agmas.noellesroles.packet.role.controller.ControllerPossessC2SPacket;
import org.agmas.noellesroles.packet.role.controller.ControllerReleaseC2SPacket;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class ControllerWidget extends ButtonWidget {
    public final LimitedInventoryScreen screen;
    public final AbstractClientPlayerEntity targetEntity;
    public final boolean isSelf;

    public ControllerWidget(LimitedInventoryScreen screen, int x, int y,
                            @NotNull AbstractClientPlayerEntity targetEntity,
                            int index, boolean isSelf) {
        super(x, y, 16, 16, targetEntity.getName(),
                (button) -> {
                    ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
                    if (isSelf) {
                        if (controllerComp.controlledTarget != null && controllerComp.possessTicks > 0) {
                            ClientPlayNetworking.send(new ControllerReleaseC2SPacket());
                        }
                        return;
                    }
                    if (controllerComp.possessTicks < 0) return;
                    if (controllerComp.controlledTarget != null && controllerComp.possessTicks > 0) return;
                    ClientPlayNetworking.send(new ControllerPossessC2SPacket(targetEntity.getUuid()));
                },
                DEFAULT_NARRATION_SUPPLIER);
        this.screen = screen;
        this.targetEntity = targetEntity;
        this.isSelf = isSelf;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        boolean isPossessing = controllerComp.controlledTarget != null && controllerComp.possessTicks > 0;
        boolean isCurrentTarget = controllerComp.controlledTarget != null &&
                controllerComp.controlledTarget.equals(targetEntity.getUuid()) && !isSelf;
        var headTexture = PlayerHeadTextureHelper.resolveStableSkinTextures(targetEntity.getUuid(), null).texture();

        if (controllerComp.possessTicks < 0) {
            context.setShaderColor(0.25f, 0.25f, 0.25f, 0.5f);
        } else if (isCurrentTarget) {
            context.setShaderColor(0.9f, 0.6f, 1.0f, 1.0f);
        } else if (isSelf && isPossessing) {
            context.setShaderColor(1.0f, 0.8f, 0.4f, 1.0f);
        }

        super.renderWidget(context, mouseX, mouseY, delta);

        if (isSelf) {
            context.drawGuiTexture(ShopEntry.Type.TOOL.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        } else {
            context.drawGuiTexture(ShopEntry.Type.POISON.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        }

        // 背包头像必须固定读取原始皮肤缓存，不能跟随实时伪装后的实体皮肤变化。
        PlayerSkinDrawer.draw(context, headTexture, this.getX(), this.getY(), 16);

        if (isSelf && isPossessing) {
            drawSelfBorder(context, this.getX(), this.getY());
        }

        if (this.isHovered()) {
            this.drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
            Text name = targetEntity.getName();
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, name,
                    this.getX() - 4 - MinecraftClient.getInstance().textRenderer.getWidth(name) / 2, this.getY() - 9);
            if (isSelf && isPossessing) {
                Text removeHint = Text.translatable("ui.controller.remove_possess_hint");
                context.drawTooltip(MinecraftClient.getInstance().textRenderer, removeHint,
                        this.getX() + 20, this.getY() - 9);
            }
        }

        context.setShaderColor(1f, 1f, 1f, 1f);

        if (controllerComp.possessTicks < 0) {
            int cooldownSeconds = -controllerComp.possessTicks / 20;
            context.drawText(MinecraftClient.getInstance().textRenderer,
                    String.valueOf(cooldownSeconds), this.getX(), this.getY(), Color.RED.getRGB(), true);
        }
    }

    private void drawSelfBorder(DrawContext context, int x, int y) {
        int borderColor = new Color(255, 165, 0, 200).getRGB();
        context.fill(x - 2, y - 2, x + 18, y, borderColor);
        context.fill(x - 2, y + 16, x + 18, y + 18, borderColor);
        context.fill(x - 2, y - 2, x, y + 18, borderColor);
        context.fill(x + 16, y - 2, x + 18, y + 18, borderColor);
    }

    private void drawShopSlotHighlight(DrawContext context, int x, int y, int z) {
        int color = -1862287543;
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, z);
    }

    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {}
}
