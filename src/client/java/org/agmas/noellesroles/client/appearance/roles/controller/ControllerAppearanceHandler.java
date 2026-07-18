package org.agmas.noellesroles.client.appearance.roles.controller;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;

import java.util.UUID;

/**
 * 附体师接入 Wathe 外观与准心名字 API 的规则。
 *
 * <p>附体师的伪装目标由 ControllerPlayerComponent 保存；只要目标 UUID 存在，
 * 客户端就把该玩家渲染成目标外观，并把准心名字同步解析为目标名字。</p>
 */
public final class ControllerAppearanceHandler {
    private ControllerAppearanceHandler() {
    }

    public static void register() {
        PlayerAppearanceApi.registerPlayerSkin(
                NoellesAppearanceSupport.id("controller/appearance/player"),
                NoellesAppearancePriorities.ACTIVE_DISGUISE,
                player -> {
                    UUID disguiseUuid = ControllerPlayerComponent.KEY.get(player).getDisguiseTarget();
                    return disguiseUuid == null
                            ? null
                            : DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, disguiseUuid, true);
                }
        );

        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("controller/role_name/name"),
                NoellesAppearancePriorities.ACTIVE_DISGUISE,
                (viewer, target, originalName) -> {
                    UUID disguiseUuid = ControllerPlayerComponent.KEY.get(target).getDisguiseTarget();
                    /*
                     * getDisguiseTarget 为空表示没有处于伪装状态，直接 PASS；
                     * 非空时准心名字按同一个 UUID 解析，和皮肤保持一致。
                     */
                    return disguiseUuid == null
                            ? null
                            : NoellesAppearanceSupport.resolveNameFromUuid(target, disguiseUuid, originalName);
                }
        );
    }
}
