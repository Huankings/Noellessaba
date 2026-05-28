package org.agmas.noellesroles.client.mixin.roles.voodoo;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.ui.common.PagedPlayerScreenState;
import org.agmas.noellesroles.client.ui.common.PlayerPageLayout;
import org.agmas.noellesroles.client.ui.common.PlayerPageSwitchWidget;
import org.agmas.noellesroles.client.ui.roles.voodoo.VoodooPlayerWidget;
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
public abstract class VoodoScreenMixin extends LimitedHandledScreen<PlayerScreenHandler>{
    @Shadow @Final public ClientPlayerEntity player;
    @Unique private final List<VoodooPlayerWidget> noellesroles$voodooPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$voodooPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$voodooNextPageWidget;
    @Unique private int noellesroles$voodooCurrentPage = 0;

    public VoodoScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    @Inject(method = "render", at = @At("HEAD"))
    void renderVoodooText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        ConfigWorldComponent configWorldComponent = (ConfigWorldComponent) ConfigWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player,Noellesroles.VOODOO)) {
            int y = (height- 32) / 2;
            int x = width / 2;
            if (!configWorldComponent.naturalVoodoosAllowed) {
                Text name = Text.translatable("hud.voodoo.player_deaths_only");
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, name, x - (MinecraftClient.getInstance().textRenderer.getWidth(name)/2), y + 40, Color.RED.getRGB());
            }
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    void renderVoodooHeads(CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorldComponent.isRole(player,Noellesroles.VOODOO)) {
            if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().player.networkHandler == null) {
                return;
            }

            noellesroles$voodooPlayerWidgets.clear();
            noellesroles$voodooCurrentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.VOODOO_PAGE_KEY);

            List<UUID> entries = new ArrayList<>(MinecraftClient.getInstance().player.networkHandler.getPlayerUuids());
            entries.removeIf((e) -> e.equals(player.getUuid()));
            int y = PlayerPageLayout.getPlayerRowY(this.height);

            for(int i = 0; i < entries.size(); ++i) {
                VoodooPlayerWidget child = new VoodooPlayerWidget(
                        ((LimitedInventoryScreen)(Object)this),
                        0,
                        y,
                        entries.get(i),
                        MinecraftClient.getInstance().player.networkHandler.getPlayerListEntry(entries.get(i)),
                        player.getWorld(),
                        i
                );
                noellesroles$voodooPlayerWidgets.add(child);
                addDrawableChild(child);
            }

            noellesroles$voodooPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    0,
                    y,
                    Items.PURPLE_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.previous"),
                    button -> {
                        noellesroles$voodooCurrentPage--;
                        noellesroles$voodooRefreshPage();
                    }
            ));

            noellesroles$voodooNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    0,
                    y,
                    Items.LIME_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.next"),
                    button -> {
                        noellesroles$voodooCurrentPage++;
                        noellesroles$voodooRefreshPage();
                    }
            ));

            noellesroles$voodooRefreshPage();
        }
    }

    @Unique
    private void noellesroles$voodooRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$voodooPlayerWidgets.size());
        if (noellesroles$voodooCurrentPage < 0) {
            noellesroles$voodooCurrentPage = 0;
        }
        if (noellesroles$voodooCurrentPage >= totalPages) {
            noellesroles$voodooCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.VOODOO_PAGE_KEY, noellesroles$voodooCurrentPage);

        int startIndex = noellesroles$voodooCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$voodooPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = noellesroles$voodooCurrentPage > 0;
        boolean showNext = noellesroles$voodooCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < noellesroles$voodooPlayerWidgets.size(); i++) {
            VoodooPlayerWidget widget = noellesroles$voodooPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$voodooPreviousPageWidget != null) {
            noellesroles$voodooPreviousPageWidget.visible = showPrevious;
            noellesroles$voodooPreviousPageWidget.active = showPrevious;
            noellesroles$voodooPreviousPageWidget.setX(groupStartX);
            noellesroles$voodooPreviousPageWidget.setY(y);
        }

        if (noellesroles$voodooNextPageWidget != null) {
            noellesroles$voodooNextPageWidget.visible = showNext;
            noellesroles$voodooNextPageWidget.active = showNext;
            noellesroles$voodooNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$voodooNextPageWidget.setY(y);
        }
    }
}
