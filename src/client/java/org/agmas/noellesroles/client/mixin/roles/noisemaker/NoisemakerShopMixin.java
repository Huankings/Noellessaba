package org.agmas.noellesroles.client.mixin.roles.noisemaker;

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
import org.agmas.noellesroles.client.ui.roles.noisemaker.NoisemakerPlayerWidget;
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
 * 大嗓门商店页面。
 * 这里除了商店物品外，还会保留原本的玩家选择区域。
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class NoisemakerShopMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;
    @Unique private final List<NoisemakerPlayerWidget> noellesroles$noisemakerPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$noisemakerPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$noisemakerNextPageWidget;
    @Unique private int noellesroles$noisemakerCurrentPage = 0;

    public NoisemakerShopMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    void noisemakerShopAddChildren(CallbackInfo ci) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.NOISEMAKER)) return;

        // 商店条目改由统一商店 mixin 负责，这里只保留大嗓门独有的玩家选择区域。
        noellesroles$noisemakerAddPlayerSelectionUI();
    }

    /**
     * 使用职业专属的唯一方法名，避免与其他也混入 LimitedInventoryScreen 的职业方法重名后互相串用。
     * 这类冲突会直接导致按钮样式和点击行为都跑到另一个职业的实现上。
     */
    @Unique
    private void noellesroles$noisemakerAddPlayerSelectionUI() {
        // 获取当前在线玩家 UUID 列表，包含死亡或旁观玩家，维持原有行为。
        List<UUID> playerUuids = new ArrayList<>(MinecraftClient.getInstance().player.networkHandler.getPlayerUuids());
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        noellesroles$noisemakerPlayerWidgets.clear();
        noellesroles$noisemakerCurrentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.NOISEMAKER_PAGE_KEY);

        for (int i = 0; i < playerUuids.size(); ++i) {
            UUID targetUuid = playerUuids.get(i);
            PlayerListEntry playerListEntry = MinecraftClient.getInstance().player.networkHandler.getPlayerListEntry(targetUuid);

            NoisemakerPlayerWidget child = new NoisemakerPlayerWidget(
                    ((LimitedInventoryScreen) (Object) this),
                    0,
                    y,
                    targetUuid,
                    playerListEntry,
                    i
            );
            noellesroles$noisemakerPlayerWidgets.add(child);
            addDrawableChild(child);
        }

        noellesroles$noisemakerPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.previous"),
                button -> {
                    noellesroles$noisemakerCurrentPage--;
                    noellesroles$noisemakerRefreshPage();
                }
        ));
        noellesroles$noisemakerNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.next"),
                button -> {
                    noellesroles$noisemakerCurrentPage++;
                    noellesroles$noisemakerRefreshPage();
                }
        ));

        noellesroles$noisemakerRefreshPage();
    }

    @Unique
    private void noellesroles$noisemakerRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$noisemakerPlayerWidgets.size());
        if (noellesroles$noisemakerCurrentPage < 0) {
            noellesroles$noisemakerCurrentPage = 0;
        }
        if (noellesroles$noisemakerCurrentPage >= totalPages) {
            noellesroles$noisemakerCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.NOISEMAKER_PAGE_KEY, noellesroles$noisemakerCurrentPage);

        int startIndex = noellesroles$noisemakerCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$noisemakerPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = noellesroles$noisemakerCurrentPage > 0;
        boolean showNext = noellesroles$noisemakerCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < noellesroles$noisemakerPlayerWidgets.size(); i++) {
            NoisemakerPlayerWidget widget = noellesroles$noisemakerPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$noisemakerPreviousPageWidget != null) {
            noellesroles$noisemakerPreviousPageWidget.visible = showPrevious;
            noellesroles$noisemakerPreviousPageWidget.active = showPrevious;
            noellesroles$noisemakerPreviousPageWidget.setX(groupStartX);
            noellesroles$noisemakerPreviousPageWidget.setY(y);
        }
        if (noellesroles$noisemakerNextPageWidget != null) {
            noellesroles$noisemakerNextPageWidget.visible = showNext;
            noellesroles$noisemakerNextPageWidget.active = showNext;
            noellesroles$noisemakerNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$noisemakerNextPageWidget.setY(y);
        }
    }
}
