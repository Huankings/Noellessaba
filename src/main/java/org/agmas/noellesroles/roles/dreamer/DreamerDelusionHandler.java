package org.agmas.noellesroles.roles.dreamer;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.api.event.DelusionEvents;

/**
 * 梦者监听 Noelles 幻觉试剂事件的入口。
 *
 * <p>kinssaba 原先通过跨模组监听 Noelles 的 {@link DelusionEvents} 给梦者计数。
 * 现在 Dreamer 已经迁进 NoellesRoles，这段兼容逻辑也一并回到本模组内维护。</p>
 */
public final class DreamerDelusionHandler {
    private static boolean initialized = false;

    private DreamerDelusionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DelusionEvents.STARTED.register((player, applier) -> {
            if (GameFunctions.isPlayerSpectatingOrCreative(player)) {
                return;
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.canUseKillerFeatures(player) || gameWorld.isRole(player, NoellesRoleRegistry.DREAMER)) {
                return;
            }

            for (ServerPlayerEntity possibleDreamer : player.getServer().getPlayerManager().getPlayerList()) {
                if (!GameFunctions.isPlayerAliveAndSurvival(possibleDreamer) || !gameWorld.isRole(possibleDreamer, NoellesRoleRegistry.DREAMER)) {
                    continue;
                }

                DreamerKillerComponent dreamerProgress = DreamerKillerComponent.KEY.get(possibleDreamer);
                possibleDreamer.sendMessage(Text.translatable("tip.noellesroles.dreamer.fake_poisoned").withColor(NoellesRoleRegistry.DREAMER.color()), true);
                dreamerProgress.addDreamerCount(possibleDreamer);
            }
        });
    }
}
