package org.agmas.noellesroles.client.appearance.roles.necromancer;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.coroner.CoronerConstants;
import org.agmas.noellesroles.roles.necromancer.NecromancerWorldComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 死灵法师的尸体复活提示。
 *
 * <p>这块和验尸官一样，本质上都是“准心对着尸体时显示额外信息”，
 * 所以直接挂到 Wathe 的 extra HUD 上最稳。</p>
 */
public final class NecromancerBodyHudHandler {
    private NecromancerBodyHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesAppearanceSupport.id("role_name/necromancer_body"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    ClientPlayerEntity player = context.player();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (!gameWorld.isRole(player, Noellesroles.NECROMANCER) || GameFunctions.isPlayerSpectatingOrCreative(player)) {
                        return;
                    }

                    PlayerBodyEntity body = RoleNameHudApi.findLookedAtBody(player, RoleNameHudApi.defaultLookRange(player));
                    if (body == null) {
                        return;
                    }

                    Text status = Text.translatable("hud.noellesroles.necromancer.possible_revive");
                    NecromancerWorldComponent necromancerWorld = NecromancerWorldComponent.KEY.get(player.getWorld());
                    if (necromancerWorld.getAvailableRevives() < 1) {
                        status = Text.translatable("hud.noellesroles.necromancer.no_possible_revive");
                    }

                    AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
                    if (ability.cooldown > 0) {
                        status = Text.translatable("hud.noellesroles.necromancer.cooldown", ability.cooldown / 20);
                    }

                    drawCentered(context.renderer(), context.drawContext(), status, 32, Noellesroles.NECROMANCER.color());
                }
        );
    }

    private static void drawCentered(@NotNull TextRenderer renderer,
                                     @NotNull DrawContext context,
                                     @NotNull Text text,
                                     int y,
                                     int color) {
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2.0F, context.getScaledWindowHeight() / 2.0F + CoronerConstants.HUD_TRANSLATE_Y, 0.0F);
        context.getMatrices().scale(0.6F, 0.6F, 1.0F);
        context.drawTextWithShadow(renderer, text, -renderer.getWidth(text) / 2, y, color);
        context.getMatrices().pop();
    }
}
