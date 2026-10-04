package org.agmas.noellesroles.client.instinct.roles.perceiver;

import dev.doctor4t.wathe.api.blackout.BlackoutApi;
import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.perceiver.PerceiverConstants;
import org.agmas.noellesroles.roles.perceiver.PerceiverPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;

/** 感知者双向被动描边：选中目标看感知者，感知者看满足凝视条件的观察者。 */
public final class PerceiverInstinctHandler {
    private PerceiverInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("perceiver_bidirectional_perception"),
                PerceiverConstants.INSTINCT_HIGHLIGHT_PRIORITY,
                (viewer, target) -> {
                    if (!(target instanceof PlayerEntity targetPlayer)
                            || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                            || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                            || TimekeeperPlayerComponent.KEY.get(viewer).isInTimeRift()
                            || TimekeeperPlayerComponent.KEY.get(targetPlayer).isInTimeRift()) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());

                    /* 被选中的玩家在 30 秒内始终能被动透视感知者，停电不会关闭这一方向。 */
                    if (gameWorld.isRole(targetPlayer, NoellesRoleRegistry.PERCEIVER)
                            && PerceiverPlayerComponent.KEY.get(targetPlayer).isSelectedViewer(viewer.getUuid())) {
                        return InstinctApi.HighlightResult.color(PerceiverConstants.ROLE_COLOR);
                    }

                    /* 停电只禁止感知者反向看见观察者；恢复供电后由服务端重新累计  秒凝视。 */
                    if (gameWorld.isRole(viewer, NoellesRoleRegistry.PERCEIVER)
                            && !BlackoutApi.isActive(viewer.getWorld())
                            && PerceiverPlayerComponent.KEY.get(viewer).senses(targetPlayer.getUuid())) {
                        return InstinctApi.HighlightResult.color(PerceiverConstants.ROLE_COLOR);
                    }
                    return InstinctApi.HighlightResult.pass();
                }
        );
    }
}
