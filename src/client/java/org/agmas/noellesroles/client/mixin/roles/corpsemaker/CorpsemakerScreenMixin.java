package org.agmas.noellesroles.client.mixin.roles.corpsemaker;


import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.ui.common.PagedPlayerScreenState;
import org.agmas.noellesroles.client.ui.common.PlayerPageLayout;
import org.agmas.noellesroles.client.ui.common.PlayerPageSwitchWidget;
import org.agmas.noellesroles.client.ui.roles.corpsemaker.*;
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
public abstract class CorpsemakerScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {

    @Shadow @Final public ClientPlayerEntity player;

    @Unique private List<CorpsemakerPlayerWidget> playerWidgets = new ArrayList<>();
    @Unique private List<CorpsemakerDeathReasonWidget> deathReasonWidgets = new ArrayList<>();
    @Unique private CorpsemakerRoleInputWidget roleInputWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$corpsemakerPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget noellesroles$corpsemakerNextPageWidget;
    @Unique private int noellesroles$corpsemakerCurrentPage = 0;

    public CorpsemakerScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    void renderCorpsemakerHeads(CallbackInfo ci) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorldComponent.isRole(player, Noellesroles.CORPSEMAKER)) return;

        // 每次打开界面重置状态
        CorpsemakerState.reset();

        playerWidgets.clear();
        deathReasonWidgets.clear();
        noellesroles$corpsemakerCurrentPage = PagedPlayerScreenState.getPage(PagedPlayerScreenState.CORPSEMAKER_PAGE_KEY);

        int apart = 36;
        int shouldBeY = (this.height - 32) / 2;
        int y = shouldBeY + 80;

        // 创建所有玩家头像按钮
        var networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) return;

        List<UUID> playerUuids = new ArrayList<>(networkHandler.getPlayerUuids());
// 可选：排除自己（如需排除，取消下一行注释）
// playerUuids.removeIf(uuid -> uuid.equals(MinecraftClient.getInstance().player.getUuid()));

        int x = this.width / 2 - (playerUuids.size()) * apart / 2 + 9;
        for (int i = 0; i < playerUuids.size(); ++i) {
            UUID targetUuid = playerUuids.get(i);
            PlayerListEntry entry = networkHandler.getPlayerListEntry(targetUuid);
            if (entry == null) continue; // 理论上不会为空，但安全起见
            CorpsemakerPlayerWidget widget = new CorpsemakerPlayerWidget(
                    ((LimitedInventoryScreen) (Object) this),
                    0,
                    y,
                    targetUuid,
                    entry
            );
            playerWidgets.add(widget);
            addDrawableChild(widget);
        }

        noellesroles$corpsemakerPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.previous"),
                button -> {
                    noellesroles$corpsemakerCurrentPage--;
                    noellesroles$corpsemakerRefreshPage();
                }
        ));
        noellesroles$corpsemakerNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.noellesroles.pagination.next"),
                button -> {
                    noellesroles$corpsemakerCurrentPage++;
                    noellesroles$corpsemakerRefreshPage();
                }
        ));
        // 创建所有死亡原因按钮
        List<Item> deathReasons = new ArrayList<>();
        deathReasons.add(WatheItems.KNIFE);
        deathReasons.add(WatheItems.REVOLVER);
        deathReasons.add(WatheItems.GRENADE);
        deathReasons.add(WatheItems.BAT);
        deathReasons.add(WatheItems.POISON_VIAL);
        deathReasons.add(Items.OMINOUS_BOTTLE);
        // 这两个是 noellesroles 自己新增的伪造死因，按你的要求要插在外部模组死因之前。
        deathReasons.add(ModItems.THROWING_AXE);
        deathReasons.add(ModItems.TIMED_BOMB);
        if (FabricLoader.getInstance().isModLoaded("starexpress")) {
            deathReasons.add(Registries.ITEM.get(Identifier.of("starexpress", "tape")));
        }
        if (FabricLoader.getInstance().isModLoaded("stupid_express")) {
            deathReasons.add(Registries.ITEM.get(Identifier.of("stupid_express", "lighter")));
        }
        x = this.width / 2 - (deathReasons.size()) * apart / 2 + 9;
        for (int i = 0; i < deathReasons.size(); ++i) {
            CorpsemakerDeathReasonWidget widget = new CorpsemakerDeathReasonWidget(
                    ((LimitedInventoryScreen) (Object) this),
                    x + apart * i,
                    y,
                    deathReasons.get(i),
                    i
            );
            deathReasonWidgets.add(widget);
            addDrawableChild(widget);
        }

        // 创建角色输入框（位于屏幕中央）
        roleInputWidget = new CorpsemakerRoleInputWidget(
                ((LimitedInventoryScreen) (Object) this),
                textRenderer,
                (this.width / 2) - 100,
                y - 20
        );
        addDrawableChild(roleInputWidget);

        updateVisibility();
        noellesroles$corpsemakerRefreshPage();
    }

    @Inject(method = "render", at = @At("TAIL"))
    void updateCorpsemakerVisibility(CallbackInfo ci) {
        updateVisibility();
    }

    @Unique
    private void updateVisibility() {
        CorpsemakerPhase phase = CorpsemakerState.phase;
        for (CorpsemakerPlayerWidget widget : playerWidgets) {
            widget.visible = (phase == CorpsemakerPhase.PLAYER_SELECT);
        }
        for (CorpsemakerDeathReasonWidget widget : deathReasonWidgets) {
            widget.visible = (phase == CorpsemakerPhase.DEATH_REASON);
        }
        if (roleInputWidget != null) {
            roleInputWidget.visible = (phase == CorpsemakerPhase.ROLE_INPUT);
        }
        noellesroles$corpsemakerRefreshPage();
    }

    @Unique
    private void noellesroles$corpsemakerRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(playerWidgets.size());
        if (noellesroles$corpsemakerCurrentPage < 0) {
            noellesroles$corpsemakerCurrentPage = 0;
        }
        if (noellesroles$corpsemakerCurrentPage >= totalPages) {
            noellesroles$corpsemakerCurrentPage = totalPages - 1;
        }
        PagedPlayerScreenState.setPage(PagedPlayerScreenState.CORPSEMAKER_PAGE_KEY, noellesroles$corpsemakerCurrentPage);

        int startIndex = noellesroles$corpsemakerCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, playerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean playerSelectPhase = CorpsemakerState.phase == CorpsemakerPhase.PLAYER_SELECT;
        boolean showPrevious = playerSelectPhase && noellesroles$corpsemakerCurrentPage > 0;
        boolean showNext = playerSelectPhase && noellesroles$corpsemakerCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < playerWidgets.size(); i++) {
            CorpsemakerPlayerWidget widget = playerWidgets.get(i);
            boolean visible = playerSelectPhase && i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (noellesroles$corpsemakerPreviousPageWidget != null) {
            noellesroles$corpsemakerPreviousPageWidget.visible = showPrevious;
            noellesroles$corpsemakerPreviousPageWidget.active = showPrevious;
            noellesroles$corpsemakerPreviousPageWidget.setX(groupStartX);
            noellesroles$corpsemakerPreviousPageWidget.setY(y);
        }
        if (noellesroles$corpsemakerNextPageWidget != null) {
            noellesroles$corpsemakerNextPageWidget.visible = showNext;
            noellesroles$corpsemakerNextPageWidget.active = showNext;
            noellesroles$corpsemakerNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            noellesroles$corpsemakerNextPageWidget.setY(y);
        }
    }
}
