package org.agmas.noellesroles.client.appearance.roles.amnesiac;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.PlayerBodyHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import java.util.List;

/**
 * 失忆患者对准尸体时的能力提示。
 */
public final class AmnesiacBodyHudHandler {
    private AmnesiacBodyHudHandler() {
    }

    public static void register() {
        PlayerBodyHudApi.registerAdditionalLines(
                NoellesAppearanceSupport.id("body/amnesiac"),
                PlayerBodyHudApi.DEFAULT_PRIORITY,
                context -> {
                    var player = context.viewer();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (!gameWorld.isRole(player, NoellesRoleRegistry.AMNESIAC) || GameFunctions.isPlayerSpectatingOrCreative(player)) {
                        return List.of();
                    }
                    return List.of(Text.translatable("hud.noellesroles.amnesiac.select_body")
                            .withColor(NoellesRoleRegistry.AMNESIAC.color()));
                }
        );
    }
}
