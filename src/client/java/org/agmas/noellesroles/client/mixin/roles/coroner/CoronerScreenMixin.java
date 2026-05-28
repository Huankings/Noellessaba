package org.agmas.noellesroles.client.mixin.roles.coroner;


import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.ui.common.PagedPlayerScreenState;
import org.agmas.noellesroles.client.ui.common.PlayerPageLayout;
import org.agmas.noellesroles.client.ui.common.PlayerPageSwitchWidget;
import org.agmas.noellesroles.client.ui.roles.coroner.CoronerPlayerWidget;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(LimitedInventoryScreen.class)
public abstract class CoronerScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;
    @Unique private final List<CoronerPlayerWidget> noellesroles$coronerPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$coronerPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$coronerNextPageWidget;
    @Unique private int noellesroles$coronerCurrentPage = 0;

    public CoronerScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    @Inject(method = "render", at = @At("HEAD"))
    void renderCoronerText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());

        if (gameWorldComponent.isRole(player, Noellesroles.CORONER)) {
            CoronerPlayerComponent coronerComp = CoronerPlayerComponent.KEY.get(player);

            if (coronerComp.getMorphTicks() != 0) {
                int y = (height - 32) / 2;
                int x = width / 2;

                if (coronerComp.getMorphTicks() > 0) {
                    // 显示变形状态和剩余时间
                    int secondsRemaining = coronerComp.getMorphTicks() / 20;
                    Text status = Text.translatable("ui.coroner.morphing", secondsRemaining);
                    context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                            status,
                            x - MinecraftClient.getInstance().textRenderer.getWidth(status) / 2,
                            y + 40,
                            Color.GREEN.getRGB());

                    // 显示当前伪装目标
                    if (coronerComp.disguise != null) {
                        PlayerEntity disguisePlayer = player.getWorld().getPlayerByUuid(coronerComp.disguise);
                        String disguiseName = disguisePlayer != null ?
                                disguisePlayer.getName().getString() :
                                "Unknown";
                        Text disguiseText = Text.translatable("ui.coroner.disguised_as", disguiseName);
                        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                                disguiseText,
                                x - MinecraftClient.getInstance().textRenderer.getWidth(disguiseText) / 2,
                                y + 55,
                                Color.YELLOW.getRGB());
                    }
                } else {
                    // 显示冷却时间
                    int cooldownSeconds = -coronerComp.getMorphTicks() / 20;
                    Text cooldown = Text.translatable("ui.coroner.cooldown", cooldownSeconds);
                    context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                            cooldown,
                            x - MinecraftClient.getInstance().textRenderer.getWidth(cooldown) / 2,
                            y + 40,
                            Color.RED.getRGB());
                }
            }
        }
        // 可选：添加一些文本渲染，如VoodooScreenMixin所做
    }

    @Inject(method = "init", at = @At("HEAD"))
    void renderCoronerHeads(CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player, Noellesroles.CORONER)) {
            if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().player.networkHandler == null) {
                return;
            }
            List<UUID> playerUuids = new ArrayList<>(MinecraftClient.getInstance().player.networkHandler.getPlayerUuids());
            int y = PlayerPageLayout.getPlayerRowY(this.height);
            noellesroles$coronerPlayerWidgets.clear();
            noellesroles$coronerCurrentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.CORONER_PAGE_KEY);

            for (int i = 0; i < playerUuids.size(); ++i) {
                UUID targetUuid = playerUuids.get(i);
                PlayerListEntry playerListEntry = MinecraftClient.getInstance().player.networkHandler.getPlayerListEntry(targetUuid);
                AbstractClientPlayerEntity playerEntity = null;

                if (MinecraftClient.getInstance().world != null) {
                    PlayerEntity rawPlayer = MinecraftClient.getInstance().world.getPlayerByUuid(targetUuid);
                    if (rawPlayer instanceof AbstractClientPlayerEntity) {
                        playerEntity = (AbstractClientPlayerEntity) rawPlayer;
                    }
                }

                CoronerPlayerWidget child = new CoronerPlayerWidget(
                        ((LimitedInventoryScreen) (Object) this),
                        0,
                        y,
                        targetUuid,
                        playerListEntry,
                        playerEntity,
                        i,
                        targetUuid.equals(player.getUuid()) // 添加标记是否是自己的头像

                );
                noellesroles$coronerPlayerWidgets.add(child);
                addDrawableChild(child);
            }

            noellesroles$coronerPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    0,
                    y,
                    Items.PURPLE_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.previous"),
                    button -> {
                        noellesroles$coronerCurrentPage--;
                        noellesroles$coronerRefreshPage();
                    }
            ));
            noellesroles$coronerNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    0,
                    y,
                    Items.LIME_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.next"),
                    button -> {
                        noellesroles$coronerCurrentPage++;
                        noellesroles$coronerRefreshPage();
                    }
            ));

            noellesroles$coronerRefreshPage();
        }
    }

    @Unique
    private void noellesroles$coronerRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$coronerPlayerWidgets.size());
        if (noellesroles$coronerCurrentPage < 0) {
            noellesroles$coronerCurrentPage = 0;
        }
        if (noellesroles$coronerCurrentPage >= totalPages) {
            noellesroles$coronerCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.CORONER_PAGE_KEY, noellesroles$coronerCurrentPage);

        int startIndex = noellesroles$coronerCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$coronerPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = noellesroles$coronerCurrentPage > 0;
        boolean showNext = noellesroles$coronerCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < noellesroles$coronerPlayerWidgets.size(); i++) {
            CoronerPlayerWidget widget = noellesroles$coronerPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$coronerPreviousPageWidget != null) {
            noellesroles$coronerPreviousPageWidget.visible = showPrevious;
            noellesroles$coronerPreviousPageWidget.active = showPrevious;
            noellesroles$coronerPreviousPageWidget.setX(groupStartX);
            noellesroles$coronerPreviousPageWidget.setY(y);
        }
        if (noellesroles$coronerNextPageWidget != null) {
            noellesroles$coronerNextPageWidget.visible = showNext;
            noellesroles$coronerNextPageWidget.active = showNext;
            noellesroles$coronerNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$coronerNextPageWidget.setY(y);
        }
    }
}
