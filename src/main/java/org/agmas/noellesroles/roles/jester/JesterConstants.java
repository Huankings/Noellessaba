package org.agmas.noellesroles.roles.jester;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.KillerCountScaling;
import org.jetbrains.annotations.NotNull;

/**
 * 狂信者职业常量。
 *
 * <p>疯魔持续时间和护盾缩放数据都收束在这里，避免 profile 注册处继续出现散落魔法数字。</p>
 */
public final class JesterConstants {
    public static final int PSYCHO_DURATION_TICKS = GameConstants.getInTicks(0, 48);
    public static final int INVULNERABLE_END_TICKS = GameConstants.getInTicks(0, 44);

    /*
     * 狂信者被好人杀死时是否启动疯魔。
     * true：保持当前默认玩法，好人击杀狂信者会取消这次死亡并进入疯魔；
     * false：关闭反制触发，好人击杀狂信者时不再启动疯魔，死亡链会继续执行并让狂信者直接死亡。
     * 这里做成常量，是为了后续只改这一处 true/false 就能切换玩法，不需要碰死亡保护链的具体实现。
     */
    public static final boolean TRIGGER_PSYCHO_WHEN_KILLED_BY_INNOCENT = true;

    /*
     * 狂信者疯魔护盾的动态数据：
     * 1 个杀手时 0 层；每多 2 个杀手增加 1 层；最多 3 层。
     * 真正启动疯魔时由 getPsychoShieldLayers(...) 读取本局杀手位数量并套用这三个常量。
     */
    public static final int INITIAL_PSYCHO_SHIELD_LAYERS = 0;
    public static final int PSYCHO_SHIELD_LAYERS_PER_EXTRA_KILLER = 2;
    public static final int MAX_PSYCHO_SHIELD_LAYERS = 3;

    private JesterConstants() {
    }

    /**
     * 计算当前这一局狂信者启动疯魔时应获得的护盾层数。
     *
     * <p>这个方法只返回数值，不直接改 Wathe 的 psycho 组件；
     * 写入组件的动作统一交给 {@link JesterPsychoHandler} 的 profile provider，
     * 这样不会绕过 Wathe API 的物品、HUD、回放和同步流程。</p>
     */
    public static int getPsychoShieldLayers(@NotNull PlayerEntity player) {
        return KillerCountScaling.scaleFromSingleKiller(
                player,
                INITIAL_PSYCHO_SHIELD_LAYERS,
                PSYCHO_SHIELD_LAYERS_PER_EXTRA_KILLER,
                MAX_PSYCHO_SHIELD_LAYERS
        );
    }
}
