package org.agmas.noellesroles.client.ui.common;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * 玩家头像分页按钮。
 *
 * 这里故意不复用任何“玩家头像按钮”类，而是单独做一个翻页控件。
 * 这样点击上一页/下一页时，只会触发翻页回调，不会误走职业技能的目标选择逻辑。
 * 这正是为了避免像交换者那种“点翻页却被当成选中玩家”的串状态问题。
 */
public class PlayerPageSwitchWidget extends ButtonWidget {
    /**
     * 按钮中间要渲染的物品图标。
     * 左边会放紫色染料，右边会放黄绿色染料。
     */
    private final ItemStack iconStack;

    /**
     * 鼠标悬停时显示的文本。
     * 文本本身走语言文件，方便后续统一本地化。
     */
    private final Text tooltipText;

    public PlayerPageSwitchWidget(int x, int y, ItemStack iconStack, Text tooltipText, PressAction onPress) {
        super(x, y, 16, 16, tooltipText, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.iconStack = iconStack;
        this.tooltipText = tooltipText;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染一个和其它职业按钮风格一致的外框，再把染料物品放进框内。
        context.drawGuiTexture(ShopEntry.Type.TOOL.getTexture(), getX() - 7, getY() - 7, 30, 30);
        context.drawItem(iconStack, getX(), getY());

        if (isHovered()) {
            drawHighlight(context);
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltipText, mouseX, mouseY);
        }
    }

    private void drawHighlight(DrawContext context) {
        int color = -1862287543;
        int x = getX();
        int y = getY();
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, 0);
    }
}
