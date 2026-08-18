package org.agmas.noellesroles.client.appearance.roles.shadow_jester;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.client.WatheClient;
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
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterPhase;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 影子小丑第三阶段后的互相变形外观。
 */
public final class ShadowJesterAppearanceHandler {
    private ShadowJesterAppearanceHandler() {
    }

    public static void register() {
        PlayerAppearanceApi.registerPlayerSkin(
                NoellesAppearanceSupport.id("shadow_jester/appearance/player"),
                NoellesAppearancePriorities.ACTIVE_DISGUISE,
                ShadowJesterAppearanceHandler::resolvePartnerSkin
        );

        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("shadow_jester/role_name/name"),
                NoellesAppearancePriorities.ACTIVE_DISGUISE,
                ShadowJesterAppearanceHandler::resolvePartnerName
        );

        PlayerAppearanceApi.registerBodySkin(
                NoellesAppearanceSupport.id("shadow_jester/appearance/body_instinct_reveal"),
                NoellesAppearancePriorities.ACTIVE_DISGUISE + 1,
                ShadowJesterAppearanceHandler::resolveBodyInstinctReveal
        );
    }

    private static @Nullable SkinTextures resolvePartnerSkin(@NotNull AbstractClientPlayerEntity player) {
        if (!isAliveShadowJester(player)) {
            /*
             * 影子小丑只要不再处于“真实存活”状态，就立刻解除互换外观。
             * 这样死亡、旁观、创造或时间狭缝中的玩家都会回到自己的本体皮肤；
             * 如果调试里把玩家重新拉回存活状态，这里又会按誓言规则继续变成另一半。
             */
            return null;
        }

        UUID partner = resolvePartnerAfterVow(player);
        if (partner == null) {
            return null;
        }

        /*
         * 互相变形必须按 UUID 解析“原始皮肤”，不能读取实体当前皮肤。
         * 否则 A 显示成 B、B 显示成 A 时会互相套娃，出现双方都显示错误外观的客户端循环。
         */
        return PlayerAppearanceApi.resolveOriginalSkinTextures(partner, true);
    }

    private static @Nullable Text resolvePartnerName(
            ClientPlayerEntity viewer,
            PlayerEntity target,
            Text originalName
    ) {
        if (!isAliveShadowJester(target)) {
            return null;
        }

        UUID partner = resolvePartnerAfterVow(target);
        return partner == null ? null : NoellesAppearanceSupport.resolveNameFromUuid(target, partner, originalName);
    }

    private static @Nullable SkinTextures resolveBodyInstinctReveal(@NotNull PlayerBodyEntity body) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null
                || !GameFunctions.isPlayerSpectatingOrCreative(viewer)
                || !WatheClient.isInstinctEnabled()) {
            return null;
        }

        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(body.getWorld());
        if (!component.contains(body.getPlayerUuid())
                || !component.getPhase(body.getPlayerUuid()).atLeast(ShadowJesterPhase.VOW_BOUND)) {
            return null;
        }

        /*
         * 服务端尸体 appearanceUuid 会按誓言显示成另一半。
         * 旁观者开启本能时需要“看穿尸体皮肤”回到死者本人，但仍然只改本地渲染，
         * 不改尸体真实 owner、验尸信息或回放数据。
         */
        return PlayerAppearanceApi.resolveOriginalSkinTextures(body.getPlayerUuid(), true);
    }

    private static @Nullable UUID resolvePartnerAfterVow(PlayerEntity player) {
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(player.getWorld());
        UUID partner = component.getPartner(player.getUuid());
        if (partner == null || !component.getPhase(player.getUuid()).atLeast(ShadowJesterPhase.VOW_BOUND)) {
            return null;
        }
        return partner;
    }

    private static boolean isAliveShadowJester(@NotNull PlayerEntity player) {
        return GameFunctions.isPlayerAliveAndSurvival(player)
                && !TimekeeperPlayerComponent.KEY.get(player).isInTimeRift();
    }
}
