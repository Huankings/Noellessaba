package org.agmas.noellesroles.client.appearance.modifiers.lovers;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.modifiers.lovers.LoversConstants;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.List;
import java.util.UUID;

/**
 * 恋人 HUD 接入。
 */
public final class LoversHudHandler {
    private LoversHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesRolesCore.id("role_name/lovers/hud"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                LoversHudHandler::render
        );
    }

    private static void render(RoleNameHudApi.Context context) {
        renderOwnPartnerHud(context);
        renderCrosshairPartnerHud(context);
    }

    private static void renderOwnPartnerHud(RoleNameHudApi.Context context) {
        ClientPlayerEntity player = context.player();
        if (player.networkHandler == null || !WatheClient.isPlayerAliveAndInSurvival()) {
            // 恋人 HUD 依赖客户端本地玩家和玩家列表网络连接。
            // 切世界、重连、观战切换视角的瞬间，这两个对象可能短暂为空；
            // 如果这里继续往下取恋人资料，就会直接在客户端渲染线程里空指针崩溃。
            return;
        }

        WorldModifierComponent component = WorldModifierComponent.KEY.get(player.getWorld());
        if (!component.isModifier(player, NoellesModifierRegistry.LOVERS)) {
            return;
        }
        LoversPairComponent pairComponent = LoversPairComponent.KEY.get(player.getWorld());

        /*
         * 多对恋人时，不能再把“所有拥有 LOVERS 的玩家”都显示成伴侣。
         * 这里先从配对组件里取自己的唯一伴侣；
         * 如果是旧式单对数据且组件缺失，则 getPartnerOrFallback 会在只有两名 LOVERS 时自动兜底。
         */
        List<UUID> lovers = component.getAllWithModifier(NoellesModifierRegistry.LOVERS);
        UUID partnerUuid = pairComponent.getPartnerOrFallback(player.getUuid(), lovers);
        if (partnerUuid == null) {
            return;
        }

        int textY = context.drawContext().getScaledWindowHeight() - 12;
        int textX = 18;

        Text name;
        if (!LoversConstants.KNOW_IMMEDIATELY) {
            name = Text.translatable("hud.noellesroles.lovers.notification");
            textX -= 14;
        } else {
            PlayerListEntry partnerInfo = player.networkHandler.getPlayerListEntry(partnerUuid);
            if (partnerInfo == null || partnerInfo.getProfile() == null) {
                // 玩家资料不一定会和 HUD 渲染完全同步；跳过这一帧，下一帧资料到齐后再显示。
                return;
            }
            name = Text.translatable("tip.noellesroles.lovers.partner", partnerInfo.getProfile().getName());

            if (partnerInfo.getSkinTextures() != null) {
                PlayerSkinDrawer.draw(context.drawContext(), partnerInfo.getSkinTextures().texture(), 2, textY - 2, 12);
            }
        }

        var role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (NoellesRoleRegistry.EXECUTIONER.equals(role)) {
            textY -= 15;
        }

        context.drawContext().drawTextWithShadow(context.renderer(), name, textX, textY, LoversConstants.COLOR);
    }

    private static void renderCrosshairPartnerHud(RoleNameHudApi.Context context) {
        ClientPlayerEntity player = context.player();
        PlayerEntity target = context.targetPlayer();
        if (target == null) {
            return;
        }

        WorldModifierComponent component = WorldModifierComponent.KEY.get(player.getWorld());
        LoversPairComponent pairComponent = LoversPairComponent.KEY.get(player.getWorld());
        if (!component.isModifier(target, NoellesModifierRegistry.LOVERS)) {
            return;
        }

        if (WatheClient.isPlayerAliveAndInSurvival()
                && !LoversConstants.KNOW_IMMEDIATELY
                && component.isModifier(player, NoellesModifierRegistry.LOVERS)
                && pairComponent.arePartnersOrFallback(
                player.getUuid(),
                target.getUuid(),
                component.getAllWithModifier(NoellesModifierRegistry.LOVERS)
        )) {
            renderCenteredSmall(context, Text.translatable("hud.noellesroles.lovers.partner"));
        } else if (WatheClient.isPlayerSpectatingOrCreative()) {
            // 观战目标的另一位恋人可能还没同步到当前客户端，先跳过这一帧，不执行空值读名。
            List<UUID> lovers = component.getAllWithModifier(NoellesModifierRegistry.LOVERS);
            UUID partnerUuid = pairComponent.getPartnerOrFallback(target.getUuid(), lovers);
            if (partnerUuid == null) {
                return;
            }
            PlayerEntity loverPlayer = player.getWorld().getPlayerByUuid(partnerUuid);
            if (loverPlayer != null) {
                renderCenteredSmall(context, Text.translatable(
                        "hud.noellesroles.lovers.in_love",
                        loverPlayer.getName()
                ));
            }
        }
    }

    private static void renderCenteredSmall(RoleNameHudApi.Context context, Text text) {
        int alpha = (int) (context.nametagAlpha() * 255.0F) << 24;
        int color = LoversConstants.COLOR | alpha;

        var matrices = context.drawContext().getMatrices();
        matrices.push();
        matrices.translate(context.drawContext().getScaledWindowWidth() / 2.0F, context.drawContext().getScaledWindowHeight() / 2.0F - 35.0F, 0.0F);
        matrices.scale(0.6F, 0.6F, 1.0F);
        context.drawContext().drawTextWithShadow(
                context.renderer(),
                text,
                -context.renderer().getWidth(text) / 2,
                32,
                color
        );
        matrices.pop();
    }
}
