package org.agmas.noellesroles.client.instinct.roles.conductor;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.entity.ItemEntity;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 乘务员的掉落物提示。
 * 这是客户端本能描边，不改变服务端物品逻辑；配置通过世界组件同步，默认关闭。
 */
public final class ConductorInstinctHandler {
    private ConductorInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("conductor_dropped_items"),
                NoellesInstinctHandlers.PRIORITY_ABILITY_MARK,
                (viewer, target) -> {
                    if (!(target instanceof ItemEntity)
                            || !ConfigWorldComponent.KEY.get(viewer.getWorld()).conductorDroppedItemInstinct
                            || !GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.CONDUCTOR)
                            || !WatheClient.isPlayerAliveAndInSurvival()) {
                        return InstinctApi.HighlightResult.pass();
                    }
                    return InstinctApi.HighlightResult.color(0xDB9D00);
                }
        );
    }
}
