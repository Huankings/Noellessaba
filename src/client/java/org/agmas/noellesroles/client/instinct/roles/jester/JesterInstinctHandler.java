package org.agmas.noellesroles.client.instinct.roles.jester;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;

import java.awt.Color;

public final class JesterInstinctHandler {
    private JesterInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("jester_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (gameWorld.isRole(viewer, Noellesroles.JESTER) && WatheClient.isInstinctInputActive()) {
                /*
                 * 狂信者不是杀手，但它自己拥有一套本能键透视。
                 * 这里保持 priority 0，表示它和 Wathe 默认杀手本能平级；
                 * StupidExpress 的 Convener 压制会以更高优先级返回 DISABLE，所以仍能压住这条资格。
                 */
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("jester_color"), InstinctApi.DEFAULT_PRIORITY, (viewer, target) -> {
            if (target instanceof PlayerEntity
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, Noellesroles.JESTER)
                    && WatheClient.isInstinctEnabled()) {
                /*
                 * 狂信者自己的本能透视只依赖 WatheClient.isInstinctEnabled()。
                 * 因此一旦上层 availability 被 Convener 或其它高优先级规则禁用，这里会自然失效。
                 */
                return InstinctApi.HighlightResult.color(Color.PINK.getRGB());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
