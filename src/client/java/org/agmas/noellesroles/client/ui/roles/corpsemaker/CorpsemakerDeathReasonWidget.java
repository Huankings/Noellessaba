package org.agmas.noellesroles.client.ui.roles.corpsemaker;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;

public class CorpsemakerDeathReasonWidget extends ButtonWidget {

    private final LimitedInventoryScreen screen;
    private final ItemStack deathReasonStack;
    private final String deathReasonId;

    public CorpsemakerDeathReasonWidget(@NotNull LimitedInventoryScreen screen, int x, int y, @NotNull Item deathReason, int index) {
        super(x, y, 16, 16, Text.literal(""), (button) -> {
            CorpsemakerState.selectedDeathReason = getDeathReasonId(deathReason);
            CorpsemakerState.phase = CorpsemakerPhase.ROLE_INPUT;
        }, DEFAULT_NARRATION_SUPPLIER);
        this.screen = screen;
        this.deathReasonStack = deathReason.getDefaultStack();
        this.deathReasonId = getDeathReasonId(deathReason);
    }

    private static String getDeathReasonId(@NotNull Item item) {
        if (item == WatheItems.KNIFE) return "wathe:knife_stab";
        if (item == WatheItems.REVOLVER) return "wathe:gun_shot";
        if (item == WatheItems.GRENADE) return "wathe:grenade";
        if (item == WatheItems.BAT) return "wathe:bat_hit";
        if (item == WatheItems.POISON_VIAL) return "wathe:poison";
        if (item == Items.OMINOUS_BOTTLE) return "noellesroles:voodoo";
        if (item == ModItems.THROWING_AXE) return "noellesroles:throwing_axe";
        if (item == ModItems.TIMED_BOMB) return "noellesroles:bomb";
        if (FabricLoader.getInstance().isModLoaded("starexpress")) {
            if (item == Registries.ITEM.get(Identifier.of("starexpress", "tape"))) return "starexpress:silenced_and_outside";
        }
        if (FabricLoader.getInstance().isModLoaded("stupid_express")) {
            if (item == Registries.ITEM.get(Identifier.of("stupid_express", "lighter"))) return "stupid_express:ignited";
        }
        return "wathe:generic";
    }

    @Override
    protected void renderWidget(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
        context.drawItem(deathReasonStack, this.getX(), this.getY());
        context.drawGuiTexture(ShopEntry.Type.WEAPON.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        if (this.isHovered()) {
            this.drawShopSlotHighlight(context, this.getX(), this.getY());
            String translationKey = "death_reason." + deathReasonId.replace(':', '.');
            context.drawTooltip(MinecraftClient.getInstance().textRenderer,
                    Text.translatable(translationKey),
                    mouseX, mouseY);
        }
    }

    private void drawShopSlotHighlight(@NotNull DrawContext context, int x, int y) {
        int color = -1862287543;
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, 0);
    }
}
