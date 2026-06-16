package org.agmas.noellesroles.roles.magician;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 一次“皮套被玩法武器命中”时需要统一带入播放结束与尸体生成链的信息。
 *
 * <p>之所以单独抽成对象，是因为这次收束会同时喂给：</p>
 * <p>1. 播放管理器的阶段收束与 replay 全局事件；</p>
 * <p>2. 尸体生成时的死因覆盖；</p>
 * <p>3. 回放文案里“来自谁、用什么武器”的参数展示。</p>
 */
public record MagicianPlaybackHitInfo(
        @Nullable UUID attackerUuid,
        @Nullable String attackerName,
        String weaponName,
        Identifier deathReason
) {
}
