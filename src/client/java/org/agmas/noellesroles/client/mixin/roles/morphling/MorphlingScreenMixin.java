package org.agmas.noellesroles.client.mixin.roles.morphling;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.ui.common.PlayerPageLayout;
import org.agmas.noellesroles.client.ui.common.PagedPlayerScreenState;
import org.agmas.noellesroles.client.ui.common.PlayerPageSwitchWidget;
import org.agmas.noellesroles.client.ui.roles.morphling.MorphlingPlayerWidget;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
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


@Mixin(LimitedInventoryScreen.class)
public abstract class MorphlingScreenMixin extends LimitedHandledScreen<PlayerScreenHandler>{
    @Shadow @Final public ClientPlayerEntity player;
    @Unique private final List<MorphlingPlayerWidget> noellesroles$morphlingPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$morphlingPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$morphlingNextPageWidget;
    @Unique private int noellesroles$morphlingCurrentPage = 0;

    public MorphlingScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }


    @Inject(method = "init", at = @At("TAIL"))
    void renderMorphlingHeads(CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());

        if (gameWorldComponent.isRole(player, Noellesroles.MORPHLING)) {
            if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().player.networkHandler == null) {
                return;
            }

            noellesroles$morphlingPlayerWidgets.clear();
            noellesroles$morphlingCurrentPage = PagedPlayerScreenState.getMorphlingPage();

            List<UUID> playerUuids = new ArrayList<>(MinecraftClient.getInstance().player.networkHandler.getPlayerUuids());
            playerUuids.removeIf(uuid -> uuid.equals(player.getUuid()));
            int y = PlayerPageLayout.getPlayerRowY(this.height);

            for (int i = 0; i < playerUuids.size(); ++i) {
                UUID targetUuid = playerUuids.get(i);
                PlayerListEntry playerListEntry = MinecraftClient.getInstance().player.networkHandler.getPlayerListEntry(targetUuid);
                AbstractClientPlayerEntity playerEntity = null;
                if (MinecraftClient.getInstance().world != null) {
                    // 获取玩家实体并安全转换。
                    // 变形怪优先使用 PlayerListEntry 的皮肤缓存，实体这里只作为兜底数据来源。
                    PlayerEntity rawPlayer = MinecraftClient.getInstance().world.getPlayerByUuid(targetUuid);
                    if (rawPlayer instanceof AbstractClientPlayerEntity) {
                        playerEntity = (AbstractClientPlayerEntity) rawPlayer;
                    }
                }
                MorphlingPlayerWidget child = new MorphlingPlayerWidget(((LimitedInventoryScreen) (Object) this),
                        0,
                        y,
                        targetUuid,
                        playerListEntry,
                        playerEntity,
                        i
                );
                noellesroles$morphlingPlayerWidgets.add(child);
                addDrawableChild(child);
            }

            noellesroles$morphlingPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    PlayerPageLayout.getPreviousButtonX(this.width),
                    y,
                    Items.PURPLE_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.previous"),
                    button -> {
                        noellesroles$morphlingCurrentPage--;
                        noellesroles$morphlingRefreshPage();
                    }
            ));

            noellesroles$morphlingNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    PlayerPageLayout.getNextButtonX(this.width),
                    y,
                    Items.LIME_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.next"),
                    button -> {
                        noellesroles$morphlingCurrentPage++;
                        noellesroles$morphlingRefreshPage();
                    }
            ));

            noellesroles$morphlingRefreshPage();
        }
    }

    /**
     * 变形怪分页刷新。
     *
     * 和交换者一样，这里只更新当前页的可见头像与坐标，不会重新创建控件。
     * 翻页按钮也和玩家头像按钮彻底分离，因此点上一页/下一页时只会换页，
     * 不会误发 MorphC2SPacket，更不会把翻页当成选择了一个目标玩家。
     */
    @Unique
    private void noellesroles$morphlingRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$morphlingPlayerWidgets.size());
        if (noellesroles$morphlingCurrentPage < 0) {
            noellesroles$morphlingCurrentPage = 0;
        }
        if (noellesroles$morphlingCurrentPage >= totalPages) {
            noellesroles$morphlingCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setMorphlingPage(noellesroles$morphlingCurrentPage);

        int startIndex = noellesroles$morphlingCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$morphlingPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = noellesroles$morphlingCurrentPage > 0;
        boolean showNext = noellesroles$morphlingCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        // 变形怪在变形持续期间，原本玩家头像就会整体消失。
        // 这里让分页按钮也跟着走同一显示条件，避免头像没了但翻页按钮还单独悬着。
        boolean playerSelectionVisible = MinecraftClient.getInstance().player != null
                && MorphlingPlayerComponent.KEY.get(MinecraftClient.getInstance().player).getMorphTicks() <= 0;

        for (int i = 0; i < noellesroles$morphlingPlayerWidgets.size(); i++) {
            MorphlingPlayerWidget widget = noellesroles$morphlingPlayerWidgets.get(i);
            boolean visible = playerSelectionVisible && i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;

            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$morphlingPreviousPageWidget != null) {
            boolean visible = playerSelectionVisible && showPrevious;
            noellesroles$morphlingPreviousPageWidget.visible = visible;
            noellesroles$morphlingPreviousPageWidget.active = visible;
            noellesroles$morphlingPreviousPageWidget.setX(groupStartX);
            noellesroles$morphlingPreviousPageWidget.setY(y);
        }

        if (noellesroles$morphlingNextPageWidget != null) {
            boolean visible = playerSelectionVisible && showNext;
            noellesroles$morphlingNextPageWidget.visible = visible;
            noellesroles$morphlingNextPageWidget.active = visible;
            noellesroles$morphlingNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$morphlingNextPageWidget.setY(y);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    void noellesroles$refreshMorphlingPageVisibility(CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player, Noellesroles.MORPHLING)) {
            noellesroles$morphlingRefreshPage();
        }
    }
}
