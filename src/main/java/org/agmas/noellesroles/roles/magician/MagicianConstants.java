package org.agmas.noellesroles.roles.magician;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 魔术师职业相关的统一常量。
 *
 * <p>后续如果你要继续调平衡，优先改这里即可：
 * 1. 开局冷却；
 * 2. 录制时长；
 * 3. 播放后冷却；
 * 4. 皮套与动作记录的一些兼容参数。</p>
 */
public final class MagicianConstants {

    /**
     * 魔术师职业颜色。
     */
    public static final int ROLE_COLOR = (107 << 16) | (23 << 8) | 176;

    /**
     * 开局第一次可录制前，需要等待 30 秒。
     */
    public static final int INITIAL_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);

    /**
     * 一次录制的最长时长：30 秒。
     */
    public static final int RECORD_DURATION_TICKS = GameConstants.getInTicks(0, 30);

    /**
     * 一次播放结束后进入 90 秒冷却。
     */
    public static final int PLAYBACK_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);

    /**
     * 其他玩家强制打碎魔术师皮套时，补偿给魔术师的金币数。
     *
     * <p>如果是魔术师自己打碎自己的皮套，则不发奖励，避免自刷经济。</p>
     */
    public static final int PLAYBACK_FORCED_END_REWARD_COINS = 50;

    /**
     * 播放皮套默认使用玩家实体的实体追踪范围。
     *
     * <p>这里保守采用 92 格，已经足够覆盖大多数列车地图的可见区间。</p>
     */
    public static final int PLAYBACK_TRACKING_RANGE = 92;

    /**
     * 皮套在服务端每 tick 都强制保持 noClip。
     * 这能尽量保证轨迹复刻不会被门、玩家、狭窄地形卡住。
     */
    public static final boolean PLAYBACK_NO_CLIP = true;

    /**
     * 能力键服务端去抖时间。
     *
     * <p>魔术师的同一个 G 键同时负责“开始播放”和“提前结束播放”，
     * 如果客户端因为键盘重复、帧边界或网络重发在极短时间内发来两次包，
     * 第二次包就会把刚开始的播放立刻结束。这里在服务端再兜一层，
     * 防止“一闪而过”的播放被自己的连续按键误关。</p>
     */
    public static final int ABILITY_KEY_DEBOUNCE_TICKS = 4;

    /**
     * 孤儿皮套清理保护时间。
     *
     * <p>刚创建的实体可能还处于“服务端刚生成、追踪器刚同步”的首几个 tick。
     * 如果因为异常没有进入播放表，也给它一个很短的缓冲窗口；
     * 这样不会误删正常启动中的皮套，同时旧残留依然会很快被清掉。</p>
     */
    public static final int ORPHAN_CLEANUP_GRACE_TICKS = 10;

    private MagicianConstants() {
    }
}
