package org.agmas.noellesroles.roles.insane_damned_paranoid_killer;

/**
 * 亡语杀手尸体伪装相关常量。
 *
 * <p>尸体伪装按 spark 版默认机制做成无限开关，不设置持续时间和冷却；
 * 这里集中保存真正会影响玩法手感的数值，后续平衡时只需要改这一处。</p>
 */
public final class InsaneDamnedKillerConstants {
    /**
     * 尸体伪装期间的移动速度倍率。
     *
     * <p>spark 版用缓慢效果把速度压低；当前自改版要求走 Wathe 的公开移动速度 API，
     * 所以这里直接声明为“最终速度的一半”。</p>
     */
    public static final float CORPSE_SPEED_MULTIPLIER = 0.5F;

    /**
     * 移速修正优先级。
     *
     * <p>移动 API 是按优先级从高到低累计应用的。这里刻意放低，
     * 让其它职业 / 词条的加速或覆盖速度先结算，最后再把尸体伪装的半速压上去，
     * 更接近“无论原本多快，躺尸时都只剩一半速度”的直觉。</p>
     */
    public static final int CORPSE_MOVEMENT_PRIORITY = -1000;

    /**
     * 玩家碰撞规则优先级。
     *
     * <p>尸体伪装需要比普通玩家实体墙更强：只要一方处于尸体模式，
     * 就应该完全取消移动碰撞和原版推挤。</p>
     */
    public static final int CORPSE_COLLISION_PRIORITY = 1500;

    /**
     * 准心目标 / 交互隐藏优先级。
     *
     * <p>这个规则要早于普通目标规则，避免其它较低优先级规则把伪装尸体重新声明为可选中，
     * 导致枪械 / 匕首准心变成“已锁定目标”的贴图而暴露。</p>
     */
    public static final int CORPSE_TARGET_VISIBILITY_PRIORITY = 1500;

    private InsaneDamnedKillerConstants() {
    }
}
