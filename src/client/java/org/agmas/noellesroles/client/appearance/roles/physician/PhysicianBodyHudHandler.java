package org.agmas.noellesroles.client.appearance.roles.physician;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.coroner.BodyDeathReasonComponent;
import org.agmas.noellesroles.roles.physician.PhysicianConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 医师准心对准尸体时显示死亡时间与死因。
 */
public final class PhysicianBodyHudHandler {
    private PhysicianBodyHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesAppearanceSupport.id("role_name/physician_body"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    ClientPlayerEntity player = context.player();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (!gameWorld.isRole(player, Noellesroles.PHYSICIAN) || !GameFunctions.isPlayerAliveAndSurvival(player)) {
                        return;
                    }

                    PlayerBodyEntity body = RoleNameHudApi.findLookedAtBody(player, PhysicianConstants.BODY_HUD_RANGE);
                    if (body == null) {
                        return;
                    }

                    drawCentered(context.renderer(), context.drawContext(), getDeathInfo(player, body), 32, Colors.RED);
                }
        );
    }

    private static Text getDeathInfo(@NotNull ClientPlayerEntity player, @NotNull PlayerBodyEntity body) {
        BodyDeathReasonComponent bodyDeathReason = BodyDeathReasonComponent.KEY.get(body);
        if (bodyDeathReason.vultured) {
            int randomLength = player.getRandom().nextBetween(12, 26);
            return Text.literal("a".repeat(randomLength)).formatted(Formatting.OBFUSCATED);
        }

        return Text.translatable("hud.physician.death_info", body.age / 20)
                .append(Text.translatable("death_reason." + bodyDeathReason.deathReason.getNamespace() + "." + bodyDeathReason.deathReason.getPath()));
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
