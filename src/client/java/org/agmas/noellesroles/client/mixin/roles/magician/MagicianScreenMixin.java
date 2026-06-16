package org.agmas.noellesroles.client.mixin.roles.magician;

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
import org.agmas.noellesroles.client.ui.roles.magician.MagicianPlayerWidget;
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
 * 魔术师背包里的分页头像选人 UI。
 *
 * <p>这里故意直接对齐风灵师的交互风格：
 * 玩家可以在背包下方浏览全部在线玩家头像，并即时切换当前皮套目标。</p>
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class MagicianScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;

    @Unique private final List<MagicianPlayerWidget> noellesroles$magicianPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$magicianPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$magicianNextPageWidget;
    @Unique private int noellesroles$magicianCurrentPage = 0;

    public MagicianScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void noellesroles$initMagicianUi(CallbackInfo ci) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.MAGICIAN)) {
            return;
        }

        noellesroles$addPlayerSelectionUi();
    }

    @Unique
    private void noellesroles$addPlayerSelectionUi() {
        if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().player.networkHandler == null) {
            return;
        }

        List<UUID> entries = new ArrayList<>(MinecraftClient.getInstance().player.networkHandler.getPlayerUuids());
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        noellesroles$magicianPlayerWidgets.clear();
        noellesroles$magicianCurrentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.MAGICIAN_PAGE_KEY);

        for (UUID targetUuid : entries) {
            PlayerListEntry playerListEntry = MinecraftClient.getInstance().player.networkHandler.getPlayerListEntry(targetUuid);
            MagicianPlayerWidget child = new MagicianPlayerWidget(
                    (LimitedInventoryScreen) (Object) this,
                    0,
                    y,
                    targetUuid,
                    playerListEntry
            );
            noellesroles$magicianPlayerWidgets.add(child);
            addDrawableChild(child);
        }

        noellesroles$magicianPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.previous"),
                button -> {
                    noellesroles$magicianCurrentPage--;
                    noellesroles$refreshPage();
                }
        ));
        noellesroles$magicianNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.next"),
                button -> {
                    noellesroles$magicianCurrentPage++;
                    noellesroles$refreshPage();
                }
        ));

        noellesroles$refreshPage();
    }

    @Unique
    private void noellesroles$refreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$magicianPlayerWidgets.size());
        if (noellesroles$magicianCurrentPage < 0) {
            noellesroles$magicianCurrentPage = 0;
        }
        if (noellesroles$magicianCurrentPage >= totalPages) {
            noellesroles$magicianCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.MAGICIAN_PAGE_KEY, noellesroles$magicianCurrentPage);

        int startIndex = noellesroles$magicianCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$magicianPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = noellesroles$magicianCurrentPage > 0;
        boolean showNext = noellesroles$magicianCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < noellesroles$magicianPlayerWidgets.size(); i++) {
            MagicianPlayerWidget widget = noellesroles$magicianPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$magicianPreviousPageWidget != null) {
            noellesroles$magicianPreviousPageWidget.visible = showPrevious;
            noellesroles$magicianPreviousPageWidget.active = showPrevious;
            noellesroles$magicianPreviousPageWidget.setX(groupStartX);
            noellesroles$magicianPreviousPageWidget.setY(y);
        }
        if (noellesroles$magicianNextPageWidget != null) {
            noellesroles$magicianNextPageWidget.visible = showNext;
            noellesroles$magicianNextPageWidget.active = showNext;
            noellesroles$magicianNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$magicianNextPageWidget.setY(y);
        }
    }
}
