package org.agmas.noellesroles.client.appearance.roles.vulture;

import dev.doctor4t.wathe.api.client.gui.PlayerBodyHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.input.NoellesClientKeybindings;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.vulture.BodyConsumedComponent;

import java.util.List;

/** 秃鹫的尸体死因权限与吞噬提示。 */
public final class VultureBodyHudHandler {
    private VultureBodyHudHandler() {
    }

    public static void register() {
        PlayerBodyHudApi.registerLineRule(
                NoellesAppearanceSupport.id("body/vulture_death"),
                PlayerBodyHudApi.BodyHudLine.DEATH,
                PlayerBodyHudApi.DEFAULT_PRIORITY,
                (context, line) -> {
                    if (!isAliveVulture(context)) {
                        return PlayerBodyHudApi.BodyHudDecision.PASS;
                    }
                    return BodyConsumedComponent.KEY.get(context.body()).consumed
                            ? PlayerBodyHudApi.BodyHudDecision.OBFUSCATE
                            : PlayerBodyHudApi.BodyHudDecision.SHOW;
                }
        );
        PlayerBodyHudApi.registerAdditionalLines(
                NoellesAppearanceSupport.id("body/vulture_action"),
                PlayerBodyHudApi.DEFAULT_PRIORITY,
                context -> {
                    if (!isAliveVulture(context)) {
                        return List.of();
                    }
                    if (BodyConsumedComponent.KEY.get(context.body()).consumed) {
                        return List.of(Text.translatable("hud.vulture.already_consumed")
                                .withColor(NoellesRoleRegistry.VULTURE.color()));
                    }
                    if (AbilityPlayerComponent.KEY.get(context.viewer()).cooldown > 0) {
                        return List.of();
                    }
                    return List.of(Text.translatable("hud.vulture.eat",
                            NoellesClientKeybindings.getAbilityBind().getBoundKeyLocalizedText()).formatted(Formatting.RED));
                }
        );
    }

    private static boolean isAliveVulture(PlayerBodyHudApi.Context context) {
        return GameFunctions.isPlayerAliveAndSurvival(context.viewer())
                && GameWorldComponent.KEY.get(context.viewer().getWorld()).isRole(context.viewer(), NoellesRoleRegistry.VULTURE);
    }
}
