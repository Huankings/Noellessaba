package org.agmas.noellesroles.client.appearance.roles.morphling;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;

/**
 * 变形怪接入 Wathe 外观与准心名字 API 的规则。
 *
 * <p>变形怪的 disguise UUID 是主动技能写入的目标；只要 morphTicks 仍大于 0，
 * 皮肤和准心名字都要显示为目标玩家，时间结束后自然 PASS 给后续规则。</p>
 */
public final class MorphlingAppearanceHandler {
    private MorphlingAppearanceHandler() {
    }

    public static void register() {
        PlayerAppearanceApi.registerPlayerSkin(
                NoellesAppearanceSupport.id("morphling/appearance/player"),
                NoellesAppearancePriorities.ACTIVE_DISGUISE,
                player -> {
                    MorphlingPlayerComponent component = MorphlingPlayerComponent.KEY.get(player);
                    return component.getMorphTicks() > 0
                            ? DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, component.disguise, true)
                            : null;
                }
        );

        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("morphling/role_name/name"),
                NoellesAppearancePriorities.ACTIVE_DISGUISE,
                (viewer, target, originalName) -> {
                    MorphlingPlayerComponent component = MorphlingPlayerComponent.KEY.get(target);
                    /*
                     * 名字解析跟随同一个 disguise UUID，避免出现皮肤像 A、准心名字仍是 B 的穿帮。
                     */
                    return component.getMorphTicks() > 0
                            ? NoellesAppearanceSupport.resolveNameFromUuid(target, component.disguise, originalName)
                            : null;
                }
        );
    }
}
