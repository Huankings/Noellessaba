package org.agmas.noellesroles.client.appearance.roles.amnesiac;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.jetbrains.annotations.NotNull;

/**
 * 失忆患者对准尸体时的能力提示。
 */
public final class AmnesiacBodyHudHandler {
    private AmnesiacBodyHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesAppearanceSupport.id("role_name/amnesiac_body"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    ClientPlayerEntity player = context.player();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (!gameWorld.isRole(player, NoellesRoleRegistry.AMNESIAC) || GameFunctions.isPlayerSpectatingOrCreative(player)) {
                        return;
                    }

                    PlayerBodyEntity body = RoleNameHudApi.findLookedAtBody(player, RoleNameHudApi.defaultLookRange(player));
                    if (body != null) {
                        drawCentered(context.renderer(), context.drawContext(), Text.translatable("hud.noellesroles.amnesiac.select_body"), 32, NoellesRoleRegistry.AMNESIAC.color());
                    }
                }
        );
    }

    private static void drawCentered(@NotNull TextRenderer renderer, @NotNull DrawContext context, @NotNull Text text, int y, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2.0F, context.getScaledWindowHeight() / 2.0F + 6.0F, 0.0F);
        context.getMatrices().scale(0.6F, 0.6F, 1.0F);
        context.drawTextWithShadow(renderer, text, -renderer.getWidth(text) / 2, y, color);
        context.getMatrices().pop();
    }
}
