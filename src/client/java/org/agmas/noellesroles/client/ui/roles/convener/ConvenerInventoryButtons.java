package org.agmas.noellesroles.client.ui.roles.convener;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.agmas.noellesroles.client.roles.convener.ConvenerDisguiseResolver;
import org.agmas.noellesroles.roles.convener.ConvenerDisguiseComponent;
import org.agmas.noellesroles.roles.convener.ConvenerPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ConvenerInventoryButtons {
    private ConvenerInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("convener", ConvenerInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), NoellesRoleRegistry.CONVENER) ? new Extension() : null;
    }

    private static final class Extension implements InventoryButtonExtension {
        private final NoellesInventoryButtonSupport.PagedButtons<ConvenerPlayerWidget> buttons = new NoellesInventoryButtonSupport.PagedButtons<>("convener");
        private final List<UUID> buttonTargets = new ArrayList<>();

        @Override
        public void init(@NotNull InventoryButtonContext context) {
            this.buttons.reset(context);
            this.ensureWidgets(context);
        }

        @Override
        public void tick(@NotNull InventoryButtonContext context) {
            this.ensureWidgets(context);
        }

        @Override
        public void render(@NotNull InventoryButtonContext context, @NotNull DrawContext drawContext, int mouseX, int mouseY, float delta) {
            this.ensureWidgets(context);
            ClientPlayerEntity player = context.requirePlayer();
            ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(player);
            ConvenerPlayerComponent convener = ConvenerPlayerComponent.KEY.get(player);
            Text disguiseName = ConvenerDisguiseResolver.resolveDisguiseName(player, disguise.getDisguiseUuid());
            Text stateLine = Text.translatable(
                    "hud.noellesroles.convener.current_disguise",
                    disguiseName != null ? disguiseName : Text.translatable(convener.hasUnlockedMorphs()
                            ? "hud.noellesroles.convener.waiting"
                            : "hud.noellesroles.convener.locked")
            );
            Text progressLine = Text.translatable("hud.noellesroles.convener.progress", convener.getSummonCount(), convener.getRequiredSummons());
            int centerX = context.width() / 2;
            int baseY = (context.height() - 32) / 2 + 40;
            drawContext.drawTextWithShadow(context.textRenderer(), stateLine, centerX - context.textRenderer().getWidth(stateLine) / 2, baseY, NoellesRoleRegistry.CONVENER.color());
            drawContext.drawTextWithShadow(context.textRenderer(), progressLine, centerX - context.textRenderer().getWidth(progressLine) / 2, baseY + context.textRenderer().fontHeight + 2, NoellesRoleRegistry.CONVENER.color());
        }

        private void ensureWidgets(InventoryButtonContext context) {
            List<UUID> targets = this.collectTargets(context.requirePlayer());
            if (!this.buttonTargets.equals(targets)) {
                /*
                 * 召集者的伪装名单会在背包打开期间动态变化。
                 * 这里比较 UUID 列表后整体重建 group，让新增/删除的头像和翻页按钮同步刷新。
                 */
                this.buttons.reset(context);
                this.buttonTargets.clear();
                this.buttonTargets.addAll(targets);
                LimitedInventoryScreen screen = context.requireLimitedScreen();
                ClientPlayerEntity player = context.requirePlayer();
                int y = InventoryButtonLayout.getPlayerRowY(context.height());
                for (UUID targetUuid : targets) {
                    this.buttons.addWidget(context, new ConvenerPlayerWidget(
                            screen,
                            0,
                            y,
                            targetUuid,
                            targetUuid.equals(player.getUuid()),
                            ConvenerDisguiseResolver.resolvePlayerListEntry(targetUuid)
                    ));
                }
                this.buttons.addPageButtons(context);
            }
            this.buttons.refresh(context, true);
        }

        private List<UUID> collectTargets(ClientPlayerEntity player) {
            ConvenerPlayerComponent convener = ConvenerPlayerComponent.KEY.get(player);
            ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(player);
            Set<UUID> orderedTargets = new LinkedHashSet<>();
            orderedTargets.add(player.getUuid());
            orderedTargets.addAll(convener.getUnlockedDisguises());
            if (disguise.getDisguiseUuid() != null) {
                orderedTargets.add(disguise.getDisguiseUuid());
            }

            List<UUID> sortedTargets = new ArrayList<>();
            for (UUID onlineUuid : NoellesInventoryButtonSupport.onlineUuids(player)) {
                if (orderedTargets.contains(onlineUuid)) {
                    sortedTargets.add(onlineUuid);
                }
            }
            for (UUID uuid : orderedTargets) {
                if (!sortedTargets.contains(uuid)) {
                    sortedTargets.add(uuid);
                }
            }
            sortedTargets.sort(Comparator.comparing(uuid -> !uuid.equals(player.getUuid())));
            return sortedTargets;
        }
    }
}
