package org.agmas.noellesroles.client.mixin.roles.winder;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
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
import org.agmas.noellesroles.client.ui.roles.winder.WinderPlayerWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 风灵师背包界面：
 * 上方渲染商店，下方保留头像选人。
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class WinderScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;
    @Unique private final List<WinderPlayerWidget> noellesroles$winderPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$winderPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$winderNextPageWidget;
    @Unique private int noellesroles$winderCurrentPage = 0;

    public WinderScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    void winderInit(CallbackInfo ci) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.WINDER)) {
            return;
        }

        // 商店条目现在统一由通用商店 mixin 负责，这里只保留风灵师额外的选人界面。
        noellesroles$winderAddPlayerSelectionUI();
    }

    /**
     * 直接用玩家列表缓存里的 UUID，
     * 这样死亡玩家依旧会留在界面里，不会中途消失。
     */
    @Unique
    private void noellesroles$winderAddPlayerSelectionUI() {
        List<UUID> entries = new ArrayList<>(MinecraftClient.getInstance().player.networkHandler.getPlayerUuids());
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        noellesroles$winderPlayerWidgets.clear();
        noellesroles$winderCurrentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.WINDER_PAGE_KEY);

        for (int i = 0; i < entries.size(); ++i) {
            UUID targetUuid = entries.get(i);
            PlayerListEntry playerListEntry = MinecraftClient.getInstance().player.networkHandler.getPlayerListEntry(targetUuid);
            WinderPlayerWidget child = new WinderPlayerWidget(
                    (LimitedInventoryScreen) (Object) this,
                    0,
                    y,
                    targetUuid,
                    playerListEntry
            );
            noellesroles$winderPlayerWidgets.add(child);
            addDrawableChild(child);
        }

        noellesroles$winderPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.previous"),
                button -> {
                    noellesroles$winderCurrentPage--;
                    noellesroles$winderRefreshPage();
                }
        ));
        noellesroles$winderNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.next"),
                button -> {
                    noellesroles$winderCurrentPage++;
                    noellesroles$winderRefreshPage();
                }
        ));

        noellesroles$winderRefreshPage();
    }

    @Unique
    private void noellesroles$winderRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$winderPlayerWidgets.size());
        if (noellesroles$winderCurrentPage < 0) {
            noellesroles$winderCurrentPage = 0;
        }
        if (noellesroles$winderCurrentPage >= totalPages) {
            noellesroles$winderCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.WINDER_PAGE_KEY, noellesroles$winderCurrentPage);

        int startIndex = noellesroles$winderCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$winderPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = noellesroles$winderCurrentPage > 0;
        boolean showNext = noellesroles$winderCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < noellesroles$winderPlayerWidgets.size(); i++) {
            WinderPlayerWidget widget = noellesroles$winderPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$winderPreviousPageWidget != null) {
            noellesroles$winderPreviousPageWidget.visible = showPrevious;
            noellesroles$winderPreviousPageWidget.active = showPrevious;
            noellesroles$winderPreviousPageWidget.setX(groupStartX);
            noellesroles$winderPreviousPageWidget.setY(y);
        }
        if (noellesroles$winderNextPageWidget != null) {
            noellesroles$winderNextPageWidget.visible = showNext;
            noellesroles$winderNextPageWidget.active = showNext;
            noellesroles$winderNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$winderNextPageWidget.setY(y);
        }
    }
}
