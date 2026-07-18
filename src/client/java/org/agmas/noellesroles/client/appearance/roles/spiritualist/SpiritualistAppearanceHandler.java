package org.agmas.noellesroles.client.appearance.roles.spiritualist;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;

/**
 * 灵术师接入 Wathe 外观与准心名字 API 的规则。
 *
 * <p>灵术师的核心不是“改变全服真实外观”，而是本地玩家在出窍/附体状态下如何理解画面。
 * 因此这些 handler 都使用最高优先级，只影响当前客户端看到的玩家、尸体、准心名字与射线来源。</p>
 */
public final class SpiritualistAppearanceHandler {
    private SpiritualistAppearanceHandler() {
    }

    public static void register() {
        registerPlayerSkin();
        registerBodySkin();
        registerRaycastSource();
        registerPossessionTargetFilter();
        registerProjectedName();
    }

    private static void registerPlayerSkin() {
        PlayerAppearanceApi.registerPlayerSkin(
                NoellesAppearanceSupport.id("spiritualist/appearance/player"),
                NoellesAppearancePriorities.SPIRITUALIST,
                player -> {
                    ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
                    if (localPlayer == null) {
                        return null;
                    }

                    SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(localPlayer);
                    if (component.isProjecting() && player != localPlayer) {
                        /*
                         * 出窍时，灵术师客户端会把其它活玩家都看成自己。
                         * 这只是本地视觉遮罩，不写入玩家真实皮肤，也不影响其它客户端。
                         */
                        return NoellesAppearanceSupport.resolveLocalOriginalSkin(localPlayer);
                    }
                    if (component.isPossessing() && player == localPlayer && component.possessionTarget != null) {
                        /*
                         * 附体视角下，本地玩家模型显示为附体目标。
                         * resolveSkinTexturesFromUuid 内部按 UUID 取原皮，避免读取已经被其它伪装覆盖的实体皮肤。
                         */
                        return DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, component.possessionTarget, true);
                    }
                    return null;
                }
        );
    }

    private static void registerBodySkin() {
        PlayerAppearanceApi.registerBodySkin(
                NoellesAppearanceSupport.id("spiritualist/appearance/body"),
                NoellesAppearancePriorities.SPIRITUALIST,
                body -> {
                    ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
                    if (localPlayer == null || !SpiritualistPlayerComponent.KEY.get(localPlayer).isProjecting()) {
                        return null;
                    }

                    /*
                     * 出窍时尸体也显示成灵术师本人，保证“所有可见玩家/尸体都是自己”的视觉规则完整。
                     * 这里只覆盖客户端渲染，不会改 Wathe 尸体的 owner UUID。
                     */
                    return NoellesAppearanceSupport.resolveLocalOriginalSkin(localPlayer);
                }
        );
    }

    private static void registerRaycastSource() {
        RoleNameHudApi.registerRaycastSource(
                NoellesAppearanceSupport.id("spiritualist/role_name/raycast_source"),
                NoellesAppearancePriorities.SPIRITUALIST,
                player -> {
                    if (SpiritualistClientController.isProjectionActive() || SpiritualistClientController.isPossessionViewActive()) {
                        /*
                         * 出窍/附体时玩家真正看的不是本体眼睛位置，而是当前相机实体。
                         * RoleNameHudApi 会用这个实体重新做准心射线，HUD 目标才能和画面中心一致。
                         */
                        Entity cameraEntity = MinecraftClient.getInstance().getCameraEntity();
                        return cameraEntity == null ? null : cameraEntity;
                    }
                    return null;
                }
        );
    }

    private static void registerPossessionTargetFilter() {
        RoleNameHudApi.registerPlayerTargetFilter(
                NoellesAppearanceSupport.id("spiritualist/role_name/possession_target_filter"),
                NoellesAppearancePriorities.SPIRITUALIST,
                (viewer, target) ->
                        SpiritualistClientController.shouldHideEntityInPossessionView(target)
                                ? RoleNameHudApi.TargetResult.DENY
                                : RoleNameHudApi.TargetResult.PASS
        );
    }

    private static void registerProjectedName() {
        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("spiritualist/role_name/name"),
                NoellesAppearancePriorities.SPIRITUALIST,
                (viewer, target, originalName) -> {
                    /*
                     * 出窍时皮肤已经把其它玩家显示成自己，准心名字也同步显示本地玩家名。
                     * 如果名字不跟着改，玩家会立刻通过 HUD 识别出真实目标。
                     */
                    return SpiritualistClientController.isProjectionActive() ? viewer.getDisplayName() : null;
                }
        );
    }
}
