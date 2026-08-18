package org.agmas.noellesroles.roles.shadow_jester;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 影子小丑职业常量。
 *
 * <p>除职业 RGB 外，所有会影响机制、时长、距离、数量、冷却和客户端表现的数值都集中在这里，
 * 方便后续只改本类就能调平衡。</p>
 */
public final class ShadowJesterConstants {
    /** 职业颜色：RGB(200, 65, 120)，同时用于 HUD、回放字样、本能高亮和胜利色。 */
    public static final int ROLE_COLOR = 0xC84178;

    /** 无限体力沿用 Wathe 职业注册里的 -1 约定。 */
    public static final int MAX_SPRINT_TIME_TICKS = -1;

    /** 至少有 2 个杀手位时，影子小丑才允许进入随机生成池。 */
    public static final int MIN_KILLER_SLOTS_FOR_PAIR = 2;

    /** Harpy 只随机抽 1 个影子小丑，第二个由本职业分配规则从平民阵营补齐。 */
    public static final int MAX_RANDOM_PRIMARY_COUNT = 1;

    /** 开局填满 Wathe 当前的心情任务槽位：默认 3 个随机任务。 */
    public static final int INITIAL_TASK_COUNT = 3;

    /** 每名影子小丑独立完成 4 个任务后进入第二阶段。 */
    public static final int REQUIRED_COMPLETED_TASKS = 4;

    /** 任务槽出现空位后，最短 35 秒后补发下一项随机任务。 */
    public static final int TASK_REFILL_MIN_TICKS = GameConstants.getInTicks(0, 35);

    /** 任务槽出现空位后，最长 70 秒后补发下一项随机任务。 */
    public static final int TASK_REFILL_MAX_TICKS = GameConstants.getInTicks(1, 10);

    /** 缔结申请持续 6 秒，超时后双方收到过期提示。 */
    public static final int VOW_REQUEST_DURATION_TICKS = GameConstants.getInTicks(0, 6);

    /** 缔结申请和同意时，准心目标必须在 2 格内。 */
    public static final double VOW_TARGET_RANGE_BLOCKS = 2.0D;

    /** 准心距离用平方比较时的 2 格平方值，避免每次开根号。 */
    public static final double VOW_TARGET_RANGE_SQUARED = VOW_TARGET_RANGE_BLOCKS * VOW_TARGET_RANGE_BLOCKS;

    /** 第四阶段影子小丑左轮冷却固定为 4 秒。 */
    public static final int PHASE_FOUR_REVOLVER_COOLDOWN_TICKS = GameConstants.getInTicks(0, 4);

    /**
     * 调试开关：未缔结誓言前，另一半离线、主动切创造/旁观或其它“没有明确死因但不再存活”的状态是否触发转狂信。
     * true 为正式机制：第一/第二阶段一方不再活跃，另一方立即转狂信并清任务；false 时只接受死亡流程里的明确死因触发。
     */
    public static final boolean CONVERT_TO_JESTER_WHEN_EARLY_PARTNER_MISSING = false;

    /**
     * 调试开关：缔结誓言后，另一半离线、主动切创造/旁观或其它“没有明确死因但不再存活”的状态是否触发殉情。
     * true 为正式机制：第三/四阶段一方不再活跃，另一方立即 broken_heart；false 时只接受死亡流程里的明确死因触发。
     */
    public static final boolean KILL_BOUND_PARTNER_WHEN_PARTNER_MISSING = false;

    /** 阶段回放里第二阶段的本地化 key。 */
    public static final String PHASE_TWO_TEXT_KEY = "replay.stage.noellesroles.shadow_jester.phase2";

    /** 阶段回放里第二阶段定义“一念之间”的本地化 key。 */
    public static final String PHASE_TWO_DEFINITION_KEY = "replay.stage.noellesroles.shadow_jester.phase2.definition";

    /** 阶段回放里第三阶段的本地化 key。 */
    public static final String PHASE_THREE_TEXT_KEY = "replay.stage.noellesroles.shadow_jester.phase3";

    /** 阶段回放里第三阶段定义“缔结誓言”的本地化 key。 */
    public static final String PHASE_THREE_DEFINITION_KEY = "replay.stage.noellesroles.shadow_jester.phase3.definition";

    /** 阶段回放里第四阶段的本地化 key。 */
    public static final String PHASE_FOUR_TEXT_KEY = "replay.stage.noellesroles.shadow_jester.phase4";

    /** 阶段回放里第四阶段定义“谢幕时刻”的本地化 key。 */
    public static final String PHASE_FOUR_DEFINITION_KEY = "replay.stage.noellesroles.shadow_jester.phase4.definition";

    /** 谢幕音乐基础音量，客户端淡入淡出会围绕该值变化。 */
    public static final float MUSIC_BASE_VOLUME = 1.4F;

    /** 谢幕音乐播放音高，固定为原速播放。 */
    public static final float MUSIC_PITCH = 1.0F;

    /** 客户端音乐淡入/淡出的步长，20 tick 约 1 秒完成淡入。 */
    public static final float MUSIC_FADE_STEP = 0.05F;

    private ShadowJesterConstants() {
    }
}
