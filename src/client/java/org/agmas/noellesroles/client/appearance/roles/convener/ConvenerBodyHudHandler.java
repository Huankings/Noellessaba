package org.agmas.noellesroles.client.appearance.roles.convener;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.jetbrains.annotations.NotNull;

/**
 * 召集者对准尸体时的召集提示。
 */
public final class ConvenerBodyHudHandler {
    private ConvenerBodyHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesAppearanceSupport.id("role_name/convener_body"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    ClientPlayerEntity player = context.player();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (!gameWorld.isRole(player, NoellesRoleRegistry.CONVENER) || GameFunctions.isPlayerSpectatingOrCreative(player)) {
                        return;
                    }

                    PlayerBodyEntity body = RoleNameHudApi.findLookedAtBody(player, RoleNameHudApi.defaultLookRange(player));
                    if (body == null) {
                        return;
                    }

                    AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
                    Text text = ability.cooldown > 0
                            ? Text.translatable("hud.noellesroles.convener.cooldown", Math.max(0, (ability.cooldown + 19) / 20))
                            : Text.translatable("hud.noellesroles.convener.select_body");
                    drawCentered(context.renderer(), context.drawContext(), text, 32, NoellesRoleRegistry.CONVENER.color());
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
