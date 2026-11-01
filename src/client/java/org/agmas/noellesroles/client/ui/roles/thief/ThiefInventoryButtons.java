package org.agmas.noellesroles.client.ui.roles.thief;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.thief.ThiefConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 小偷悦灵能力的两级背包按钮。 */
public final class ThiefInventoryButtons {
    private ThiefInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("thief", ThiefInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), NoellesRoleRegistry.THIEF)
                ? new Extension()
                : null;
    }

    private static final class Extension implements InventoryButtonExtension {
        private final NoellesInventoryButtonSupport.PagedButtons<ThiefPlayerWidget> playerButtons =
                new NoellesInventoryButtonSupport.PagedButtons<>("thief");
        private final Identifier countGroup = NoellesInventoryButtonSupport.id("inventory_group/thief_allay_count");

        @Override
        public void init(@NotNull InventoryButtonContext context) {
            ThiefInventoryState.reset();
            this.playerButtons.reset(context);
            context.clearGroup(this.countGroup);

            LimitedInventoryScreen screen = context.requireLimitedScreen();
            ClientPlayerEntity localPlayer = context.requirePlayer();
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            for (PlayerEntity candidate : localPlayer.getWorld().getPlayers()) {
                if (candidate.getUuid().equals(localPlayer.getUuid())
                        || !GameFunctions.isPlayerAliveAndSurvival(candidate)
                        || candidate.isSpectator()
                        || candidate.isCreative()) {
                    continue;
                }
                PlayerListEntry entry = NoellesInventoryButtonSupport.entry(localPlayer, candidate.getUuid());
                if (entry != null) {
                    this.playerButtons.addWidget(context,
                            new ThiefPlayerWidget(screen, 0, y, candidate.getUuid(), entry));
                }
            }
            this.playerButtons.addPageButtons(context);

            int startX = context.width() / 2 - InventoryButtonLayout.SLOT_APART;
            context.addWidget(this.countGroup, new ThiefAllayCountWidget(
                    startX, y, Items.IRON_INGOT.getDefaultStack(), ThiefConstants.ALLAY_MIN_COUNT));
            context.addWidget(this.countGroup, new ThiefAllayCountWidget(
                    startX + InventoryButtonLayout.SLOT_APART, y,
                    Items.GOLD_INGOT.getDefaultStack(), ThiefConstants.ALLAY_MIDDLE_COUNT));
            context.addWidget(this.countGroup, new ThiefAllayCountWidget(
                    startX + InventoryButtonLayout.SLOT_APART * 2, y,
                    Items.DIAMOND.getDefaultStack(), ThiefConstants.ALLAY_MAX_COUNT));
            updateVisibility(context);
        }

        @Override
        public void tick(@NotNull InventoryButtonContext context) {
            updateVisibility(context);
        }

        @Override
        public void render(@NotNull InventoryButtonContext context, @NotNull DrawContext drawContext,
                           int mouseX, int mouseY, float delta) {
            updateVisibility(context);
            Text title = Text.translatable(ThiefInventoryState.phase == ThiefInventoryPhase.PLAYER_SELECT
                    ? "ui.noellesroles.thief.allay.select_player"
                    : "ui.noellesroles.thief.allay.select_count");
            int centerX = context.width() / 2;
            int titleY = InventoryButtonLayout.getPlayerRowY(context.height())
                    - context.textRenderer().fontHeight
                    - ThiefConstants.INVENTORY_TITLE_OFFSET;
            drawContext.drawTextWithShadow(
                    context.textRenderer(),
                    title,
                    centerX - context.textRenderer().getWidth(title) / 2,
                    titleY,
                    NoellesRoleRegistry.THIEF.color()
            );
        }

        private void updateVisibility(InventoryButtonContext context) {
            boolean playerPhase = ThiefInventoryState.phase == ThiefInventoryPhase.PLAYER_SELECT;
            this.playerButtons.refresh(context, playerPhase);
            context.setGroupVisible(this.countGroup, !playerPhase);
        }
    }
}
