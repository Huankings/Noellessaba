package org.agmas.noellesroles.client.inventory;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonApi;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.api.client.inventory.InventoryPageState;
import dev.doctor4t.wathe.api.client.inventory.InventoryPageSwitchWidget;
import dev.doctor4t.wathe.api.client.inventory.InventoryScreenType;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * NoellesRoles 背包按钮共享工具。
 *
 * <p>每个职业的按钮已经拆到自己的职业包里；这里仅放通用的 Wathe API 注册、
 * 在线玩家查询和分页按钮布局，避免各职业复制同一套屏幕生命周期代码。</p>
 */
public final class NoellesInventoryButtonSupport {
    private NoellesInventoryButtonSupport() {
    }

    public static void registerLimited(@NotNull String path, @NotNull ExtensionFactory factory) {
        InventoryButtonApi.registerProvider(id("inventory/" + path), InventoryButtonApi.DEFAULT_PRIORITY, context -> {
            if (context.type() != InventoryScreenType.LIMITED || context.player() == null || context.limitedScreen() == null) {
                return null;
            }
            return factory.create(context);
        });
    }

    public static Identifier id(String path) {
        return Identifier.of(NoellesRolesCore.MOD_ID, path);
    }

    public static boolean isRole(@NotNull ClientPlayerEntity player, @NotNull Role role) {
        return GameWorldComponent.KEY.get(player.getWorld()).isRole(player, role);
    }

    public static List<UUID> onlineUuids(@NotNull ClientPlayerEntity player) {
        ClientPlayNetworkHandler networkHandler = player.networkHandler;
        return networkHandler == null ? List.of() : new ArrayList<>(networkHandler.getPlayerUuids());
    }

    public static @Nullable PlayerListEntry entry(@NotNull ClientPlayerEntity player, @NotNull UUID uuid) {
        ClientPlayNetworkHandler networkHandler = player.networkHandler;
        return networkHandler == null ? null : networkHandler.getPlayerListEntry(uuid);
    }

    public static @Nullable AbstractClientPlayerEntity clientEntity(@NotNull UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }
        PlayerEntity player = client.world.getPlayerByUuid(uuid);
        return player instanceof AbstractClientPlayerEntity clientPlayer ? clientPlayer : null;
    }

    public interface ExtensionFactory {
        @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context);
    }

    public static final class PagedButtons<W extends ClickableWidget> {
        private final Identifier pageKey;
        private final Identifier groupKey;
        private final List<W> widgets = new ArrayList<>();
        private InventoryPageSwitchWidget previous;
        private InventoryPageSwitchWidget next;
        private int currentPage;

        public PagedButtons(String key) {
            this.pageKey = id("inventory_page/" + key);
            this.groupKey = id("inventory_group/" + key);
        }

        public void reset(@NotNull InventoryButtonContext context) {
            context.clearGroup(this.groupKey);
            this.widgets.clear();
            this.previous = null;
            this.next = null;
            this.currentPage = InventoryPageState.getPage(this.pageKey);
        }

        public void addWidget(@NotNull InventoryButtonContext context, @NotNull W widget) {
            this.widgets.add(context.addWidget(this.groupKey, widget));
        }

        public void addPageButtons(@NotNull InventoryButtonContext context) {
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            this.previous = context.addWidget(this.groupKey, new InventoryPageSwitchWidget(
                    0,
                    y,
                    Items.PURPLE_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.previous"),
                    button -> {
                        this.currentPage--;
                        this.refresh(context, true);
                    }
            ));
            this.next = context.addWidget(this.groupKey, new InventoryPageSwitchWidget(
                    0,
                    y,
                    Items.LIME_DYE.getDefaultStack(),
                    Text.translatable("ui.noellesroles.pagination.next"),
                    button -> {
                        this.currentPage++;
                        this.refresh(context, true);
                    }
            ));
            this.refresh(context, true);
        }

        public void refresh(@NotNull InventoryButtonContext context, boolean selectionVisible) {
            int totalPages = InventoryButtonLayout.getTotalPageCount(this.widgets.size());
            this.currentPage = Math.max(0, Math.min(this.currentPage, totalPages - 1));
            InventoryPageState.setPage(this.pageKey, this.currentPage);

            int startIndex = this.currentPage * InventoryButtonLayout.PLAYERS_PER_PAGE;
            int endIndex = Math.min(startIndex + InventoryButtonLayout.PLAYERS_PER_PAGE, this.widgets.size());
            int visibleCount = Math.max(0, endIndex - startIndex);
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            boolean showPrevious = selectionVisible && this.currentPage > 0;
            boolean showNext = selectionVisible && this.currentPage < totalPages - 1;
            int groupStartX = InventoryButtonLayout.getCenteredGroupStartX(context.width(), visibleCount, showPrevious, showNext);
            int playerStartX = groupStartX + (showPrevious ? InventoryButtonLayout.SLOT_APART : 0);

            for (int i = 0; i < this.widgets.size(); i++) {
                W widget = this.widgets.get(i);
                boolean visible = selectionVisible && i >= startIndex && i < endIndex;
                widget.visible = visible;
                widget.active = visible;
                if (visible) {
                    int visibleIndex = i - startIndex;
                    widget.setX(playerStartX + visibleIndex * InventoryButtonLayout.SLOT_APART);
                    widget.setY(y);
                }
            }

            if (this.previous != null) {
                this.previous.visible = showPrevious;
                this.previous.active = showPrevious;
                this.previous.setX(groupStartX);
                this.previous.setY(y);
            }
            if (this.next != null) {
                this.next.visible = showNext;
                this.next.active = showNext;
                this.next.setX(playerStartX + visibleCount * InventoryButtonLayout.SLOT_APART);
                this.next.setY(y);
            }
        }
    }

    public abstract static class PagedExtension<W extends ClickableWidget> implements InventoryButtonExtension {
        protected final PagedButtons<W> buttons;

        protected PagedExtension(String key) {
            this.buttons = new PagedButtons<>(key);
        }

        @Override
        public final void init(@NotNull InventoryButtonContext context) {
            this.buttons.reset(context);
            this.populate(context, context.requireLimitedScreen(), context.requirePlayer());
            this.buttons.addPageButtons(context);
            this.buttons.refresh(context, this.selectionVisible(context.requirePlayer()));
        }

        @Override
        public void tick(@NotNull InventoryButtonContext context) {
            this.buttons.refresh(context, this.selectionVisible(context.requirePlayer()));
        }

        protected boolean selectionVisible(ClientPlayerEntity player) {
            return true;
        }

        protected abstract void populate(@NotNull InventoryButtonContext context,
                                         @NotNull LimitedInventoryScreen screen,
                                         @NotNull ClientPlayerEntity player);
    }
}
