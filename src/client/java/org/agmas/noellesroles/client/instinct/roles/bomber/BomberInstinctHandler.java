package org.agmas.noellesroles.client.instinct.roles.bomber;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;

public final class BomberInstinctHandler {
    private BomberInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("bomber_bomb_color"), NoellesInstinctHandlers.PRIORITY_HIGH_INSTINCT_COLOR, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                    && gameWorld.isRole(viewer, Noellesroles.BOMBER)
                    && WatheClient.isInstinctEnabled()
                    && BomberPlayerComponent.KEY.get(targetPlayer).hasBomb()) {
                /*
                 * 炸弹客塞过炸弹的玩家必须压过 Wathe 默认杀手本能颜色。
                 * viewer 必须仍存活，否则死亡观察者会因为 isInstinctEnabled() 被观察者本能开启而误看到炸弹客颜色。
                 * 因此这里使用高于 0 的 priority，但仍依赖 isInstinctEnabled()，保证本能压制能统一生效。
                 */
                return InstinctApi.HighlightResult.color(Noellesroles.BOMBER.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
