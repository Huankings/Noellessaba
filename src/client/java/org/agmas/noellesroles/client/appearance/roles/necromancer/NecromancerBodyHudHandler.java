package org.agmas.noellesroles.client.appearance.roles.necromancer;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.gui.PlayerBodyHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.necromancer.NecromancerWorldComponent;
import java.util.List;

/**
 * 死灵法师的尸体复活提示。
 *
 * <p>这块和验尸官一样，本质上都是“准心对着尸体时显示额外信息”，
 * 所以直接挂到 Wathe 的 extra HUD 上最稳。</p>
 */
public final class NecromancerBodyHudHandler {
    private NecromancerBodyHudHandler() {
    }

    public static void register() {
        PlayerBodyHudApi.registerAdditionalLines(
                NoellesAppearanceSupport.id("body/necromancer"),
                PlayerBodyHudApi.DEFAULT_PRIORITY,
                context -> {
                    var player = context.viewer();
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                    if (!gameWorld.isRole(player, NoellesRoleRegistry.NECROMANCER) || GameFunctions.isPlayerSpectatingOrCreative(player)) {
                        return List.of();
                    }

                    Text status = Text.translatable("hud.noellesroles.necromancer.possible_revive");
                    NecromancerWorldComponent necromancerWorld = NecromancerWorldComponent.KEY.get(player.getWorld());
                    if (necromancerWorld.getAvailableRevives() < 1) {
                        status = Text.translatable("hud.noellesroles.necromancer.no_possible_revive");
                    }

                    AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
                    if (ability.cooldown > 0) {
                        status = Text.translatable("hud.noellesroles.necromancer.cooldown", ability.cooldown / 20);
                    }

                    return List.of(status.copy().withColor(NoellesRoleRegistry.NECROMANCER.color()));
                }
        );
    }
}
