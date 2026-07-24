package org.agmas.noellesroles.client.ui.roles.corpsemaker;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CorpsemakerInventoryButtons {
    private CorpsemakerInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("corpsemaker", CorpsemakerInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), Noellesroles.CORPSEMAKER) ? new Extension() : null;
    }

    private static final class Extension implements InventoryButtonExtension {
        private final NoellesInventoryButtonSupport.PagedButtons<CorpsemakerPlayerWidget> playerButtons = new NoellesInventoryButtonSupport.PagedButtons<>("corpsemaker");
        private final Identifier deathReasonGroup = NoellesInventoryButtonSupport.id("inventory_group/corpsemaker_death_reasons");
        private CorpsemakerRoleInputWidget roleInputWidget;

        @Override
        public void init(@NotNull InventoryButtonContext context) {
            LimitedInventoryScreen screen = context.requireLimitedScreen();
            ClientPlayerEntity player = context.requirePlayer();
            CorpsemakerState.reset();
            this.playerButtons.reset(context);

            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            for (UUID targetUuid : NoellesInventoryButtonSupport.onlineUuids(player)) {
                PlayerListEntry entry = NoellesInventoryButtonSupport.entry(player, targetUuid);
                if (entry != null) {
                    this.playerButtons.addWidget(context, new CorpsemakerPlayerWidget(screen, 0, y, targetUuid, entry));
                }
            }
            this.playerButtons.addPageButtons(context);

            List<Item> deathReasons = getDeathReasons();
            int x = context.width() / 2 - deathReasons.size() * InventoryButtonLayout.SLOT_APART / 2 + InventoryButtonLayout.SLOT_X_OFFSET;
            for (int i = 0; i < deathReasons.size(); i++) {
                context.addWidget(this.deathReasonGroup, new CorpsemakerDeathReasonWidget(screen, x + InventoryButtonLayout.SLOT_APART * i, y, deathReasons.get(i), i));
            }

            this.roleInputWidget = context.addWidget(NoellesInventoryButtonSupport.id("inventory_group/corpsemaker_role_input"), new CorpsemakerRoleInputWidget(screen, context.textRenderer(), context.width() / 2 - 100, y - 20));
            this.updateVisibility(context);
        }

        @Override
        public void tick(@NotNull InventoryButtonContext context) {
            this.updateVisibility(context);
        }

        @Override
        public void render(@NotNull InventoryButtonContext context, @NotNull DrawContext drawContext, int mouseX, int mouseY, float delta) {
            this.updateVisibility(context);
        }

        @Override
        public boolean allowInventoryKeyClose(@NotNull InventoryButtonContext context, int keyCode, int scanCode) {
            return CorpsemakerState.phase != CorpsemakerPhase.ROLE_INPUT;
        }

        private void updateVisibility(InventoryButtonContext context) {
            CorpsemakerPhase phase = CorpsemakerState.phase;
            this.playerButtons.refresh(context, phase == CorpsemakerPhase.PLAYER_SELECT);
            context.setGroupVisible(this.deathReasonGroup, phase == CorpsemakerPhase.DEATH_REASON);
            if (this.roleInputWidget != null) {
                boolean visible = phase == CorpsemakerPhase.ROLE_INPUT;
                this.roleInputWidget.visible = visible;
                this.roleInputWidget.active = visible;
            }
        }
    }

    private static List<Item> getDeathReasons() {
        List<Item> deathReasons = new ArrayList<>();
        deathReasons.add(WatheItems.KNIFE);
        deathReasons.add(WatheItems.REVOLVER);
        deathReasons.add(WatheItems.GRENADE);
        deathReasons.add(WatheItems.BAT);
        deathReasons.add(WatheItems.POISON_VIAL);
        deathReasons.add(Items.OMINOUS_BOTTLE);
        deathReasons.add(ModItems.THROWING_AXE);
        deathReasons.add(ModItems.TIMED_BOMB);
        if (FabricLoader.getInstance().isModLoaded("starexpress")) {
            deathReasons.add(Registries.ITEM.get(Identifier.of("starexpress", "tape")));
        }
        deathReasons.add(ModItems.LIGHTER);
        return deathReasons;
    }
}
