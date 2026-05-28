package org.agmas.noellesroles.roles.bomber;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 炸弹客陷阱相关的通用工具。
 *
 * <p>当前只保留“把一枚预埋炸弹真正挂到目标玩家身上”这一层共用逻辑。
 * 至于托盘 / 床的状态保存与触发搜索，现已分别迁移到 wathe 的公共 tray / bed 接口里。</p>
 */
public final class BomberTrapUtils {

    private BomberTrapUtils() {
    }

    /**
     * 尝试把一枚预埋炸弹挂到受害者身上。
     * 只有受害者当前存活、处于游戏中、且身上还没有活动炸弹时才会成功。
     */
    public static boolean tryAttachTimedBomb(PlayerEntity target, @Nullable UUID bomberUuid) {
        if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
            return false;
        }

        BomberPlayerComponent component = BomberPlayerComponent.KEY.get(target);
        if (component.hasBomb()) {
            return false;
        }

        component.placeBomb(bomberUuid);
        return true;
    }
}
