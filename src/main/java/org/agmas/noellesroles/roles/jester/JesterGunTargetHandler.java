package org.agmas.noellesroles.roles.jester;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.GunTargetResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 假左轮不应在客户端选中玩家目标。
 *
 * <p>服务端仍会把客户端发送的 id 重新校验；这里仅保留假枪“开火不指向玩家”的视觉和交互语义。</p>
 */
public final class JesterGunTargetHandler {
    private static boolean initialized = false;

    private JesterGunTargetHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerTargetRule(
                NoellesRolesCore.id("fake_revolver_target"),
                GunShotApi.DEFAULT_PRIORITY,
                /*
                 * 假左轮只改客户端选目标：玩家右键时仍会有开火动作，但发送给服务端的是 -1。
                 * 服务端没有可击杀目标，自然不会触发 Wathe 的左轮杀人、反火或掉枪逻辑。
                 */
                context -> context.stack().isOf(ModItems.FAKE_REVOLVER)
                        ? GunTargetResult.miss()
                        : GunTargetResult.pass()
        );
    }
}
