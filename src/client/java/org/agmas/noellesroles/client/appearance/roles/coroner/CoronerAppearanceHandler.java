package org.agmas.noellesroles.client.appearance.roles.coroner;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;

/**
 * 验尸官接入 Wathe 外观与准心名字 API 的规则。
 *
 * <p>验尸官这里复用主动变形规则：morphTicks 大于 0 时读取 disguise UUID，
 * 同步覆盖玩家皮肤和准心名字；倒计时结束后不再干预 Wathe 原始显示。</p>
 */
public final class CoronerAppearanceHandler {
    private CoronerAppearanceHandler() {
    }

    public static void register() {
        PlayerAppearanceApi.registerPlayerSkin(
                NoellesAppearanceSupport.id("coroner/appearance/player"),
                NoellesAppearancePriorities.ACTIVE_DISGUISE,
                player -> {
                    CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(player);
                    return component.getMorphTicks() > 0
                            ? DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, component.disguise, true)
                            : null;
                }
        );

        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("coroner/role_name/name"),
                NoellesAppearancePriorities.ACTIVE_DISGUISE,
                (viewer, target, originalName) -> {
                    CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(target);
                    /*
                     * 皮肤和名字共用 disguise UUID，确保验尸官变形期间不会通过 HUD 露出真名。
                     */
                    return component.getMorphTicks() > 0
                            ? NoellesAppearanceSupport.resolveNameFromUuid(target, component.disguise, originalName)
                            : null;
                }
        );
    }
}
