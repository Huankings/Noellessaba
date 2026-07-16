package org.agmas.noellesroles.client.instinct.roles.bartender;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.agmas.noellesroles.roles.bartender.BartenderPlayerComponent;

import java.awt.Color;

public final class BartenderInstinctHandler {
    private BartenderInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("bartender_status"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (!gameWorld.isRole(viewer, Noellesroles.BARTENDER)) {
                return InstinctApi.HighlightResult.pass();
            }

            BartenderPlayerComponent bartender = BartenderPlayerComponent.KEY.get(targetPlayer);
            PlayerPoisonComponent poison = PlayerPoisonComponent.KEY.get(targetPlayer);
            DelusionPlayerComponent delusion = DelusionPlayerComponent.KEY.get(targetPlayer);

            /*
             * Bartender 的绿色/蓝色/红色都是饮品或防御状态反馈。
             * 它们属于职业能力信息，不依赖本能键开启，所以这里不走 WatheClient.isInstinctEnabled()。
             */
            InstinctApi.HighlightResult result = InstinctApi.HighlightResult.pass();
            if (bartender.glowTicks > 0) {
                result = InstinctApi.HighlightResult.color(Color.GREEN.getRGB());
            }
            if (bartender.armor > 0) {
                return InstinctApi.HighlightResult.color(Color.BLUE.getRGB());
            }
            if (poison.poisonTicks > 0 || delusion.isActive()) {
                result = InstinctApi.HighlightResult.color(Color.RED.getRGB());
            }
            return result;
        });
    }
}
