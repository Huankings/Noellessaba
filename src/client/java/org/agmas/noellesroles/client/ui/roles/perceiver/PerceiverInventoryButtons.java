package org.agmas.noellesroles.client.ui.roles.perceiver;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.perceiver.PerceiverConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 感知者有限背包中的在线玩家单选列表。 */
public final class PerceiverInventoryButtons {
    private PerceiverInventoryButtons() {
    }

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("perceiver", PerceiverInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), NoellesRoleRegistry.PERCEIVER)
                ? new Extension()
                : null;
    }

    private static final class Extension extends NoellesInventoryButtonSupport.PagedExtension<PerceiverPlayerWidget> {
        private Extension() {
            super("perceiver");
        }

        @Override
        protected void populate(
                @NotNull InventoryButtonContext context,
                @NotNull LimitedInventoryScreen screen,
                @NotNull ClientPlayerEntity player
        ) {
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            for (UUID targetUuid : NoellesInventoryButtonSupport.onlineUuids(player)) {
                /* 玩家列表同时保留存活与非存活在线玩家，但需求明确排除感知者自己。 */
                if (targetUuid.equals(player.getUuid())) {
                    continue;
                }
                this.buttons.addWidget(context, new PerceiverPlayerWidget(
                        screen,
                        0,
                        y,
                        targetUuid,
                        NoellesInventoryButtonSupport.entry(player, targetUuid)
                ));
            }
        }

        @Override
        public void render(
                @NotNull InventoryButtonContext context,
                @NotNull DrawContext drawContext,
                int mouseX,
                int mouseY,
                float delta
        ) {
            Text text = Text.translatable("hud.perceiver.player_selection");
            drawContext.drawTextWithShadow(
                    context.textRenderer(),
                    text,
                    context.width() / 2 - context.textRenderer().getWidth(text) / 2,
                    (context.height() - 32) / 2 + 40,
                    PerceiverConstants.ROLE_COLOR
            );
        }
    }
}
