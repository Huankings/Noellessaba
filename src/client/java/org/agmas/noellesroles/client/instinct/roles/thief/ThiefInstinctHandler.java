package org.agmas.noellesroles.client.instinct.roles.thief;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.thief.ThiefAllayEntity;
import org.agmas.noellesroles.roles.thief.ThiefConstants;

import java.awt.Color;

public final class ThiefInstinctHandler {
    private ThiefInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("thief_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.THIEF)
                    && WatheClient.isInstinctInputActive()) {
                /*
                 * 小偷本能只在存活时开启。
                 * 非存活玩家的物品/玩家描边要走观察者规则，避免被小偷灰色逻辑覆盖。
                 */
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("thief_targets"), NoellesInstinctHandlers.PRIORITY_ROLE_INSTINCT_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (!WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }

            if (target instanceof ThiefAllayEntity) {
                /*
                 * 存活小偷仍以灰色查看自己的悦灵；普通死亡旁观/创造观察者则以小偷职业色查看，
                 * 方便观察悦灵跨楼层绕路、偷窃和返程。Wathe 默认 availability 已负责让非存活玩家
                 * 在开启本能键时进入描边链，因此这里只需要补目标颜色，不需要修改 Wathe。
                 */
                if (GameFunctions.isPlayerSpectatingOrCreative(viewer)) {
                    return InstinctApi.HighlightResult.color(NoellesRoleRegistry.THIEF.color());
                }
                if (gameWorld.isRole(viewer, NoellesRoleRegistry.THIEF)
                        && GameFunctions.isPlayerAliveAndSurvival(viewer)) {
                    return InstinctApi.HighlightResult.color(ThiefConstants.ALLAY_INSTINCT_COLOR);
                }
                return InstinctApi.HighlightResult.pass();
            }

            if (!gameWorld.isRole(viewer, NoellesRoleRegistry.THIEF)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || (!(target instanceof PlayerEntity)
                    && !(target instanceof ItemEntity))) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 小偷本能只关心玩家和物品：自己显示盗贼色，其余目标统一灰色。
             * 这条规则完全依赖本能开启，因此会被 Convener availability DISABLE 正确压住。
             */
            return InstinctApi.HighlightResult.color(target == viewer
                    ? NoellesRoleRegistry.THIEF.color()
                    : Color.GRAY.getRGB());
        });
    }
}
