package org.agmas.noellesroles.client.ui.roles.thief;

import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.agmas.noellesroles.packet.role.thief.ThiefAllaySummonC2SPacket;
import org.agmas.noellesroles.roles.thief.ThiefPlayerComponent;
import org.agmas.noellesroles.roles.thief.ThiefConstants;

/** 小偷背包第二阶段的悦灵数量按钮。 */
public final class ThiefAllayCountWidget extends ButtonWidget {
    private final ItemStack icon;
    private final int count;

    public ThiefAllayCountWidget(int x, int y, ItemStack icon, int count) {
        super(x, y, ThiefConstants.INVENTORY_BUTTON_SIZE, ThiefConstants.INVENTORY_BUTTON_SIZE,
                Text.translatable("ui.noellesroles.thief.allay.count", count), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || ThiefInventoryState.selectedTarget == null) {
                return;
            }
            ThiefPlayerComponent component = ThiefPlayerComponent.KEY.get(client.player);
            if (component.getSummonCooldownTicks() > 0 || component.hasLivingAllayState()) {
                return;
            }
            ClientPlayNetworking.send(new ThiefAllaySummonC2SPacket(ThiefInventoryState.selectedTarget, count));
            client.setScreen(null);
        }, DEFAULT_NARRATION_SUPPLIER);
        this.icon = icon.copy();
        this.count = count;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        ThiefPlayerComponent component = ThiefPlayerComponent.KEY.get(client.player);
        boolean blocked = component.getSummonCooldownTicks() > 0 || component.hasLivingAllayState();
        this.active = !blocked;
        if (blocked) {
            context.setShaderColor(
                    ThiefConstants.INVENTORY_BLOCKED_SHADER_RGB,
                    ThiefConstants.INVENTORY_BLOCKED_SHADER_RGB,
                    ThiefConstants.INVENTORY_BLOCKED_SHADER_RGB,
                    ThiefConstants.INVENTORY_BLOCKED_SHADER_ALPHA
            );
        }
        context.drawGuiTexture(
                ShopEntry.Type.WEAPON.getTexture(),
                getX() - ThiefConstants.INVENTORY_BUTTON_FRAME_OFFSET,
                getY() - ThiefConstants.INVENTORY_BUTTON_FRAME_OFFSET,
                ThiefConstants.INVENTORY_BUTTON_FRAME_SIZE,
                ThiefConstants.INVENTORY_BUTTON_FRAME_SIZE
        );
        context.drawItem(this.icon, getX(), getY());
        if (blocked) {
            context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        if (isHovered()) {
            int color = ThiefConstants.INVENTORY_HOVER_COLOR;
            context.fillGradient(RenderLayer.getGuiOverlay(), getX(), getY(),
                    getX() + ThiefConstants.INVENTORY_BUTTON_SIZE,
                    getY() + ThiefConstants.INVENTORY_BUTTON_SIZE, color, color, 0);
            context.drawTooltip(client.textRenderer,
                    Text.translatable("ui.noellesroles.thief.allay.count", this.count), mouseX, mouseY);
        }
    }
}
