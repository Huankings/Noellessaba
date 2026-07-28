package org.agmas.noellesroles.client.appearance.roles.timekeeper;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 时间狭缝中的本地视觉隔离。
 *
 * <p>这个机制只改变“处于狭缝的本地玩家如何看别人”，不写服务端实体数据。
 * 玩家被拉进特殊存活旁观时，本能、语音和聊天已经被服务端/客户端其它入口隔离；
 * 这里负责把其它存活玩家的皮肤、手臂模型、披风、玩家尸体和准心名牌都伪装成本地玩家自己。</p>
 */
public final class TimekeeperRiftAppearanceHandler {
    private TimekeeperRiftAppearanceHandler() {
    }

    public static void register() {
        PlayerAppearanceApi.registerPlayerSkin(
                NoellesAppearanceSupport.id("timekeeper/rift/appearance/player"),
                NoellesAppearancePriorities.TIMEKEEPER_RIFT,
                TimekeeperRiftAppearanceHandler::resolveRiftSkin
        );

        PlayerAppearanceApi.registerBodySkin(
                NoellesAppearanceSupport.id("timekeeper/rift/appearance/body"),
                NoellesAppearancePriorities.TIMEKEEPER_RIFT,
                TimekeeperRiftAppearanceHandler::resolveRiftBodySkin
        );

        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("timekeeper/rift/role_name/name"),
                NoellesAppearancePriorities.TIMEKEEPER_RIFT,
                TimekeeperRiftAppearanceHandler::resolveRiftName
        );

        RoleNameHudApi.registerCohortHint(
                NoellesAppearanceSupport.id("timekeeper/rift/role_name/hide_cohort_hint"),
                NoellesAppearancePriorities.TIMEKEEPER_RIFT,
                (viewer, target, vanillaValue) -> isRiftViewerLookingAtOtherAlivePlayer(viewer, target)
                        ? RoleNameHudApi.VisibilityResult.HIDE
                        : RoleNameHudApi.VisibilityResult.PASS
        );
    }

    private static @Nullable SkinTextures resolveRiftSkin(@NotNull AbstractClientPlayerEntity target) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || !isRiftViewerLookingAtOtherAlivePlayer(viewer, target)) {
            return null;
        }
        return NoellesAppearanceSupport.resolveLocalOriginalSkin(viewer);
    }

    private static @Nullable SkinTextures resolveRiftBodySkin(@NotNull PlayerBodyEntity body) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || !TimekeeperPlayerComponent.KEY.get(viewer).isInTimeRift()) {
            return null;
        }

        /*
         * 尸体外观同样属于“时间狭缝里不能读取外界信息”的一部分。
         * 这里只在客户端渲染阶段把所有 PlayerBodyEntity 显示成本地玩家原皮肤，
         * 不改 body 的真实 owner / appearanceUuid。这样验尸、尸袋、回放和服务端逻辑仍能读到真正死者，
         * 但处于狭缝的玩家不会通过地上的尸体皮肤、披风或 slim/default 模型推断身份。
         */
        return NoellesAppearanceSupport.resolveLocalOriginalSkin(viewer);
    }

    private static @Nullable Text resolveRiftName(
            ClientPlayerEntity viewer,
            PlayerEntity target,
            Text originalName
    ) {
        if (!isRiftViewerLookingAtOtherAlivePlayer(viewer, target)) {
            return null;
        }
        return viewer.getDisplayName();
    }

    private static boolean isRiftViewerLookingAtOtherAlivePlayer(
            PlayerEntity viewer,
            PlayerEntity target
    ) {
        return TimekeeperPlayerComponent.KEY.get(viewer).isInTimeRift()
                && !viewer.getUuid().equals(target.getUuid())
                && GameFunctions.isPlayerAliveAndSurvival(target);
    }
}
