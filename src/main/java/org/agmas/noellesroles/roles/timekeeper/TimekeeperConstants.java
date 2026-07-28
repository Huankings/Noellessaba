package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 时停者职业常量。
 *
 * <p>本职业牵涉到货币、物品冷却、快照采样、商店与 HUD 多条链路。
 * 所有玩法数值都集中在这里，后续平衡时只需要改这一处，
 * 避免“某个数写死在物品 / HUD / 回溯逻辑里”导致表现和服务端判定不一致。</p>
 */
public final class TimekeeperConstants {
    /** 时停者职业色，来源于需求 RGB(44, 150, 221)。 */
    public static final int ROLE_COLOR = 0x2C96DD;

    /** 光阴货币 id 的 path，完整 id 为 noellesroles:time。 */
    public static final String TIME_CURRENCY_PATH = "time";

    /** 光阴货币的完整注册 id。 */
    public static final Identifier TIME_CURRENCY_ID = NoellesRolesCore.id(TIME_CURRENCY_PATH);

    /** 光阴货币在字体表里对应的私用区字符。 */
    public static final String TIME_CURRENCY_ICON = "\uE783";

    /** 光阴被动收入的间隔：每 5 秒结算一次。 */
    public static final int PASSIVE_TIME_INCOME_INTERVAL_TICKS = GameConstants.getInTicks(0, 5);

    /** 每次光阴被动收入发放的数量。 */
    public static final int PASSIVE_TIME_INCOME_AMOUNT = 5;

    /** 时停者完成一个任务时额外获得的光阴数量。金币仍走 NoellesRoles 通用 50 金币。 */
    public static final int TASK_TIME_INCOME_AMOUNT = 10;

    /** 物品加速与技能加速的光阴消耗。 */
    public static final int ACCELERATE_TIME_COST = 60;

    /** 时间回溯的光阴消耗。 */
    public static final int REWIND_TIME_COST = 120;

    /** 普通濒毁怀表使用物品加速 / 技能加速后的冷却。 */
    public static final int NORMAL_ACCELERATE_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);

    /** 升级为精致怀表后，物品加速 / 技能加速使用后的冷却。 */
    public static final int ELEGANT_ACCELERATE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 40);

    /** 普通濒毁怀表发动时间回溯后的冷却；普通表成功回溯后仍会破碎。 */
    public static final int NORMAL_REWIND_COOLDOWN_TICKS = GameConstants.getInTicks(2, 0);

    /** 精致怀表发动时间回溯后的冷却。 */
    public static final int ELEGANT_REWIND_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);

    /** 时间回溯右键蓄力要求：按满 3 秒才真正触发回溯。 */
    public static final int REWIND_CHARGE_TICKS = GameConstants.getInTicks(0, 3);

    /** 快照采样频率：每 4 tick 存一张，也就是每秒 5 张。 */
    public static final int SNAPSHOT_INTERVAL_TICKS = 4;

    /** 历史缓存长度：保留 120 秒历史，作为多次回溯衔接的冗余前缀。 */
    public static final int HISTORY_CACHE_SECONDS = 120;

    /** 单次回溯深度：每次最多回到 30 秒前。 */
    public static final int SINGLE_REWIND_DEPTH_SECONDS = 30;

    /** 历史缓存最多保存的快照张数，按“120 秒 * 每秒 5 张”计算。 */
    public static final int MAX_HISTORY_SNAPSHOTS = HISTORY_CACHE_SECONDS * 20 / SNAPSHOT_INTERVAL_TICKS;

    /** 单次回溯实际倒放的快照张数，按“30 秒 * 每秒 5 张”计算。 */
    public static final int SINGLE_REWIND_SNAPSHOTS = SINGLE_REWIND_DEPTH_SECONDS * 20 / SNAPSHOT_INTERVAL_TICKS;

    /** 回溯期间给未受保护玩家反复刷新冻结输入的短冷却，避免组件自然倒计时提前解冻。 */
    public static final int REWIND_FREEZE_REFRESH_TICKS = 5;

    /** 时间狭缝持续时间，与单次回溯深度保持一致：30 秒。 */
    public static final int RIFT_DURATION_TICKS = GameConstants.getInTicks(0, SINGLE_REWIND_DEPTH_SECONDS);

    /** 怀表准心下方进度条宽度，单位为客户端 GUI 像素。 */
    public static final int WATCH_CROSSHAIR_BAR_WIDTH = 22;

    /** 怀表准心下方进度条高度，单位为客户端 GUI 像素。 */
    public static final int WATCH_CROSSHAIR_BAR_HEIGHT = 3;

    /** 怀表进度条相对屏幕中心的 Y 偏移，避免覆盖 Wathe 原准心。 */
    public static final int WATCH_CROSSHAIR_BAR_Y_OFFSET = 10;

    /** 怀表进度条背景色，半透明黑色用于兼容明暗场景。 */
    public static final int WATCH_CROSSHAIR_BAR_BACKGROUND_COLOR = 0xAA000000;

    /** 怀表进度条填充色，使用时停者职业色并带不透明 alpha。 */
    public static final int WATCH_CROSSHAIR_BAR_FILL_COLOR = 0xFF2C96DD;

    /** 回溯保护商店价格，使用 Wathe 金币。 */
    public static final int REWIND_PROTECTION_PRICE = 150;

    /** 损坏怀表修复为普通濒毁怀表需要的金币。 */
    public static final int REPAIR_WATCH_PRICE = 250;

    /** 普通濒毁怀表升级为精致怀表需要的金币。 */
    public static final int UPGRADE_WATCH_PRICE = 250;

    /** 时停者最大生成数量，避免一局出现多个全局回溯源互相踩状态。 */
    public static final int MAX_ROLE_COUNT = 1;

    private TimekeeperConstants() {
    }
}
