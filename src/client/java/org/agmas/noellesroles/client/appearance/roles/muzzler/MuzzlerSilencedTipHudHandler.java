package org.agmas.noellesroles.client.appearance.roles.muzzler;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.muzzler.MuzzlerConstants;
import org.agmas.noellesroles.roles.muzzler.SilencePlayerComponent;

/**
 * 静语者的“目标被封嘴”准心提示。
 */
public final class MuzzlerSilencedTipHudHandler {
    private MuzzlerSilencedTipHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerAdditionalLine(
                NoellesAppearanceSupport.id("role_name/muzzler_silenced_tip"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                context -> {
                    PlayerEntity target = context.targetPlayer();
                    if (target == null) {
                        return null;
                    }

                    SilencePlayerComponent victimSilence = SilencePlayerComponent.KEY.get(target);
                    if (!victimSilence.isSilenced()
                            || victimSilence.getSilencedTicks() < MuzzlerConstants.DISPLAY_SILENCED_TIP_DELAY_TICKS) {
                        return null;
                    }

                    return RoleNameHudApi.AdditionalLine.aboveRoleDetails(
                            Text.translatable("tip.noellesroles.muzzler.silenced"),
                            NoellesRoleRegistry.MUZZLER.color(),
                            1.0F
                    );
                }
        );
    }
}
