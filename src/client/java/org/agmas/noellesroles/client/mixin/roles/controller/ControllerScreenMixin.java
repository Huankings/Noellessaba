package org.agmas.noellesroles.client.mixin.roles.controller;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.ui.common.PagedPlayerScreenState;
import org.agmas.noellesroles.client.ui.common.PlayerPageLayout;
import org.agmas.noellesroles.client.ui.common.PlayerPageSwitchWidget;
import org.agmas.noellesroles.client.ui.roles.controller.ControllerWidget;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
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

@Mixin(LimitedInventoryScreen.class)
public abstract class ControllerScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;
    @Unique private final List<ControllerWidget> noellesroles$controllerPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$controllerPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$controllerNextPageWidget;
    @Unique private int noellesroles$controllerCurrentPage = 0;

    public ControllerScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    void renderControllerText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());

        if (gameWorldComponent.isRole(player, Noellesroles.CONTROLLER)) {
            ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(player);

            if (controllerComp.possessTicks != 0) {
                int y = (height - 32) / 2;
                int x = width / 2;

                if (controllerComp.possessTicks > 0) {
                    // 显示附体状态和剩余时间
                    int secondsRemaining = controllerComp.possessTicks / 20;
                    Text status = Text.translatable("ui.controller.morphing", secondsRemaining);
                    context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                            status,
                            x - MinecraftClient.getInstance().textRenderer.getWidth(status) / 2,
                            y + 40,
                            Color.MAGENTA.getRGB());

                    // 显示当前附体目标
                    if (controllerComp.controlledTarget != null) {
                        PlayerEntity targetPlayer = player.getWorld().getPlayerByUuid(controllerComp.controlledTarget);
                        String targetName = targetPlayer != null ?
                                targetPlayer.getName().getString() :
                                "Unknown";
                        Text targetText = Text.translatable("ui.controller.disguised_as", targetName);
                        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                                targetText,
                                x - MinecraftClient.getInstance().textRenderer.getWidth(targetText) / 2,
                                y + 55,
                                Color.YELLOW.getRGB());
                    }
                } else {
                    // 显示冷却时间
                    int cooldownSeconds = -controllerComp.possessTicks / 20;
                    Text cooldown = Text.translatable("ui.controller.cooldown", cooldownSeconds);
                    context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                            cooldown,
                            x - MinecraftClient.getInstance().textRenderer.getWidth(cooldown) / 2,
                            y + 40,
                            Color.RED.getRGB());
                }
            }
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    void renderControllerHeads(CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player, Noellesroles.CONTROLLER)) {
            if (MinecraftClient.getInstance().world == null) {
                return;
            }
            // 获取所有存活玩家实体
            List<AbstractClientPlayerEntity> players = MinecraftClient.getInstance().world.getPlayers();
            int y = PlayerPageLayout.getPlayerRowY(this.height);
            noellesroles$controllerPlayerWidgets.clear();
            noellesroles$controllerCurrentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.CONTROLLER_PAGE_KEY);

            for (int i = 0; i < players.size(); ++i) {
                AbstractClientPlayerEntity targetEntity = players.get(i);
                boolean isSelf = targetEntity.getUuid().equals(player.getUuid());

                ControllerWidget child = new ControllerWidget(
                        ((LimitedInventoryScreen) (Object) this),
                        0,
                        y,
                        targetEntity,   // 直接传递实体
                        i,
                        isSelf
                );
                noellesroles$controllerPlayerWidgets.add(child);
                addDrawableChild(child);
            }

            noellesroles$controllerPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    0,
                    y,
                    Items.PURPLE_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.previous"),
                    button -> {
                        noellesroles$controllerCurrentPage--;
                        noellesroles$controllerRefreshPage();
                    }
            ));
            noellesroles$controllerNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    0,
                    y,
                    Items.LIME_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.next"),
                    button -> {
                        noellesroles$controllerCurrentPage++;
                        noellesroles$controllerRefreshPage();
                    }
            ));

            noellesroles$controllerRefreshPage();
        }
    }

    @Unique
    private void noellesroles$controllerRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$controllerPlayerWidgets.size());
        if (noellesroles$controllerCurrentPage < 0) {
            noellesroles$controllerCurrentPage = 0;
        }
        if (noellesroles$controllerCurrentPage >= totalPages) {
            noellesroles$controllerCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.CONTROLLER_PAGE_KEY, noellesroles$controllerCurrentPage);

        int startIndex = noellesroles$controllerCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$controllerPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = noellesroles$controllerCurrentPage > 0;
        boolean showNext = noellesroles$controllerCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < noellesroles$controllerPlayerWidgets.size(); i++) {
            ControllerWidget widget = noellesroles$controllerPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$controllerPreviousPageWidget != null) {
            noellesroles$controllerPreviousPageWidget.visible = showPrevious;
            noellesroles$controllerPreviousPageWidget.active = showPrevious;
            noellesroles$controllerPreviousPageWidget.setX(groupStartX);
            noellesroles$controllerPreviousPageWidget.setY(y);
        }
        if (noellesroles$controllerNextPageWidget != null) {
            noellesroles$controllerNextPageWidget.visible = showNext;
            noellesroles$controllerNextPageWidget.active = showNext;
            noellesroles$controllerNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$controllerNextPageWidget.setY(y);
        }
    }
}
