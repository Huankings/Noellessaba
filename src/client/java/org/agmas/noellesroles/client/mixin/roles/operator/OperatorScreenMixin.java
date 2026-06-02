package org.agmas.noellesroles.client.mixin.roles.operator;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.ui.common.PagedPlayerScreenState;
import org.agmas.noellesroles.client.ui.common.PlayerPageLayout;
import org.agmas.noellesroles.client.ui.common.PlayerPageSwitchWidget;
import org.agmas.noellesroles.client.ui.roles.operator.OperatorPlayerWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 接线员背包界面。
 *
 * <p>这里的数据源同时兼顾三个要求：</p>
 * <p>1. 要包含当前在线存活玩家；</p>
 * <p>2. 要包含已经旁观/创造的非存活玩家；</p>
 * <p>3. 要确保把自己也包含进去。</p>
 *
 * <p>因此这里以 tab 列表 UUID 为主，再补一遍自己，避免极端情况下自身不在列表里。</p>
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class OperatorScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;

    @Unique private final List<OperatorPlayerWidget> noellesroles$operatorPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$operatorPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$operatorNextPageWidget;
    @Unique private int noellesroles$operatorCurrentPage = 0;

    public OperatorScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    void renderOperatorText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.OPERATOR)) {
            return;
        }

        int y = (height - 32) / 2;
        int x = width / 2;
        Text name = OperatorPlayerWidget.firstChoice == null
                ? Text.translatable("hud.operator.first_player_selection")
                : Text.translatable("hud.operator.second_player_selection");
        context.drawTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                name,
                x - (MinecraftClient.getInstance().textRenderer.getWidth(name) / 2),
                y + 40,
                Noellesroles.OPERATOR.color()
        );
    }

    @Inject(method = "init", at = @At("HEAD"))
    void initOperatorHeads(CallbackInfo ci) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.OPERATOR)) {
            return;
        }
        if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().player.networkHandler == null) {
            return;
        }

        noellesroles$operatorPlayerWidgets.clear();
        OperatorPlayerWidget.firstChoice = null;
        noellesroles$operatorCurrentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.OPERATOR_PAGE_KEY);

        Set<UUID> entries = new LinkedHashSet<>(MinecraftClient.getInstance().player.networkHandler.getPlayerUuids());
        entries.add(player.getUuid());
        int y = PlayerPageLayout.getPlayerRowY(this.height);

        int index = 0;
        for (UUID targetUuid : entries) {
            PlayerListEntry entry = MinecraftClient.getInstance().player.networkHandler.getPlayerListEntry(targetUuid);
            OperatorPlayerWidget child = new OperatorPlayerWidget(
                    (LimitedInventoryScreen) (Object) this,
                    0,
                    y,
                    targetUuid,
                    entry
            );
            noellesroles$operatorPlayerWidgets.add(child);
            addDrawableChild(child);
            index++;
        }

        noellesroles$operatorPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.previous"),
                button -> {
                    noellesroles$operatorCurrentPage--;
                    noellesroles$operatorRefreshPage();
                }
        ));
        noellesroles$operatorNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.next"),
                button -> {
                    noellesroles$operatorCurrentPage++;
                    noellesroles$operatorRefreshPage();
                }
        ));

        noellesroles$operatorRefreshPage();
    }

    @Unique
    private void noellesroles$operatorRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$operatorPlayerWidgets.size());
        if (noellesroles$operatorCurrentPage < 0) {
            noellesroles$operatorCurrentPage = 0;
        }
        if (noellesroles$operatorCurrentPage >= totalPages) {
            noellesroles$operatorCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.OPERATOR_PAGE_KEY, noellesroles$operatorCurrentPage);

        int startIndex = noellesroles$operatorCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$operatorPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = noellesroles$operatorCurrentPage > 0;
        boolean showNext = noellesroles$operatorCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < noellesroles$operatorPlayerWidgets.size(); i++) {
            OperatorPlayerWidget widget = noellesroles$operatorPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$operatorPreviousPageWidget != null) {
            noellesroles$operatorPreviousPageWidget.visible = showPrevious;
            noellesroles$operatorPreviousPageWidget.active = showPrevious;
            noellesroles$operatorPreviousPageWidget.setX(groupStartX);
            noellesroles$operatorPreviousPageWidget.setY(y);
        }
        if (noellesroles$operatorNextPageWidget != null) {
            noellesroles$operatorNextPageWidget.visible = showNext;
            noellesroles$operatorNextPageWidget.active = showNext;
            noellesroles$operatorNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$operatorNextPageWidget.setY(y);
        }
    }
}
