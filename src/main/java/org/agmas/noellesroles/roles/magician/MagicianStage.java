package org.agmas.noellesroles.roles.magician;

/**
 * 魔术师主动技能当前所处阶段。
 *
 * <p>之所以单独拆成枚举，而不是到处散落布尔值，是因为魔术师的阶段切换比较多：
 * 1. 冷却结束待录制；
 * 2. 录制中；
 * 3. 录制完成待播放；
 * 4. 播放中；
 * 5. 播放结束重新回到冷却。
 *
 * <p>拆开后 HUD、能力键、服务端 tick 和回放清理都能共用同一份语义，后续维护会稳很多。</p>
 */
public enum MagicianStage {
    /**
     * 当前不在录制也不在播放。
     * 是否真正“可以开始录制”，还要继续看 AbilityPlayerComponent 的冷却是否为 0。
     */
    IDLE,
    /**
     * 正在录制。
     */
    RECORDING,
    /**
     * 录制已完成，等待玩家再次按能力键开始播放。
     */
    READY_PLAYBACK,
    /**
     * 正在播放录制内容。
     */
    PLAYING
}
