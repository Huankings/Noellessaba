package org.agmas.noellesroles.client.ui.roles.coroner;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.agmas.noellesroles.packet.role.morphling.MorphC2SPacket;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.UUID;

public class CoronerPlayerWidget extends ButtonWidget {
    public final LimitedInventoryScreen screen;
    public final UUID targetUuid;
    @Nullable
    public final PlayerListEntry targetPlayerEntry;
    @Nullable
    public final AbstractClientPlayerEntity playerEntity;
    public final boolean isSelf; // 新增：标记是否是自己的头像

    public CoronerPlayerWidget(LimitedInventoryScreen screen, int x, int y, UUID targetUuid,
                               @Nullable PlayerListEntry targetPlayerEntry,
                               @Nullable AbstractClientPlayerEntity playerEntity, int index, boolean isSelf) {
        super(x, y, 16, 16,
                getDisplayNameText(playerEntity, targetPlayerEntry,isSelf),
                (a) -> {
                    CoronerPlayerComponent coronerComp = CoronerPlayerComponent.KEY.get(MinecraftClient.getInstance().player);

                    if (isSelf) {
                        // 点击自己头像：卸除伪装
                        if (coronerComp.disguise != null && coronerComp.getMorphTicks() > 0) {
                            // 如果正在伪装中，发送一个特殊的数据包来表示卸除伪装
                            // 我们可以发送一个null UUID或者特殊值，这里选择发送自己的UUID作为卸除信号
                            ClientPlayNetworking.send(new MorphC2SPacket(MinecraftClient.getInstance().player.getUuid()));
                        }
                        // 如果不在伪装中，点击自己头像没有效果
                        return;
                    }

                    // 检查目标玩家是否为旁观模式
                //    boolean isTargetSpectator = false;
                 //   if (playerEntity != null) {
                 //       isTargetSpectator = playerEntity.isSpectator();
                //    } else if (targetPlayerEntry != null) {
                 //       isTargetSpectator = targetPlayerEntry.getGameMode() == net.minecraft.world.GameMode.SPECTATOR;
                 //   }

                 //   if (!isTargetSpectator) {
                 //       // 目标不是旁观者（存活玩家），发送提示并拒绝变形
                 //       MinecraftClient.getInstance().player.sendMessage(
                 //               Text.literal("§c只能变形死亡玩家").formatted(Formatting.RED), true);
                 //       return;
                 //   }


                    // 检查是否在冷却中
                    if (coronerComp.getMorphTicks() < 0) {
                        return; // 冷却期间不能点击
                    }



                    // 发送变形请求
                    ClientPlayNetworking.send(new MorphC2SPacket(targetUuid));
                },
                DEFAULT_NARRATION_SUPPLIER);
        this.screen = screen;
        this.targetUuid = targetUuid;
        this.targetPlayerEntry = targetPlayerEntry;
        this.playerEntity = playerEntity;
        this.isSelf = isSelf;
    }

    private static Text getDisplayNameText(@Nullable AbstractClientPlayerEntity playerEntity,
                                           @Nullable PlayerListEntry targetPlayerEntry,
                                           boolean isSelf) {


        if (playerEntity != null) {
            return playerEntity.getName();
        } else if (targetPlayerEntry != null) {
            return Text.literal(targetPlayerEntry.getProfile().getName());
        } else {
            return Text.literal("Unknown");
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        CoronerPlayerComponent coronerComp = CoronerPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        var headTexture = PlayerHeadTextureHelper.resolveStableSkinTextures(targetUuid, targetPlayerEntry).texture();

        // 判断是否正在伪装中
        boolean isDisguised = coronerComp.disguise != null && coronerComp.getMorphTicks() > 0;

        // 判断是否是当前伪装目标
        boolean isCurrentDisguise = coronerComp.disguise != null &&
                coronerComp.disguise.equals(targetUuid) &&
                !isSelf; // 自己头像不显示为当前伪装
        // 根据状态调整颜色
        if (coronerComp.getMorphTicks() < 0) {
            // 冷却状态
            context.setShaderColor(0.25f, 0.25f, 0.25f, 0.5f);
        } else if (isCurrentDisguise && coronerComp.getMorphTicks() > 0) {
            // 当前伪装目标
            context.setShaderColor(0.8f, 1.0f, 0.8f, 1.0f);
        } else if (isSelf) {
            // 自己头像特殊颜色
            if (isDisguised) {
                // 如果正在伪装中，自己头像用橙色表示可以卸除
                context.setShaderColor(1.0f, 0.8f, 0.4f, 1.0f);
            } else {
                // 如果没有伪装，正常显示
                context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }

        super.renderWidget(context, mouseX, mouseY, delta);

        if (isSelf) {
            context.drawGuiTexture(ShopEntry.Type.TOOL.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        } else {
            context.drawGuiTexture(ShopEntry.Type.POISON.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        }

        context.drawGuiTexture(ShopEntry.Type.POISON.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);

        // 背包头像一律固定显示原始头像，不能让实时伪装状态反向污染选人界面。
        PlayerSkinDrawer.draw(context, headTexture, this.getX(), this.getY(), 16);

        // 如果是自己头像且正在伪装中，绘制一个特殊边框
        if (isSelf && isDisguised) {
            drawSelfBorder(context, this.getX(), this.getY());
        }

        // 鼠标悬停效果
        if (this.isHovered()) {
            this.drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
            Text name = getDisplayNameText(playerEntity, targetPlayerEntry,isSelf);
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, name,
                    this.getX() - 4 - MinecraftClient.getInstance().textRenderer.getWidth(name) / 2,
                    this.getY() - 9);

            // 如果是自己头像且正在伪装中，显示提示
            if (isSelf && isDisguised) {
                Text removeHint = Text.translatable("ui.coroner.remove_disguise_hint");
                context.drawTooltip(MinecraftClient.getInstance().textRenderer, removeHint,
                        this.getX() + 20, this.getY() - 9);
            }
        }

        // 恢复颜色
        context.setShaderColor(1f, 1f, 1f, 1f);

        // 如果冷却中，显示冷却时间
        if (coronerComp.getMorphTicks() < 0) {
            int cooldownSeconds = -coronerComp.getMorphTicks() / 20;
            context.drawText(MinecraftClient.getInstance().textRenderer,
                    String.valueOf(cooldownSeconds),
                    this.getX(), this.getY(), Color.RED.getRGB(), true);
        }
    }

    // 绘制自己头像的特殊边框
    private void drawSelfBorder(DrawContext context, int x, int y) {
        // 绘制一个橙色的边框
        int borderColor = new Color(255, 165, 0, 200).getRGB(); // 橙色，半透明

        // 上边框
        context.fill(x - 2, y - 2, x + 18, y, borderColor);
        // 下边框
        context.fill(x - 2, y + 16, x + 18, y + 18, borderColor);
        // 左边框
        context.fill(x - 2, y - 2, x, y + 18, borderColor);
        // 右边框
        context.fill(x + 16, y - 2, x + 18, y + 18, borderColor);
    }

    private void drawShopSlotHighlight(DrawContext context, int x, int y, int z) {
        int color = -1862287543;
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, z);
    }

    public void drawMessage(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int color) {
        // 留空
    }
}
