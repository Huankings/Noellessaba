package org.agmas.noellesroles.client.appearance.modifiers.dual_personality;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityConstants;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.UUID;

/** 普通非存活观察者准心查看双重人格玩家时显示其另一人格。 */
public final class DualPersonalityObserverPartnerHud {
    private DualPersonalityObserverPartnerHud() {
    }

    public static void register() {
        RoleNameHudApi.registerAdditionalLine(
                NoellesRolesCore.id("role_name/dual_personality/observer_partner"),
                60,
                context -> {
                    if (!GameFunctions.isPlayerSpectatingOrCreative(context.player())
                            || !(context.targetPlayer() instanceof PlayerEntity target)
                            || context.player().networkHandler == null) {
                        return null;
                    }

                    WorldModifierComponent modifiers = WorldModifierComponent.KEY.get(target.getWorld());
                    if (!modifiers.isModifier(target, NoellesModifierRegistry.DUAL_PERSONALITY)) {
                        return null;
                    }
                    UUID partnerUuid = DualPersonalityComponent.KEY.get(target.getWorld())
                            .getPartner(target.getUuid());
                    if (partnerUuid == null) {
                        return null;
                    }
                    PlayerListEntry partnerEntry = context.player().networkHandler.getPlayerListEntry(partnerUuid);
                    if (partnerEntry == null || partnerEntry.getProfile() == null) {
                        return null;
                    }

                    return RoleNameHudApi.AdditionalLine.below(
                            Text.translatable("hud.noellesroles.dual_personality.observer_partner",
                                    partnerEntry.getProfile().getName()),
                            DualPersonalityConstants.COLOR,
                            0.9F
                    );
                }
        );
    }
}
