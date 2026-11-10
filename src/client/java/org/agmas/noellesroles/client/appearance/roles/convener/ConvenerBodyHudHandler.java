package org.agmas.noellesroles.client.appearance.roles.convener;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.PlayerBodyHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import java.util.List;

/**
 * 召集者对准尸体时的召集提示。
 */
public final class ConvenerBodyHudHandler {
    private ConvenerBodyHudHandler() {
    }

    public static void register() {
        PlayerBodyHudApi.registerAdditionalLines(
                NoellesAppearanceSupport.id("body/convener"),
                PlayerBodyHudApi.DEFAULT_PRIORITY,
                context -> {
                    var player = context.viewer();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (!gameWorld.isRole(player, NoellesRoleRegistry.CONVENER) || GameFunctions.isPlayerSpectatingOrCreative(player)) {
                        return List.of();
                    }

                    AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
                    Text text = ability.cooldown > 0
                            ? Text.translatable("hud.noellesroles.convener.cooldown", Math.max(0, (ability.cooldown + 19) / 20))
                            : Text.translatable("hud.noellesroles.convener.select_body");
                    return List.of(text.copy().withColor(NoellesRoleRegistry.CONVENER.color()));
                }
        );
    }
}
