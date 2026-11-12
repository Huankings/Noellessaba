package org.agmas.noellesroles.client.appearance.modifiers.lovers;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.modifiers.lovers.LoversConstants;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.List;
import java.util.UUID;

/**
 * 恋人准心提示接入。
 *
 * <p>左下角固定伴侣 HUD 已经迁到 {@code LoversPartnerHud} 的通用屏幕 HUD provider。
 * 这里继续留在 {@link RoleNameHudApi}，只处理“准心指向伴侣/旁观者查看恋人关系”的信息，
 * 因为这类提示本来就依赖 Wathe RoleNameRenderer 已经算好的准心目标和淡入透明度。</p>
 */
public final class LoversHudHandler {
    private LoversHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerAdditionalLine(
                NoellesRolesCore.id("role_name/lovers/hud"),
                70,
                LoversHudHandler::render
        );
    }

    private static RoleNameHudApi.AdditionalLine render(RoleNameHudApi.Context context) {
        ClientPlayerEntity player = context.player();
        PlayerEntity target = context.targetPlayer();
        if (target == null) {
            return null;
        }

        WorldModifierComponent component = WorldModifierComponent.KEY.get(player.getWorld());
        LoversPairComponent pairComponent = LoversPairComponent.KEY.get(player.getWorld());
        if (!component.isModifier(target, NoellesModifierRegistry.LOVERS)) {
            return null;
        }

        if (GameFunctions.isPlayerAliveAndSurvival(player)
                && !LoversConstants.KNOW_IMMEDIATELY
                && component.isModifier(player, NoellesModifierRegistry.LOVERS)
                && pairComponent.arePartnersOrFallback(
                player.getUuid(),
                target.getUuid(),
                component.getAllWithModifier(NoellesModifierRegistry.LOVERS)
        )) {
            return RoleNameHudApi.AdditionalLine.below(
                    Text.translatable("hud.noellesroles.lovers.partner"),
                    LoversConstants.COLOR,
                    1.0F
            );
        } else if (GameFunctions.isPlayerSpectatingOrCreative(player)) {
            // 观战目标的另一位恋人可能还没同步到当前客户端，先跳过这一帧，不执行空值读名。
            List<UUID> lovers = component.getAllWithModifier(NoellesModifierRegistry.LOVERS);
            UUID partnerUuid = pairComponent.getPartnerOrFallback(target.getUuid(), lovers);
            if (partnerUuid == null) {
                return null;
            }
            PlayerEntity loverPlayer = player.getWorld().getPlayerByUuid(partnerUuid);
            if (loverPlayer != null) {
                return RoleNameHudApi.AdditionalLine.below(
                        Text.translatable("hud.noellesroles.lovers.in_love", loverPlayer.getName()),
                        LoversConstants.COLOR,
                        0.9F
                );
            }
        }
        return null;
    }
}
