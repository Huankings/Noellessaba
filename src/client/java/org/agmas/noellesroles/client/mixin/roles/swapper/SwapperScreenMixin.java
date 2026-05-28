package org.agmas.noellesroles.client.mixin.roles.swapper;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.ui.common.PlayerPageLayout;
import org.agmas.noellesroles.client.ui.common.PagedPlayerScreenState;
import org.agmas.noellesroles.client.ui.common.PlayerPageSwitchWidget;
import org.agmas.noellesroles.client.ui.roles.swapper.SwapperPlayerWidget;
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
public abstract class SwapperScreenMixin extends LimitedHandledScreen<PlayerScreenHandler>{
    @Shadow @Final public ClientPlayerEntity player;
    @Unique private final List<SwapperPlayerWidget> noellesroles$swapperPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$swapperPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$swapperNextPageWidget;
    @Unique private int noellesroles$swapperCurrentPage = 0;

    public SwapperScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }


    @Inject(method = "render", at = @At("HEAD"))
    void renderSwapperText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player,Noellesroles.SWAPPER)) {
            int y = (height- 32) / 2;
            int x = width / 2;
            if (SwapperPlayerWidget.playerChoiceOne == null) {
                Text name = Text.translatable("hud.swapper.first_player_selection");
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, name, x - (MinecraftClient.getInstance().textRenderer.getWidth(name)/2), y + 40, Color.CYAN.getRGB());
            } else {
                Text name = Text.translatable("hud.swapper.second_player_selection");
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, name, x - (MinecraftClient.getInstance().textRenderer.getWidth(name)/2), y + 40, Color.RED.getRGB());
            }
        }
    }
    @Inject(method = "init", at = @At("HEAD"))
    void renderSwapperHeads(CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player,Noellesroles.SWAPPER)) {
            if (MinecraftClient.getInstance().world == null) {
                return;
            }

            // 重新初始化界面时只清空本屏幕实例持有的按钮引用，
            // 真实按钮仍然由 Minecraft 的 Screen 生命周期负责销毁。
            noellesroles$swapperPlayerWidgets.clear();

            // 交换者的第一段选择应该只在“重新打开界面”时重置。
            // 分页本身不会再走 init，因此翻页不会把已选中的第一名玩家冲掉。
            SwapperPlayerWidget.playerChoiceOne = null;
            noellesroles$swapperCurrentPage = PagedPlayerScreenState.getSwapperPage();

            // 复制一份列表再处理，避免直接操作 world.getPlayers() 返回的玩家列表。
            List<AbstractClientPlayerEntity> entries = new ArrayList<>(MinecraftClient.getInstance().world.getPlayers());
            if (!entries.contains(player)) {
                entries.add(player);
            }

            int y = PlayerPageLayout.getPlayerRowY(this.height);

            // 这里先把所有玩家按钮一次性创建出来，后续翻页只改“哪几个可见、可点、显示在哪”。
            // 这样不会因为翻页而重新 new 按钮，也就不会意外触发交换逻辑或丢失已选状态。
            for(int i = 0; i < entries.size(); ++i) {
                SwapperPlayerWidget child = new SwapperPlayerWidget(
                        ((LimitedInventoryScreen)(Object)this),
                        0,
                        y,
                        entries.get(i),
                        i
                );
                noellesroles$swapperPlayerWidgets.add(child);
                addDrawableChild(child);
            }

            noellesroles$swapperPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    PlayerPageLayout.getPreviousButtonX(this.width),
                    y,
                    Items.PURPLE_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.previous"),
                    button -> {
                        noellesroles$swapperCurrentPage--;
                        noellesroles$swapperRefreshPage();
                    }
            ));

            noellesroles$swapperNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    PlayerPageLayout.getNextButtonX(this.width),
                    y,
                    Items.LIME_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.next"),
                    button -> {
                        noellesroles$swapperCurrentPage++;
                        noellesroles$swapperRefreshPage();
                    }
            ));

            noellesroles$swapperRefreshPage();
        }
    }

    /**
     * 交换者分页刷新。
     *
     * 这里只做三件事：
     * 1. 纠正当前页码，保证不会越界。
     * 2. 只让当前页的玩家头像可见并重新居中。
     * 3. 单独控制上一页/下一页按钮显示与否。
     *
     * 注意：翻页按钮是独立控件，根本不会进入 SwapperPlayerWidget 的点击回调，
     * 所以不会被记成“已经选择了一个玩家”，也不会误触发交换发包。
     */
    @Unique
    private void noellesroles$swapperRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$swapperPlayerWidgets.size());
        if (noellesroles$swapperCurrentPage < 0) {
            noellesroles$swapperCurrentPage = 0;
        }
        if (noellesroles$swapperCurrentPage >= totalPages) {
            noellesroles$swapperCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setSwapperPage(noellesroles$swapperCurrentPage);

        int startIndex = noellesroles$swapperCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$swapperPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = noellesroles$swapperCurrentPage > 0;
        boolean showNext = noellesroles$swapperCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < noellesroles$swapperPlayerWidgets.size(); i++) {
            SwapperPlayerWidget widget = noellesroles$swapperPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;

            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$swapperPreviousPageWidget != null) {
            noellesroles$swapperPreviousPageWidget.visible = showPrevious;
            noellesroles$swapperPreviousPageWidget.active = showPrevious;
            noellesroles$swapperPreviousPageWidget.setX(groupStartX);
            noellesroles$swapperPreviousPageWidget.setY(y);
        }

        if (noellesroles$swapperNextPageWidget != null) {
            noellesroles$swapperNextPageWidget.visible = showNext;
            noellesroles$swapperNextPageWidget.active = showNext;
            noellesroles$swapperNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$swapperNextPageWidget.setY(y);
        }
    }
}
