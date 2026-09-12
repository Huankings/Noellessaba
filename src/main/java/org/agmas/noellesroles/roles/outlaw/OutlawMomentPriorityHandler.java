package org.agmas.noellesroles.roles.outlaw;

import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainMomentManager;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterManager;

/**
 * 亡命时刻对影子小丑第四阶段和执照恶棍时刻的优先级协调器。
 *
 * <p>跨职业调用放在亡命徒包内，是因为“谁需要让位”由最高优先级的亡命时刻拥有。
 * 各低优先级职业仍各自负责自己的状态、物品和冷却回收。</p>
 */
public final class OutlawMomentPriorityHandler {
    private OutlawMomentPriorityHandler() {
    }

    public static void onOutlawStarted(ServerWorld world) {
        /* 先撤销服务端能力和临时物品，客户端随后会依据同步状态立即切换音乐。 */
        ShadowJesterManager.pausePhaseFourForOutlaw(world);
        LicensedVillainMomentManager.pauseAllForOutlaw(world);
    }

    public static void onOutlawEnded(ServerWorld world) {
        if (OutlawWorldComponent.KEY.get(world).hasActiveOutlaw()) {
            return;
        }

        /*
         * 恢复顺序固定为影子小丑优先、执照恶棍其次。
         * 执照恶棍自己的重算还会检查是否存在存活影子小丑，因此不会与谢幕时刻并播。
         */
        ShadowJesterManager.resumePhaseFourAfterOutlaw(world);
        LicensedVillainMomentManager.reconcileNow(world);
    }

    public static void reconcile(ServerWorld world) {
        if (OutlawWorldComponent.KEY.get(world).hasActiveOutlaw()) {
            onOutlawStarted(world);
        } else {
            onOutlawEnded(world);
        }
    }
}
