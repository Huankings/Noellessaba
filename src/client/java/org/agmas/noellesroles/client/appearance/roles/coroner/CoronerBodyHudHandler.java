package org.agmas.noellesroles.client.appearance.roles.coroner;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
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
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.appearance.modifiers.graverobber.GraverobberBodyInfoAccess;
import org.agmas.noellesroles.roles.coroner.BodyDeathReasonComponent;
import org.agmas.noellesroles.roles.coroner.CoronerConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 验尸官 / 盗墓者 / 观察者的尸体读条 HUD。
 *
 * <p>这块原来挂在 {@code CoronerHudMixin} 里，会和验尸官检查提示抢同一套矩阵状态。
 * 现在改成 Wathe 的 {@link RoleNameHudApi#registerExtraHud}，HUD 渲染顺序和坐标都更稳定，
 * 也不会再把后面的文字顶到奇怪的位置。</p>
 */
public final class CoronerBodyHudHandler {
    private CoronerBodyHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesAppearanceSupport.id("role_name/coroner_body"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    ClientPlayerEntity player = context.player();
                    if (!GraverobberBodyInfoAccess.canSeeBodyReadout(player)) {
                        return;
                    }

                    PlayerBodyEntity body = RoleNameHudApi.findLookedAtBody(player, CoronerConstants.BODY_HUD_RANGE);
                    if (body == null) {
                        return;
                    }

                    if (GraverobberBodyInfoAccess.shouldBlockCoronerReadoutForSanity(player)) {
                        drawCentered(
                                context.renderer(),
                                context.drawContext(),
                                Text.translatable("hud.coroner.sanity_requirements"),
                                CoronerConstants.SANITY_REQUIREMENTS_Y,
                                Colors.YELLOW
                        );
                        return;
                    }

                    renderBodyReadout(context.renderer(), context.drawContext(), player, body);
                }
        );
    }

    private static void renderBodyReadout(@NotNull TextRenderer renderer,
                                          @NotNull DrawContext context,
                                          @NotNull ClientPlayerEntity player,
                                          @NotNull PlayerBodyEntity body) {
        BodyDeathReasonComponent bodyDeathReason = BodyDeathReasonComponent.KEY.get(body);
        boolean shouldObfuscate = bodyDeathReason.vultured
                && GraverobberBodyInfoAccess.shouldObfuscateVulturedBodyInfo(player);

        Text deathInfo = shouldObfuscate
                ? Text.literal("a".repeat(player.getRandom().nextBetween(12, 26))).formatted(Formatting.OBFUSCATED)
                : Text.translatable("hud.coroner.death_info", body.age / 20)
                .append(Text.translatable("death_reason." + bodyDeathReason.deathReason.getNamespace() + "." + bodyDeathReason.deathReason.getPath()));
        drawCentered(renderer, context, deathInfo, CoronerConstants.BODY_INFO_Y, Colors.RED);

        /*
         * 角色信息只有两种情况会被挡掉：
         * 1. 秃鹫已经吃过这具尸体，而当前查看者仍然是局内存活玩家；
         * 2. 当前查看者不在任何可读尸体信息的范围里，前面的入口已经拦掉了。
         *
         * 非存活玩家需要继续读到完整身份，所以这里不要把“尸体被啃过”直接当成硬开关。
         */
        boolean canShowRoleInfo = GameFunctions.isPlayerSpectatingOrCreative(player)
                || GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.CORONER)
                || GraverobberBodyInfoAccess.isGraverobber(player);
        if (canShowRoleInfo && (!bodyDeathReason.vultured || !GameFunctions.isPlayerAliveAndSurvival(player))) {
            Role foundRole = WatheRoles.CIVILIAN;
            for (Role role : WatheRoles.ROLES) {
                if (role.identifier().equals(bodyDeathReason.playerRole)) {
                    foundRole = role;
                    break;
                }
            }

            Text roleInfo = Text.translatable("hud.coroner.role_info").withColor(Colors.RED)
                    .append(Harpymodloader.getRoleName(foundRole).withColor(foundRole.color()));
            drawCentered(renderer, context, roleInfo, CoronerConstants.BODY_ROLE_INFO_Y, Colors.WHITE);
        }

        if (GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.VULTURE)) {
            if (bodyDeathReason.vultured) {
                Text roleInfo = Text.translatable("hud.vulture.already_consumed").withColor(Noellesroles.VULTURE.color());
                drawCentered(renderer, context, roleInfo, CoronerConstants.BODY_ROLE_INFO_Y, Colors.WHITE);
            } else if (AbilityPlayerComponent.KEY.get(player).cooldown <= 0 && GameFunctions.isPlayerAliveAndSurvival(player)) {
                Text roleInfo = Text.translatable("hud.vulture.eat", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText())
                        .withColor(Colors.RED);
                drawCentered(renderer, context, roleInfo, CoronerConstants.BODY_ROLE_INFO_Y, Colors.WHITE);
            }
        }
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
