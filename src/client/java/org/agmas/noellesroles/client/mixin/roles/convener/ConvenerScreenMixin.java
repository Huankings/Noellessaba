package org.agmas.noellesroles.client.mixin.roles.convener;

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
import org.agmas.noellesroles.client.roles.convener.ConvenerDisguiseResolver;
import org.agmas.noellesroles.client.ui.common.PagedPlayerScreenState;
import org.agmas.noellesroles.client.ui.common.PlayerPageLayout;
import org.agmas.noellesroles.client.ui.common.PlayerPageSwitchWidget;
import org.agmas.noellesroles.client.ui.roles.convener.ConvenerDisguiseButton;
import org.agmas.noellesroles.roles.convener.ConvenerDisguiseComponent;
import org.agmas.noellesroles.roles.convener.ConvenerPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 召集者背包伪装头像界面。
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class ConvenerScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;

    @Unique private final List<ConvenerDisguiseButton> noellesroles$convenerButtons = new ArrayList<>();
    @Unique private final List<UUID> noellesroles$buttonTargets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget noellesroles$previousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$nextPageWidget;
    @Unique private int noellesroles$currentPage;
    @Unique private boolean noellesroles$pageInitialized;

    public ConvenerScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void noellesroles$initConvenerHeads(CallbackInfo ci) {
        noellesroles$ensureConvenerWidgets();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void noellesroles$renderConvenerText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!noellesroles$isConvener()) {
            noellesroles$hideConvenerWidgets();
            return;
        }

        /*
         * 已解锁头像会随着召集尸体逐步增长。
         * 这里在 render 阶段也兜底刷新一次，避免组件同步刚到时需要关闭重开背包才看见新头像。
         */
        noellesroles$ensureConvenerWidgets();

        ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(this.player);
        ConvenerPlayerComponent convener = ConvenerPlayerComponent.KEY.get(this.player);

        int centerX = this.width / 2;
        int baseY = (this.height - 32) / 2 + 40;
        Text disguiseName = ConvenerDisguiseResolver.resolveDisguiseName(this.player, disguise.getDisguiseUuid());

        Text stateLine = Text.translatable(
                "hud.noellesroles.convener.current_disguise",
                disguiseName != null ? disguiseName : Text.translatable(convener.hasUnlockedMorphs()
                        ? "hud.noellesroles.convener.waiting"
                        : "hud.noellesroles.convener.locked")
        );
        Text progressLine = Text.translatable(
                "hud.noellesroles.convener.progress",
                convener.getSummonCount(),
                convener.getRequiredSummons()
        );

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, stateLine, centerX - MinecraftClient.getInstance().textRenderer.getWidth(stateLine) / 2, baseY, Noellesroles.CONVENER.color());
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, progressLine, centerX - MinecraftClient.getInstance().textRenderer.getWidth(progressLine) / 2, baseY + MinecraftClient.getInstance().textRenderer.fontHeight + 2, Noellesroles.CONVENER.color());
    }

    @Unique
    private void noellesroles$ensureConvenerWidgets() {
        if (!noellesroles$isConvener()) {
            return;
        }
        if (!this.noellesroles$pageInitialized) {
            this.noellesroles$pageInitialized = true;
            this.noellesroles$currentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.CONVENER_PAGE_KEY);
        }

        List<UUID> targets = noellesroles$collectDisplayTargets();
        if (!this.noellesroles$buttonTargets.equals(targets)) {
            noellesroles$replaceTargetButtons(targets);
        }
        noellesroles$refreshPagedLayout();
    }

    @Unique
    private boolean noellesroles$isConvener() {
        return this.player != null
                && this.player.getWorld() != null
                && GameWorldComponent.KEY.get(this.player.getWorld()).isRole(this.player, Noellesroles.CONVENER);
    }

    @Unique
    private void noellesroles$replaceTargetButtons(List<UUID> targets) {
        /*
         * Screen 没有稳定公开的“移除并销毁所有旧 child”封装给各映射版本共用。
         * 这里把旧按钮隐藏停用，再按新列表添加一批按钮；目标列表只会随召集次数少量增长，
         * 因此不会产生可感知的控件膨胀，同时避免误删其它职业也注入到同一个背包界面的控件。
         */
        for (ConvenerDisguiseButton button : this.noellesroles$convenerButtons) {
            button.visible = false;
            button.active = false;
        }
        this.noellesroles$convenerButtons.clear();
        this.noellesroles$buttonTargets.clear();
        this.noellesroles$buttonTargets.addAll(targets);

        int rowY = PlayerPageLayout.getPlayerRowY(this.height);
        for (UUID targetUuid : targets) {
            PlayerListEntry entry = ConvenerDisguiseResolver.resolvePlayerListEntry(targetUuid);
            ConvenerDisguiseButton button = new ConvenerDisguiseButton(
                    (LimitedInventoryScreen) (Object) this,
                    0,
                    rowY,
                    targetUuid,
                    targetUuid.equals(this.player.getUuid()),
                    entry
            );
            button.visible = false;
            button.active = false;
            this.noellesroles$convenerButtons.add(button);
            addDrawableChild(button);
        }
    }

    @Unique
    private void noellesroles$refreshPagedLayout() {
        int totalPlayers = this.noellesroles$convenerButtons.size();
        int totalPages = PlayerPageLayout.getTotalPageCount(totalPlayers);
        this.noellesroles$currentPage = Math.max(0, Math.min(this.noellesroles$currentPage, totalPages - 1));
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.CONVENER_PAGE_KEY, this.noellesroles$currentPage);

        int startIndex = this.noellesroles$currentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, totalPlayers);
        int visibleCount = Math.max(0, endIndex - startIndex);
        boolean showPrevious = this.noellesroles$currentPage > 0;
        boolean showNext = this.noellesroles$currentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int rowY = PlayerPageLayout.getPlayerRowY(this.height);
        int slotIndex = 0;

        for (ConvenerDisguiseButton button : this.noellesroles$convenerButtons) {
            button.visible = false;
            button.active = false;
        }

        if (showPrevious) {
            noellesroles$ensurePreviousPageWidget();
            this.noellesroles$previousPageWidget.visible = true;
            this.noellesroles$previousPageWidget.active = true;
            this.noellesroles$previousPageWidget.setX(groupStartX);
            this.noellesroles$previousPageWidget.setY(rowY);
            slotIndex++;
        } else if (this.noellesroles$previousPageWidget != null) {
            this.noellesroles$previousPageWidget.visible = false;
            this.noellesroles$previousPageWidget.active = false;
        }

        for (int index = startIndex; index < endIndex; index++) {
            ConvenerDisguiseButton button = this.noellesroles$convenerButtons.get(index);
            button.visible = true;
            button.active = true;
            button.setX(groupStartX + slotIndex * PlayerPageLayout.SLOT_APART);
            button.setY(rowY);
            slotIndex++;
        }

        if (showNext) {
            noellesroles$ensureNextPageWidget();
            this.noellesroles$nextPageWidget.visible = true;
            this.noellesroles$nextPageWidget.active = true;
            this.noellesroles$nextPageWidget.setX(groupStartX + slotIndex * PlayerPageLayout.SLOT_APART);
            this.noellesroles$nextPageWidget.setY(rowY);
        } else if (this.noellesroles$nextPageWidget != null) {
            this.noellesroles$nextPageWidget.visible = false;
            this.noellesroles$nextPageWidget.active = false;
        }
    }

    @Unique
    private void noellesroles$ensurePreviousPageWidget() {
        if (this.noellesroles$previousPageWidget != null) {
            return;
        }
        this.noellesroles$previousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                PlayerPageLayout.getPlayerRowY(this.height),
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.previous"),
                button -> {
                    this.noellesroles$currentPage = Math.max(0, this.noellesroles$currentPage - 1);
                    noellesroles$refreshPagedLayout();
                }
        ));
        this.noellesroles$previousPageWidget.visible = false;
        this.noellesroles$previousPageWidget.active = false;
    }

    @Unique
    private void noellesroles$ensureNextPageWidget() {
        if (this.noellesroles$nextPageWidget != null) {
            return;
        }
        this.noellesroles$nextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                PlayerPageLayout.getPlayerRowY(this.height),
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.next"),
                button -> {
                    int totalPages = PlayerPageLayout.getTotalPageCount(this.noellesroles$convenerButtons.size());
                    this.noellesroles$currentPage = Math.min(totalPages - 1, this.noellesroles$currentPage + 1);
                    noellesroles$refreshPagedLayout();
                }
        ));
        this.noellesroles$nextPageWidget.visible = false;
        this.noellesroles$nextPageWidget.active = false;
    }

    @Unique
    private void noellesroles$hideConvenerWidgets() {
        for (ConvenerDisguiseButton button : this.noellesroles$convenerButtons) {
            button.visible = false;
            button.active = false;
        }
        if (this.noellesroles$previousPageWidget != null) {
            this.noellesroles$previousPageWidget.visible = false;
            this.noellesroles$previousPageWidget.active = false;
        }
        if (this.noellesroles$nextPageWidget != null) {
            this.noellesroles$nextPageWidget.visible = false;
            this.noellesroles$nextPageWidget.active = false;
        }
    }

    @Unique
    private List<UUID> noellesroles$collectDisplayTargets() {
        ConvenerPlayerComponent convener = ConvenerPlayerComponent.KEY.get(this.player);
        ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(this.player);
        Set<UUID> orderedTargets = new LinkedHashSet<>();
        orderedTargets.add(this.player.getUuid());
        orderedTargets.addAll(convener.getUnlockedDisguises());
        if (disguise.getDisguiseUuid() != null) {
            orderedTargets.add(disguise.getDisguiseUuid());
        }

        List<UUID> sortedTargets = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            for (UUID onlineUuid : client.player.networkHandler.getPlayerUuids()) {
                if (orderedTargets.contains(onlineUuid)) {
                    sortedTargets.add(onlineUuid);
                }
            }
        }
        for (UUID uuid : orderedTargets) {
            if (!sortedTargets.contains(uuid)) {
                sortedTargets.add(uuid);
            }
        }

        sortedTargets.sort(Comparator.comparing(uuid -> !uuid.equals(this.player.getUuid())));
        return sortedTargets;
    }
}
