package org.agmas.noellesroles.client.ui.roles.thief;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;
import org.agmas.noellesroles.roles.thief.ThiefPlayerComponent;
import org.agmas.noellesroles.roles.thief.ThiefConstants;

import java.awt.Color;
import java.util.UUID;

/** 小偷背包第一阶段的存活玩家头像按钮。 */
public final class ThiefPlayerWidget extends ButtonWidget {
    private final UUID targetUuid;
    private final PlayerListEntry entry;

    public ThiefPlayerWidget(LimitedInventoryScreen screen, int x, int y, UUID targetUuid, PlayerListEntry entry) {
        super(x, y, ThiefConstants.INVENTORY_BUTTON_SIZE, ThiefConstants.INVENTORY_BUTTON_SIZE,
                Text.literal(entry.getProfile().getName()), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) {
                return;
            }
            ThiefPlayerComponent component = ThiefPlayerComponent.KEY.get(client.player);
            if (component.getSummonCooldownTicks() > 0) {
                client.player.sendMessage(Text.translatable(
                        "message.noellesroles.thief.allay.cooldown",
                        Math.max(0, (component.getSummonCooldownTicks()
                                + ThiefConstants.COOLDOWN_SECONDS_ROUNDING_OFFSET)
                                / ThiefConstants.TICKS_PER_SECOND)
                ), true);
                return;
            }
            if (component.hasLivingAllayState()) {
                client.player.sendMessage(
                        Text.translatable("message.noellesroles.thief.allay.active_failure")
                                .withColor(ThiefConstants.FAILURE_ACTIONBAR_COLOR),
                        true
                );
                return;
            }
            ThiefInventoryState.selectedTarget = targetUuid;
            ThiefInventoryState.phase = ThiefInventoryPhase.COUNT_SELECT;
        }, DEFAULT_NARRATION_SUPPLIER);
        this.targetUuid = targetUuid;
        this.entry = entry;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        ThiefPlayerComponent component = ThiefPlayerComponent.KEY.get(client.player);
        boolean blocked = component.getSummonCooldownTicks() > 0 || component.hasLivingAllayState();
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
        PlayerSkinDrawer.draw(
                context,
                PlayerHeadTextureHelper.resolveStableSkinTextures(this.targetUuid, this.entry).texture(),
                getX(),
                getY(),
                ThiefConstants.INVENTORY_BUTTON_SIZE
        );
        if (blocked) {
            context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (component.getSummonCooldownTicks() > 0) {
                context.drawText(client.textRenderer,
                        String.valueOf(Math.max(0, (component.getSummonCooldownTicks()
                                + ThiefConstants.COOLDOWN_SECONDS_ROUNDING_OFFSET)
                                / ThiefConstants.TICKS_PER_SECOND)),
                        getX(), getY(), Color.RED.getRGB(), true);
            }
        }
        if (isHovered()) {
            int color = ThiefConstants.INVENTORY_HOVER_COLOR;
            context.fillGradient(RenderLayer.getGuiOverlay(), getX(), getY(),
                    getX() + ThiefConstants.INVENTORY_BUTTON_SIZE,
                    getY() + ThiefConstants.INVENTORY_BUTTON_SIZE, color, color, 0);
            context.drawTooltip(client.textRenderer, getMessage(), mouseX, mouseY);
        }
    }
}
