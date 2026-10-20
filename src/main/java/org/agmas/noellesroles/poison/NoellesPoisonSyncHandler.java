package org.agmas.noellesroles.poison;

import dev.doctor4t.wathe.api.sync.PlayerComponentSyncApi;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.bartender.BartenderPoisonSyncHandler;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerPoisonSyncHandler;
import org.agmas.noellesroles.roles.physician.PhysicianPoisonSyncHandler;

/**
 * NoellesRoles 对 Wathe 毒药组件选择性观察规则的聚合入口。
 *
 * <p>制毒师、医师和酒保的本能 provider 都在客户端读取“目标玩家”的
 * {@link PlayerPoisonComponent}。Wathe 为降低网络占用后，毒药组件默认只同步给中毒者本人，
 * 因此这些职业必须显式登记为额外观察者；否则观察者客户端上的目标组件会一直保持默认 -1，
 * 本能透视就无法识别真实中毒目标。</p>
 *
 * <p>这里只维护职业 handler 的注册顺序和转职后的统一刷新，不直接堆放职业判断。
 * 后续新增毒药观察职业时，应在 {@code roles/<role>/} 新建对应的
 * {@code <RoleName>PoisonSyncHandler}，再从这里调用，避免聚合类逐渐变成大类。</p>
 */
public final class NoellesPoisonSyncHandler {
    private static boolean initialized = false;

    private NoellesPoisonSyncHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DrugmakerPoisonSyncHandler.init();
        PhysicianPoisonSyncHandler.init();
        BartenderPoisonSyncHandler.init();
    }

    /**
     * 局中调试转职时，目标毒药组件本身可能没有刚好发生变化。
     * 此时主动补发一次当前追踪目标，避免新获得职业的客户端要等到下一次中毒事件才看到透视。
     */
    public static void syncTrackedTargetsAfterRoleAssigned(ServerPlayerEntity recipient) {
        PlayerComponentSyncApi.syncTrackedToRecipient(PlayerPoisonComponent.KEY, recipient);
    }
}
