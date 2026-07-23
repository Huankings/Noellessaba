package org.agmas.noellesroles.client.appearance.roles.convener;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.roles.convener.ConvenerDisguiseResolver;
import org.agmas.noellesroles.roles.convener.ConvenerCommunicationHelper;
import org.agmas.noellesroles.roles.convener.ConvenerDisguiseComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 召集者伪装外观接入。
 */
public final class ConvenerAppearanceHandler {
    private ConvenerAppearanceHandler() {
    }

    public static void register() {
        PlayerAppearanceApi.registerPlayerSkin(
                NoellesAppearanceSupport.id("appearance/player/convener"),
                NoellesAppearancePriorities.CONVENER,
                ConvenerAppearanceHandler::resolveSkin
        );

        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("role_name/convener/name"),
                NoellesAppearancePriorities.CONVENER,
                (viewer, target, originalName) -> resolveName(target, originalName)
        );

        RoleNameHudApi.registerCohortHint(
                NoellesAppearanceSupport.id("role_name/convener/hide_cohort_hint"),
                NoellesAppearancePriorities.CONVENER,
                (viewer, target, vanillaValue) -> ConvenerCommunicationHelper.isTemporarilySummonedLivingPlayer(target)
                        ? RoleNameHudApi.VisibilityResult.HIDE
                        : RoleNameHudApi.VisibilityResult.PASS
        );
    }

    private static @Nullable SkinTextures resolveSkin(AbstractClientPlayerEntity player) {
        ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(player);
        if (!disguise.isDisguised()) {
            return null;
        }

        UUID disguiseUuid = disguise.getDisguiseUuid();
        if (disguiseUuid == null) {
            return null;
        }
        return ConvenerDisguiseResolver.resolveSkinForUuid(disguiseUuid);
    }

    private static @Nullable Text resolveName(PlayerEntity target, Text originalName) {
        ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(target);
        if (!disguise.isDisguised()) {
            return null;
        }

        UUID disguiseUuid = disguise.getDisguiseUuid();
        if (disguiseUuid == null || disguiseUuid.equals(target.getUuid())) {
            return originalName;
        }

        /*
         * 准心名字必须和皮肤使用同一个 disguiseUuid，否则会出现“脸是死者、名字是真人”的穿帮。
         */
        Text disguiseName = ConvenerDisguiseResolver.resolveDisguiseName(target, disguiseUuid);
        return disguiseName != null ? disguiseName : originalName;
    }
}
