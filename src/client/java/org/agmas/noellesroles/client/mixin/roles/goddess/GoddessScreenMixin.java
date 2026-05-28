package org.agmas.noellesroles.client.mixin.roles.goddess;

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
import org.agmas.noellesroles.client.ui.roles.goddess.GoddessPlayerWidget;
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
public abstract class GoddessScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;
    @Unique private final List<GoddessPlayerWidget> noellesroles$goddessPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$goddessPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$goddessNextPageWidget;
    @Unique private int noellesroles$goddessCurrentPage = 0;

    public GoddessScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderGoddessText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.GODDESS)) return;

        int y = (height - 32) / 2;
        int x = width / 2;
        var ability = AbilityPlayerComponent.KEY.get(player);
        Text message;
        if (ability.cooldown > 0) {
            message = Text.translatable("hud.goddess.cooldown", ability.cooldown / 20);
        } else {
            message = Text.translatable("hud.goddess.select_player");
        }
        context.drawTextWithShadow(textRenderer, message,
                x - textRenderer.getWidth(message) / 2, y + 40, Color.WHITE.getRGB());
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void addGoddessWidgets(CallbackInfo ci) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.GODDESS)) return;

        var networkHandler = player.networkHandler;
        if (networkHandler == null) return;

        List<UUID> uuids = new ArrayList<>(networkHandler.getPlayerUuids());
        uuids.removeIf(uuid -> uuid.equals(player.getUuid()));
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        noellesroles$goddessPlayerWidgets.clear();
        noellesroles$goddessCurrentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.GODDESS_PAGE_KEY);

        for (int i = 0; i < uuids.size(); i++) {
            UUID targetUuid = uuids.get(i);
            PlayerListEntry entry = networkHandler.getPlayerListEntry(targetUuid);
            if (entry == null) continue;
            var widget = new GoddessPlayerWidget(
                    (LimitedInventoryScreen) (Object) this,
                    0, y,
                    targetUuid, entry
            );
            noellesroles$goddessPlayerWidgets.add(widget);
            addDrawableChild(widget);
        }

        noellesroles$goddessPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.previous"),
                button -> {
                    noellesroles$goddessCurrentPage--;
                    noellesroles$goddessRefreshPage();
                }
        ));
        noellesroles$goddessNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.next"),
                button -> {
                    noellesroles$goddessCurrentPage++;
                    noellesroles$goddessRefreshPage();
                }
        ));

        noellesroles$goddessRefreshPage();
    }

    @Unique
    private void noellesroles$goddessRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$goddessPlayerWidgets.size());
        if (noellesroles$goddessCurrentPage < 0) {
            noellesroles$goddessCurrentPage = 0;
        }
        if (noellesroles$goddessCurrentPage >= totalPages) {
            noellesroles$goddessCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.GODDESS_PAGE_KEY, noellesroles$goddessCurrentPage);

        int startIndex = noellesroles$goddessCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$goddessPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = noellesroles$goddessCurrentPage > 0;
        boolean showNext = noellesroles$goddessCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < noellesroles$goddessPlayerWidgets.size(); i++) {
            GoddessPlayerWidget widget = noellesroles$goddessPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$goddessPreviousPageWidget != null) {
            noellesroles$goddessPreviousPageWidget.visible = showPrevious;
            noellesroles$goddessPreviousPageWidget.active = showPrevious;
            noellesroles$goddessPreviousPageWidget.setX(groupStartX);
            noellesroles$goddessPreviousPageWidget.setY(y);
        }
        if (noellesroles$goddessNextPageWidget != null) {
            noellesroles$goddessNextPageWidget.visible = showNext;
            noellesroles$goddessNextPageWidget.active = showNext;
            noellesroles$goddessNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$goddessNextPageWidget.setY(y);
        }
    }
}
