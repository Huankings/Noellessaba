package org.agmas.noellesroles.client.appearance.roles.morphling;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.roles.morphling.MorphBodyDisguiseWorldComponent;
import org.agmas.noellesroles.roles.morphling.MorphMarkPlayerComponent;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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

        PlayerAppearanceApi.registerPlayerSkin(
                NoellesAppearanceSupport.id("morphling/reagent/appearance/player"),
                NoellesAppearancePriorities.MORPH_REAGENT_DISGUISE,
                MorphlingAppearanceHandler::resolveReagentSkin
        );

        PlayerAppearanceApi.registerBodySkin(
                NoellesAppearanceSupport.id("morphling/reagent/appearance/body_reveal"),
                NoellesAppearancePriorities.MORPH_REAGENT_DISGUISE,
                MorphlingAppearanceHandler::resolveReagentBodyRevealSkin
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

        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("morphling/reagent/role_name/name"),
                NoellesAppearancePriorities.MORPH_REAGENT_DISGUISE,
                MorphlingAppearanceHandler::resolveReagentName
        );
    }

    private static @Nullable SkinTextures resolveReagentSkin(@NotNull AbstractClientPlayerEntity player) {
        MorphlingPlayerComponent originalMorph = MorphlingPlayerComponent.KEY.get(player);
        if (originalMorph.getMorphTicks() > 0) {
            /*
             * NoellesRoles 原本的主动变形优先级更高。
             * 试剂作用在变形怪自己身上时只负责延展经济、语音和陷害效果，
             * 不覆盖正在进行的主动变形皮肤。
             */
            return null;
        }

        MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(player);
        UUID sampleUuid = component.sampleUuid();
        return component.isActive() && sampleUuid != null
                ? DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, sampleUuid, true)
                : null;
    }

    private static @Nullable Text resolveReagentName(
            ClientPlayerEntity viewer,
            PlayerEntity target,
            Text originalName
    ) {
        MorphlingPlayerComponent originalMorph = MorphlingPlayerComponent.KEY.get(target);
        if (originalMorph.getMorphTicks() > 0) {
            return null;
        }

        MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(target);
        UUID sampleUuid = component.sampleUuid();
        if (!component.isActive() || sampleUuid == null) {
            return null;
        }
        return NoellesAppearanceSupport.resolveNameFromUuid(target, sampleUuid, Text.literal(component.sampleName()));
    }

    private static @Nullable SkinTextures resolveReagentBodyRevealSkin(@NotNull PlayerBodyEntity body) {
        if (MorphBodyDisguiseWorldComponent.KEY.get(body.getWorld()).getDisguise(body.getPlayerUuid()).isEmpty()) {
            return null;
        }

        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || !WatheClient.isInstinctEnabled()) {
            return null;
        }

        boolean aliveKillerInstinct = WatheClient.isPlayerAliveAndInSurvival() && WatheClient.isKiller();
        boolean outOfGameInstinct = !WatheClient.isPlayerAliveAndInSurvival();
        if (!aliveKillerInstinct && !outOfGameInstinct) {
            return null;
        }

        /*
         * 试剂尸体默认显示成样本玩家，这是陷害效果本体。
         * 但杀手本能和非存活玩家本能属于“看穿外观”的信息视角，
         * 因此这里仅在客户端渲染阶段显回真实死者，不修改尸体 owner/appearanceUuid。
         */
        return PlayerAppearanceApi.resolveOriginalSkinTextures(body.getPlayerUuid(), true);
    }
}
