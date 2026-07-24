package org.agmas.noellesroles.client.appearance.roles.arsonist;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.arsonist.DousedPlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 纵火犯准心浇油状态提示。
 */
public final class ArsonistTargetHudHandler {
    private ArsonistTargetHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesAppearanceSupport.id("role_name/arsonist_target"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    ClientPlayerEntity player = context.player();
                    PlayerEntity target = context.targetPlayer();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (target == null
                            || !gameWorld.isRole(player, NoellesRoleRegistry.ARSONIST)
                            || GameFunctions.isPlayerSpectatingOrCreative(player)) {
                        return;
                    }

                    boolean doused = DousedPlayerComponent.KEY.get(target).isDoused();
                    Text text = Text.translatable(doused
                            ? "hud.noellesroles.arsonist.doused.true"
                            : "hud.noellesroles.arsonist.doused.false");
                    drawCentered(context.renderer(), context.drawContext(), text, 32, doused ? NoellesRoleRegistry.ARSONIST.color() : 0xAAAAAA);
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
