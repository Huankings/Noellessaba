package org.agmas.noellesroles.roles.party_animal;

import dev.doctor4t.wathe.game.GameConstants;

/** 派对狂所有可调数值的集中定义。 */
public final class PartyAnimalConstants {
    /** 派对狂职业颜色：RGB(250, 50, 200)。 */
    public static final int ROLE_COLOR = 0xFA32C8;
    /** Harpy 随机职业池中派对狂的最大生成数。 */
    public static final int MAX_ROLE_COUNT = 1;
    /** 派对狂能力的准心有效范围，单位为方块。 */
    public static final double TARGET_RANGE_BLOCKS = 2.5D;
    /** 标记保留时间，单位为 tick。 */
    public static final int MARK_DURATION_TICKS = GameConstants.getInTicks(0, 25);
    /** 标记超时或失效后的能力冷却，单位为 tick。 */
    public static final int MARK_FAILURE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 15);
    /** 成功释放变声后的能力冷却，单位为 tick。 */
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    /** 变声等级的最大绝对值。 */
    public static final int MAX_LEVEL = 3;
    /** 每次升调一级时使用的语音倍率。 */
    public static final float PITCH_UP_LEVEL_1 = 1.35F;
    /** 每次升调二级时使用的语音倍率。 */
    public static final float PITCH_UP_LEVEL_2 = 1.55F;
    /** 每次升调三级时使用的语音倍率。 */
    public static final float PITCH_UP_LEVEL_3 = 1.75F;
    /** 麦克风流空闲达到一秒后丢弃旧 PCM 缓冲，单位为纳秒，使用单调时钟而非游戏 tick。 */
    public static final long VOICE_STREAM_IDLE_RESET_NANOS = 1_000_000_000L;
    /** 升调一级的被动透视颜色。 */
    public static final int PITCH_UP_COLOR_1 = 0xDC78D2;
    /** 升调二级的被动透视颜色。 */
    public static final int PITCH_UP_COLOR_2 = 0xC832C8;
    /** 升调三级的被动透视颜色。 */
    public static final int PITCH_UP_COLOR_3 = 0xE60064;
    /** 降调一级的被动透视颜色。 */
    public static final int PITCH_DOWN_COLOR_1 = 0x9600DC;
    /** 降调二级的被动透视颜色。 */
    public static final int PITCH_DOWN_COLOR_2 = 0x6400C8;
    /** 降调三级的被动透视颜色。 */
    public static final int PITCH_DOWN_COLOR_3 = 0x2800C8;
    /** 一级变声击杀额外金币。 */
    public static final int LEVEL_1_REWARD_COINS = 25;
    /** 二级变声击杀额外金币。 */
    public static final int LEVEL_2_REWARD_COINS = 50;
    /** 三级变声击杀额外金币。 */
    public static final int LEVEL_3_REWARD_COINS = 75;
    /** 匕首和左轮在一级变声目标上的冷却倍率。 */
    public static final double LEVEL_1_COOLDOWN_MULTIPLIER = 2.0D / 3.0D;
    /** 匕首和左轮在二级变声目标上的冷却倍率。 */
    public static final double LEVEL_2_COOLDOWN_MULTIPLIER = 1.0D / 3.0D;
    /** 三级变声目标的武器冷却倍率。 */
    public static final double LEVEL_3_COOLDOWN_MULTIPLIER = 0.0D;

    private PartyAnimalConstants() {
    }
}
