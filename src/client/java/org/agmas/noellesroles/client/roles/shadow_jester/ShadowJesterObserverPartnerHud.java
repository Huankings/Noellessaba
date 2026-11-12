package org.agmas.noellesroles.client.roles.shadow_jester;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterConstants;

import java.util.UUID;

/** 普通非存活观察者准心查看影子小丑时显示其另一半。 */
public final class ShadowJesterObserverPartnerHud {
    private ShadowJesterObserverPartnerHud() {
    }

    public static void register() {
        RoleNameHudApi.registerAdditionalLine(
                NoellesRolesCore.id("role_name/shadow_jester/observer_partner"),
                80,
                context -> {
                    if (!GameFunctions.isPlayerSpectatingOrCreative(context.player())
                            || !(context.targetPlayer() instanceof PlayerEntity shadowJester)
                            || context.player().networkHandler == null
                            || !GameWorldComponent.KEY.get(shadowJester.getWorld())
                            .isRole(shadowJester, NoellesRoleRegistry.SHADOW_JESTER)) {
                        return null;
                    }

                    UUID partnerUuid = ShadowJesterComponent.KEY.get(shadowJester.getWorld())
                            .getPartner(shadowJester.getUuid());
                    if (partnerUuid == null) {
                        return null;
                    }
                    PlayerListEntry partnerEntry = context.player().networkHandler.getPlayerListEntry(partnerUuid);
                    if (partnerEntry == null || partnerEntry.getProfile() == null) {
                        return null;
                    }

                    return RoleNameHudApi.AdditionalLine.below(
                            Text.translatable("hud.noellesroles.shadow_jester.observer_partner",
                                    partnerEntry.getProfile().getName()),
                            ShadowJesterConstants.ROLE_COLOR,
                            1.0F
                    );
                }
        );
    }
}
