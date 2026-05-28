package org.agmas.noellesroles.client.ui.roles.morphling;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.packet.role.morphling.MorphC2SPacket;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.UUID;

public class MorphlingPlayerWidget extends ButtonWidget{
    public final LimitedInventoryScreen screen;
    public final UUID targetUuid;
    @Nullable
    public final PlayerListEntry targetPlayerEntry;  // 改为PlayerListEntry
    @Nullable
    public final AbstractClientPlayerEntity playerEntity;

    public MorphlingPlayerWidget(LimitedInventoryScreen screen, int x, int y, UUID targetUuid, @Nullable PlayerListEntry targetPlayerEntry, @Nullable AbstractClientPlayerEntity playerEntity,int index) {
        super(x, y, 16, 16, playerEntity != null ?
                playerEntity.getName() :
                        (targetPlayerEntry != null ?
                                Text.literal(targetPlayerEntry.getProfile().getName()) :
                                Text.literal("Unknown")),
                (a) -> {
                    if ((MorphlingPlayerComponent.KEY.get(MinecraftClient.getInstance().player)).getMorphTicks() == 0) {
                        // 只有点到玩家头像按钮时才会发变形包。
                        // 分页按钮不复用这个类，因此翻页不会误触发变形。
                        ClientPlayNetworking.send(new MorphC2SPacket(targetUuid));
                    }
                },
                DEFAULT_NARRATION_SUPPLIER);
        this.screen = screen;
        this.targetUuid = targetUuid;
        this.targetPlayerEntry = targetPlayerEntry;
        this.playerEntity = playerEntity;
    }

    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MorphlingPlayerComponent morphComp = MorphlingPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        var headTexture = PlayerHeadTextureHelper.resolveStableSkinTextures(targetUuid, targetPlayerEntry).texture();

        if (morphComp.getMorphTicks() == 0) {
            super.renderWidget(context, mouseX, mouseY, delta);
            context.drawGuiTexture(ShopEntry.Type.POISON.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
            // 这里固定显示“目标玩家自己的原始头像”，避免别人正在变形时把伪装皮肤泄露到背包 UI。
            PlayerSkinDrawer.draw(context, headTexture, this.getX(), this.getY(), 16);
            if (this.isHovered()) {
                this.drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
                Text name = getDisplayName();

                        context.drawTooltip(MinecraftClient.getInstance().textRenderer, name, this.getX() - 4 - MinecraftClient.getInstance().textRenderer.getWidth(name) / 2, this.getY() - 9);
            }

        }

        if (morphComp.getMorphTicks() < 0) {
            super.renderWidget(context, mouseX, mouseY, delta);
            context.setShaderColor(0.25f,0.25f,0.25f,0.5f);
            context.drawGuiTexture(ShopEntry.Type.POISON.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);

            // 冷却期间也继续使用稳定头像来源，避免 UI 在职业状态切换时出现“头像忽然变脸”。
            PlayerSkinDrawer.draw(context, headTexture, this.getX(), this.getY(), 16);
            if (this.isHovered()) {
                this.drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
                Text name = getDisplayName();
                        context.drawTooltip(MinecraftClient.getInstance().textRenderer, name, this.getX() - 4 - MinecraftClient.getInstance().textRenderer.getWidth(name) / 2, this.getY() - 9);
            }


            context.setShaderColor(1f,1f,1f,1f);
            context.drawText(MinecraftClient.getInstance().textRenderer, -MorphlingPlayerComponent.KEY.get(MinecraftClient.getInstance().player).getMorphTicks()/20+"",this.getX(),this.getY(), Color.RED.getRGB(),true);

        }

    }
    private Text getDisplayName() {
        if (playerEntity != null) {
            return playerEntity.getName();
        } else if (targetPlayerEntry != null) {
            return Text.literal(targetPlayerEntry.getProfile().getName());
        } else {
            return Text.literal("Unknown");
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
