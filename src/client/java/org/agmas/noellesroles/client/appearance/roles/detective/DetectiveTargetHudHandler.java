package org.agmas.noellesroles.client.appearance.roles.detective;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.detective.DetectiveConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 侦探准心目标能力提示。
 */
public final class DetectiveTargetHudHandler {
    private DetectiveTargetHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesAppearanceSupport.id("role_name/detective_target"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    ClientPlayerEntity player = context.player();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (!gameWorld.isRole(player, NoellesRoleRegistry.DETECTIVE)
                            || !GameFunctions.isPlayerAliveAndSurvival(player)) {
                        return;
                    }

                    PlayerEntity target = context.targetPlayer();
                    if (target == null
                            || !GameFunctions.isPlayerAliveAndSurvival(target)
                            || NoellesrolesClient.abilityBind == null) {
                        return;
                    }

                    AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
                    PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
                    if (ability.cooldown > 0 || shop.balance < DetectiveConstants.ABILITY_PRICE) {
                        return;
                    }

                    /*
                     * 这里沿用 Wathe RoleNameHudApi 已算好的准心目标，只负责显示提示。
                     * 侦探实际调查仍在服务端 DetectiveAbility 内重新做射线校验，客户端提示不会成为可信数据。
                     */
                    Text targetInfo = Text.translatable(
                            "hud.noellesroles.detective.target",
                            NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()
                    ).withColor(NoellesRoleRegistry.DETECTIVE.color());
                    drawCentered(context.renderer(), context.drawContext(), targetInfo, 32, NoellesRoleRegistry.DETECTIVE.color());
                }
        );
    }

    private static void drawCentered(@NotNull TextRenderer renderer,
                                     @NotNull DrawContext context,
                                     @NotNull Text text,
                                     int y,
                                     int color) {
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2.0F, context.getScaledWindowHeight() / 2.0F + 6.0F, 0.0F);
        context.getMatrices().scale(0.6F, 0.6F, 1.0F);
        context.drawTextWithShadow(renderer, text, -renderer.getWidth(text) / 2, y, color);
        context.getMatrices().pop();
    }
}
