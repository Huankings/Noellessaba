package org.agmas.noellesroles.client.mixin.modifiers.guesser;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.ui.common.PagedPlayerScreenState;
import org.agmas.noellesroles.client.ui.common.PlayerPageLayout;
import org.agmas.noellesroles.client.ui.common.PlayerPageSwitchWidget;
import org.agmas.noellesroles.client.ui.modifiers.guesser.GuesserPlayerWidget;
import org.agmas.noellesroles.client.ui.modifiers.guesser.GuesserRoleWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;


@Mixin(LimitedInventoryScreen.class)
public abstract class GuesserScreenMixin extends LimitedHandledScreen<PlayerScreenHandler>{
    @Shadow @Final public ClientPlayerEntity player;
    @Unique private final List<GuesserPlayerWidget> noellesroles$guesserPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$guesserPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$guesserNextPageWidget;
    @Unique private int noellesroles$guesserCurrentPage = 0;

    public GuesserScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }


    @Inject(method = "init", at = @At("HEAD"))
    void renderGuesserHeads(CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        WorldModifierComponent worldModifierComponent = WorldModifierComponent.KEY.get(player.getWorld());
        GuesserPlayerWidget.selectedPlayer = null;
        if (worldModifierComponent.isRole(player, Noellesroles.GUESSER)) {
            GuesserRoleWidget.stopClosing = false;
            // 获取所有存活玩家实体，并移除自己（猜测者不能猜自己）
            List<AbstractClientPlayerEntity> players = MinecraftClient.getInstance().world.getPlayers();
            List<AbstractClientPlayerEntity> targets = new ArrayList<>(players);
            targets.removeIf(p -> p.getUuid().equals(player.getUuid()));
            int y = (((LimitedInventoryScreen) (Object) this).height - 32) / 2 + 105;
            noellesroles$guesserPlayerWidgets.clear();
            noellesroles$guesserCurrentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.GUESSER_PAGE_KEY);

            for (int i = 0; i < targets.size(); ++i) {
                AbstractClientPlayerEntity targetEntity = targets.get(i);

                GuesserPlayerWidget child = new GuesserPlayerWidget(
                        ((LimitedInventoryScreen) (Object) this),
                        0,
                        y,
                        targetEntity   // 直接传递实体
                );
                noellesroles$guesserPlayerWidgets.add(child);
                addDrawableChild(child);
                child.visible = false;  // 初始隐藏，等待角色选择界面出现
            }

            noellesroles$guesserPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    0,
                    y,
                    Items.PURPLE_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.previous"),
                    button -> {
                        noellesroles$guesserCurrentPage--;
                        noellesroles$guesserRefreshPage();
                    }
            ));
            noellesroles$guesserNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                    0,
                    y,
                    Items.LIME_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.next"),
                    button -> {
                        noellesroles$guesserCurrentPage++;
                        noellesroles$guesserRefreshPage();
                    }
            ));

            GuesserRoleWidget child = new GuesserRoleWidget(
                    ((LimitedInventoryScreen) (Object) this),
                    textRenderer,
                    (width / 2) - 100,
                    y
            );
            addDrawableChild(child);
            child.setVisible(false);

            noellesroles$guesserRefreshPage();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    void swapGuesserModes(CallbackInfo ci) {
        for (Element child : children()) {
            GuesserRoleWidget.stopClosing = GuesserPlayerWidget.selectedPlayer != null;
            if (child instanceof GuesserPlayerWidget gpw) {
                gpw.visible = GuesserPlayerWidget.selectedPlayer == null;
            }
            if (child instanceof GuesserRoleWidget grw) {
                grw.visible = GuesserPlayerWidget.selectedPlayer != null;
            }
        }
        noellesroles$guesserRefreshPage();
    }

    @Unique
    private void noellesroles$guesserRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(noellesroles$guesserPlayerWidgets.size());
        if (noellesroles$guesserCurrentPage < 0) {
            noellesroles$guesserCurrentPage = 0;
        }
        if (noellesroles$guesserCurrentPage >= totalPages) {
            noellesroles$guesserCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.GUESSER_PAGE_KEY, noellesroles$guesserCurrentPage);

        int startIndex = noellesroles$guesserCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, noellesroles$guesserPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = (((LimitedInventoryScreen) (Object) this).height - 32) / 2 + 105;
        boolean playerSelectPhase = GuesserPlayerWidget.selectedPlayer == null;
        boolean showPrevious = playerSelectPhase && noellesroles$guesserCurrentPage > 0;
        boolean showNext = playerSelectPhase && noellesroles$guesserCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < noellesroles$guesserPlayerWidgets.size(); i++) {
            GuesserPlayerWidget widget = noellesroles$guesserPlayerWidgets.get(i);
            boolean visible = playerSelectPhase && i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$guesserPreviousPageWidget != null) {
            noellesroles$guesserPreviousPageWidget.visible = showPrevious;
            noellesroles$guesserPreviousPageWidget.active = showPrevious;
            noellesroles$guesserPreviousPageWidget.setX(groupStartX);
            noellesroles$guesserPreviousPageWidget.setY(y);
        }
        if (noellesroles$guesserNextPageWidget != null) {
            noellesroles$guesserNextPageWidget.visible = showNext;
            noellesroles$guesserNextPageWidget.active = showNext;
            noellesroles$guesserNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$guesserNextPageWidget.setY(y);
        }
    }
}
