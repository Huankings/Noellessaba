package org.agmas.noellesroles.client.mixin.roles.brainwasher;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.ui.common.PagedPlayerScreenState;
import org.agmas.noellesroles.client.ui.common.PlayerPageLayout;
import org.agmas.noellesroles.client.ui.common.PlayerPageSwitchWidget;
import org.agmas.noellesroles.client.ui.roles.brainwasher.BrainwasherPlayerWidget;
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
public abstract class BrainwasherScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;
    @Unique private final List<BrainwasherPlayerWidget> noellesroles$brainwasherPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$brainwasherPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$brainwasherNextPageWidget;
    @Unique private int noellesroles$brainwasherCurrentPage = 0;

    public BrainwasherScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addBrainwasherWidgets(CallbackInfo ci) {
        var gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.BRAINWASHER)) return;

        var networkHandler = player.networkHandler;
        if (networkHandler == null) return;

        // 获取所有在线玩家UUID，排除自己
        List<UUID> uuids = new ArrayList<>(networkHandler.getPlayerUuids());
        uuids.removeIf(uuid -> uuid.equals(player.getUuid()));
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        noellesroles$brainwasherPlayerWidgets.clear();
        noellesroles$brainwasherCurrentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.BRAINWASHER_PAGE_KEY);

        for (int i = 0; i < uuids.size(); i++) {
            UUID targetUuid = uuids.get(i);
            PlayerListEntry entry = networkHandler.getPlayerListEntry(targetUuid);
            if (entry == null) continue;
            var widget = new BrainwasherPlayerWidget(
                    (LimitedInventoryScreen) (Object) this,
                    0, y,
                    targetUuid, entry
            );
            noellesroles$brainwasherPlayerWidgets.add(widget);
            addDrawableChild(widget);
        }

        noellesroles$brainwasherPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.previous"),
                button -> {
                    noellesroles$brainwasherCurrentPage--;
                    noellesroles$brainwasherRefreshPage();
                }
        ));
        noellesroles$brainwasherNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.next"),
                button -> {
                    noellesroles$brainwasherCurrentPage++;
                    noellesroles$brainwasherRefreshPage();
                }
        ));

        noellesroles$brainwasherRefreshPage();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderHintText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.BRAINWASHER)) return;

        int centerX = width / 2;
        int y = (height - 32) / 2 + 40;
        var ability = AbilityPlayerComponent.KEY.get(player);
        Text message;
        if (ability.cooldown > 0) {
            message = Text.translatable("hud.brainwasher.cooldown", ability.cooldown / 20);
        } else {
            message = Text.translatable("hud.brainwasher.select_player");
        }
        context.drawTextWithShadow(textRenderer, message,
                centerX - textRenderer.getWidth(message) / 2, y, 0xFF69B4);
    }

    @Unique
    private void noellesroles$brainwasherRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$brainwasherPlayerWidgets.size());
        if (noellesroles$brainwasherCurrentPage < 0) {
            noellesroles$brainwasherCurrentPage = 0;
        }
        if (noellesroles$brainwasherCurrentPage >= totalPages) {
            noellesroles$brainwasherCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.BRAINWASHER_PAGE_KEY, noellesroles$brainwasherCurrentPage);

        int startIndex = noellesroles$brainwasherCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$brainwasherPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = noellesroles$brainwasherCurrentPage > 0;
        boolean showNext = noellesroles$brainwasherCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < noellesroles$brainwasherPlayerWidgets.size(); i++) {
            BrainwasherPlayerWidget widget = noellesroles$brainwasherPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$brainwasherPreviousPageWidget != null) {
            noellesroles$brainwasherPreviousPageWidget.visible = showPrevious;
            noellesroles$brainwasherPreviousPageWidget.active = showPrevious;
            noellesroles$brainwasherPreviousPageWidget.setX(groupStartX);
            noellesroles$brainwasherPreviousPageWidget.setY(y);
        }
        if (noellesroles$brainwasherNextPageWidget != null) {
            noellesroles$brainwasherNextPageWidget.visible = showNext;
            noellesroles$brainwasherNextPageWidget.active = showNext;
            noellesroles$brainwasherNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$brainwasherNextPageWidget.setY(y);
        }
    }
}
