package org.agmas.noellesroles.modifiers;

import dev.doctor4t.wathe.api.event.GameEvents;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityManager;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;

/** 恋人与双重人格世界级回合状态的公开生命周期接入。 */
public final class RoundModifierResetHandler {
    private RoundModifierResetHandler() {
    }

    public static void init() {
        GameEvents.ON_BEGIN_INITIALIZE.register((world, game) -> {
            /*
             * 这两份组件会参与新局词条配对、存活判定和 HUD，因此必须早于任何职业/词条分配清理。
             * ON_FINISH_INITIALIZE 已经太晚；Wathe 的 ON_BEGIN_INITIALIZE 正是为此时序提供的公开入口。
             */
            LoversPairComponent.KEY.get(world).clear();
            DualPersonalityComponent.KEY.get(world).clear();
        });

        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                /* 结算画面结束后再清副人格实体/语音等收尾状态，保留原有展示时序。 */
                DualPersonalityManager.clearRoundState(serverWorld);
            }
        });
    }
}
