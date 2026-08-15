package org.agmas.noellesroles.client.roles.jason;

import net.minecraft.text.Text;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.jason.JasonAbilityPlayerComponent;
import org.agmas.noellesroles.roles.jason.JasonConstants;
import org.agmas.noellesroles.roles.jason.JasonPsychoHandler;

/**
 * 杰森“无恶不在”的右下角能力 HUD。
 */
public final class JasonAbilityStatusHud {
    private JasonAbilityStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/jason/ability_status", NoellesRoleRegistry.JASON, context -> {
            if (NoellesrolesClient.abilityBind == null || JasonPsychoHandler.isJasonModeActive(context.player())) {
                return;
            }

            JasonAbilityPlayerComponent component = JasonAbilityPlayerComponent.KEY.get(context.player());
            Text line = switch (component.getPhase()) {
                case IDLE -> component.getCooldownTicks() > 0
                        ? Text.translatable("hud.noellesroles.jason.ability.cooldown", seconds(component.getCooldownTicks()))
                        : Text.translatable("hud.noellesroles.jason.ability.ready", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
                case ENTERING, ACTIVE -> component.canRequestExit()
                        ? Text.translatable("hud.noellesroles.jason.ability.exit_ready", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText())
                        : Text.translatable("hud.noellesroles.jason.ability.min_exit", seconds(component.getRemainingMinExitTicks()));
                case EXITING -> Text.translatable(
                        "hud.noellesroles.jason.ability.exiting",
                        seconds(Math.max(0, JasonConstants.ABILITY_EXIT_TICKS - component.getPhaseTicks()))
                );
            };

            NoellesHudSupport.drawBottomRightLine(context, line, JasonConstants.ROLE_COLOR);
        });
    }

    private static int seconds(int ticks) {
        return Math.max(0, (ticks + 19) / 20);
    }
}
