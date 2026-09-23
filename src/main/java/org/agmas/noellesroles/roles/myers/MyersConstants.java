package org.agmas.noellesroles.roles.myers;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 迈尔斯的全部玩法常量。
 *
 * <p>蓄力、冲刺、恶意值、恶灵附身和客户端表现都从这里读取，
 * 避免服务端判定与 HUD/音效使用不同数字。</p>
 */
public final class MyersConstants {
    /** 迈尔斯职业颜色 RGB(90,55,195)。 */
    public static final int ROLE_COLOR = 0x5A37C3;
    /** 迈尔斯最多生成一名，避免恶灵附身与范围音乐出现多源重叠。 */
    public static final int MAX_ROLE_COUNT = 1;

    /** 屠刀有效蓄力的最短时间：0.5 秒。 */
    public static final int BUTCHER_KNIFE_MIN_CHARGE_TICKS = GameConstants.getInTicks(0, 0) + 10;
    /** 屠刀有效蓄力的最大节点：2.5 秒，超过后封顶。 */
    public static final int BUTCHER_KNIFE_MAX_CHARGE_TICKS = GameConstants.getInTicks(0, 2) + 10;
    /** 屠刀释放后冲刺时长相对有效蓄力时间的倍率。 */
    public static final double BUTCHER_KNIFE_DASH_DURATION_MULTIPLIER = 2.5D;
    /** 屠刀冲刺速度是当前基础疾跑速度的倍率。 */
    public static final float BUTCHER_KNIFE_DASH_SPEED_MULTIPLIER = 3.2F;
    /** 屠刀冲刺结束后的物品冷却：2 秒。 */
    public static final int BUTCHER_KNIFE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 2);
    /** 释放瞬间保存的屠刀准心目标最大距离，沿用 Wathe 匕首 3 格范围。 */
    public static final double BUTCHER_KNIFE_TARGET_RANGE = 3.0D;
    /** 冲刺每 tick 的最大推进距离，作为碰撞检测的稳定步长。 */
    public static final double BUTCHER_KNIFE_DASH_STEP_BLOCKS = 0.55D;
    /** 冲刺玩家碰撞盒额外扩展，防止高速移动穿过目标。 */
    public static final double BUTCHER_KNIFE_COLLISION_EXPANSION = 0.35D;
    /** 恶灵附身冲刺结束后的缓慢 III 持续时间。 */
    public static final int EVIL_POSSESS_DASH_SLOWNESS_TICKS = GameConstants.getInTicks(0, 2);
    /** Minecraft 状态效果等级从 0 开始，缓慢 III 对应 2。 */
    public static final int EVIL_POSSESS_DASH_SLOWNESS_AMPLIFIER = 2;

    /** 恶意值货币 path，完整 id 为 noellesroles:malice_value。 */
    public static final String MALICE_CURRENCY_PATH = "malice_value";
    /** 恶意值货币完整 id。 */
    public static final Identifier MALICE_CURRENCY_ID = NoellesRolesCore.id(MALICE_CURRENCY_PATH);
    /** 恶意值字体私用区图标。 */
    public static final String MALICE_CURRENCY_ICON = "\uE784";
    /** 恶意值被动增加间隔：5 秒。 */
    public static final int PASSIVE_MALICE_INTERVAL_TICKS = GameConstants.getInTicks(0, 5);
    /** 每次被动增加的恶意值。 */
    public static final int PASSIVE_MALICE_AMOUNT = 1;
    /** 每个完成任务增加的恶意值。 */
    public static final int TASK_MALICE_AMOUNT = 15;
    /** 每个确认击杀增加的恶意值。 */
    public static final int KILL_MALICE_AMOUNT = 30;
    /** 恶灵附身价格：初始参与存活人数乘以该倍率。 */
    public static final int EVIL_POSSESS_PRICE_PER_PLAYER = 30;

    /** 恶灵附身持续时间：45 秒。 */
    public static final int EVIL_POSSESS_DURATION_TICKS = GameConstants.getInTicks(0, 60);
    /** 恶灵附身商店图标冷却：4 分 05 秒。 */
    public static final int EVIL_POSSESS_COOLDOWN_TICKS = GameConstants.getInTicks(4, 5);
    /** 恶灵附身范围音乐半径。 */
    public static final double EVIL_POSSESS_MUSIC_RADIUS = 32.0D;
    /** 恶灵附身范围音乐淡入时长：2 秒。 */
    public static final int EVIL_POSSESS_MUSIC_FADE_IN_TICKS = GameConstants.getInTicks(0, 2);
    /** 恶灵附身范围音乐淡出时长：2 秒。 */
    public static final int EVIL_POSSESS_MUSIC_FADE_OUT_TICKS = GameConstants.getInTicks(0, 2);
    /** 恶灵附身音乐基础音量。 */
    public static final float EVIL_POSSESS_MUSIC_VOLUME = 1.0F;

    /** G 键恶意吸收距离。 */
    public static final double ABSORB_RANGE_BLOCKS = 64.0D;
    /** 普通吸收每个目标每秒提供 1 点恶意值。 */
    public static final int ABSORB_MALICE_PER_TARGET = 1;
    /** 蹲下吸收每个目标每秒提供 2 点恶意值。 */
    public static final int ABSORB_SNEAKING_MALICE_PER_TARGET = 2;
    /** 吸收状态每秒结算一次。 */
    public static final int ABSORB_INTERVAL_TICKS = 20;
    /** HUD 目标数量最多每 5 tick 同步一次，减少网络噪声。 */
    public static final int ABSORB_TARGET_SYNC_INTERVAL_TICKS = 5;
    /** 蓄力状态同步间隔，避免每 tick 发送组件包。 */
    public static final int CHARGE_SYNC_INTERVAL_TICKS = 5;
    /** 屠刀释放目标的准心角度余弦阈值。 */
    public static final double BUTCHER_KNIFE_TARGET_DOT_MIN = 0.985D;
    /** 吸收目标视锥角度余弦阈值。 */
    public static final double ABSORB_VIEW_DOT_MIN = 0.35D;
    /** 自定义透明门射线每格采样次数。 */
    public static final double LINE_OF_SIGHT_SAMPLES_PER_BLOCK = 8.0D;
    /** 范围音乐实例启动时的最小非零音量。 */
    public static final float MUSIC_MIN_START_VOLUME = 0.01F;
    /** 恶灵附身一次性音效音量。 */
    public static final float EVIL_POSSESS_SOUND_VOLUME = 1.0F;
    /** 恶灵附身一次性音效音高。 */
    public static final float EVIL_POSSESS_SOUND_PITCH = 1.0F;
    /** 恶灵护盾内部保持的最小层数，Wathe 随后扣除一层后仍显示 1 层。 */
    public static final int EVIL_POSSESS_MIN_INTERNAL_ARMOUR = 2;

    /** 屠刀冲刺速度修正优先级。 */
    public static final int DASH_SPEED_PRIORITY = 3000;

    private MyersConstants() {
    }
}
