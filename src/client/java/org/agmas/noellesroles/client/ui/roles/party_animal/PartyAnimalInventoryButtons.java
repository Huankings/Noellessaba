package org.agmas.noellesroles.client.ui.roles.party_animal;

import dev.doctor4t.wathe.api.client.inventory.InventoryButtonContext;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonExtension;
import dev.doctor4t.wathe.api.client.inventory.InventoryButtonLayout;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtonSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.party_animal.PartyAnimalPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 派对狂背包按钮与类型说明文字。 */
public final class PartyAnimalInventoryButtons {
    private PartyAnimalInventoryButtons() {}

    public static void register() {
        NoellesInventoryButtonSupport.registerLimited("party_animal", PartyAnimalInventoryButtons::create);
    }

    private static @Nullable InventoryButtonExtension create(@NotNull InventoryButtonContext context) {
        return NoellesInventoryButtonSupport.isRole(context.requirePlayer(), NoellesRoleRegistry.PARTY_ANIMAL) ? new Extension() : null;
    }

    private static final class Extension implements InventoryButtonExtension {
        private PartyAnimalTypeWidget down;
        private PartyAnimalTypeWidget up;

        @Override
        public void init(@NotNull InventoryButtonContext context) {
            LimitedInventoryScreen screen = context.requireLimitedScreen();
            int y = InventoryButtonLayout.getPlayerRowY(context.height());
            int groupStartX = InventoryButtonLayout.getCenteredGroupStartX(context.width(), 2, false, false);
            down = context.addWidget(NoellesInventoryButtonSupport.id("inventory_group/party_animal"),
                    // 两个按钮使用 Wathe 标准 36 格间距和整组居中算法，与其它职业的图标行对齐。
                    new PartyAnimalTypeWidget(screen, groupStartX, y, Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack(), PartyAnimalPlayerComponent.PITCH_DOWN, Text.translatable("ui.noellesroles.party_animal.pitch_down")));
            up = context.addWidget(NoellesInventoryButtonSupport.id("inventory_group/party_animal"),
                    new PartyAnimalTypeWidget(screen, groupStartX + InventoryButtonLayout.SLOT_APART, y, Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE.getDefaultStack(), PartyAnimalPlayerComponent.PITCH_UP, Text.translatable("ui.noellesroles.party_animal.pitch_up")));
        }

        @Override
        public void render(@NotNull InventoryButtonContext context, @NotNull DrawContext drawContext, int mouseX, int mouseY, float delta) {
            PartyAnimalPlayerComponent state = PartyAnimalPlayerComponent.KEY.get(context.requirePlayer());
            Text first = Text.translatable("ui.noellesroles.party_animal.select_type");
            Text second = Text.translatable("ui.noellesroles.party_animal.current_type", state.getSelectedType() < 0
                    ? Text.translatable("ui.noellesroles.party_animal.pitch_down")
                    : Text.translatable("ui.noellesroles.party_animal.pitch_up"));
            int center = context.width() / 2;
            // 两行说明放在 Wathe 槽位上方，和 30×30 槽位边框保持间距，不再压住边框。
            int y = InventoryButtonLayout.getPlayerRowY(context.height()) - 32;
            drawContext.drawTextWithShadow(context.textRenderer(), first, center - context.textRenderer().getWidth(first) / 2, y, NoellesRoleRegistry.PARTY_ANIMAL.color());
            drawContext.drawTextWithShadow(context.textRenderer(), second, center - context.textRenderer().getWidth(second) / 2, y + context.textRenderer().fontHeight + 2, NoellesRoleRegistry.PARTY_ANIMAL.color());
        }
    }
}
