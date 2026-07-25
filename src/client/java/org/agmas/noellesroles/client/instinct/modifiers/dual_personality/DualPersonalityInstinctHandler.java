package org.agmas.noellesroles.client.instinct.modifiers.dual_personality;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityClientState;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityConstants;

public final class DualPersonalityInstinctHandler {
    private static final int OTHER_PLAYER_COLOR = 0x8a8a8a;

    private DualPersonalityInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("dual_personality_double_active"), NoellesInstinctHandlers.PRIORITY_DUAL_PERSONALITY, viewer -> {
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && DualPersonalityClientState.isDoubleActive(viewer)
                    && WatheClient.isInstinctInputActive()) {
                /*
                 * 双重人格双活期间用本能键进入专属透视。
                 * 双活 pair 可能在一方死亡后短暂保留，因此这里必须先确认 viewer 仍是局内存活玩家。
                 * 否则死亡后的旁观者会继续以双重人格身份开启本能，压过 Harpy 的观察者职业色。
                 * 这里只给资格，不直接染色；真正颜色由下面的 highlight 规则负责。
                 */
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("dual_personality_targets"), NoellesInstinctHandlers.PRIORITY_DUAL_PERSONALITY, (viewer, target) -> {
            if (!GameFunctions.isPlayerAliveAndSurvival(viewer)
                    || !DualPersonalityClientState.isDoubleActive(viewer)
                    || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }
            if (!(target instanceof PlayerEntity targetPlayer) || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 这里必须检查 WatheClient.isInstinctEnabled()，而不是只看本能输入。
             * 这样 Convener 召集后的变形压制返回 DISABLE 时，双活本能也会一起被压住。
             * 但 viewer 死亡后不再属于双活可行动玩家，即使世界组件还没清 pair，也必须让观察者规则接管。
             */
            DualPersonalityComponent dualComponent = DualPersonalityComponent.KEY.get(viewer.getWorld());
            if (targetPlayer.getUuid().equals(dualComponent.getPartner(viewer.getUuid()))) {
                return InstinctApi.HighlightResult.color(DualPersonalityConstants.COLOR);
            }
            return InstinctApi.HighlightResult.color(OTHER_PLAYER_COLOR);
        });
    }
}
