package org.agmas.noellesroles.roles.dreamer;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.KillerCountScaling;
import org.jetbrains.annotations.NotNull;

/**
 * 梦者职业常量。
 *
 * <p>从 kinssaba 迁移时，原本散落在配置文件里的玩法数值收束到这里；
 * 这样 NoellesRoles 不需要反向依赖 kinssaba 的 config，也方便后续单独调整梦者。</p>
 */
public final class DreamerConstants {
    public static final int ROLE_COLOR = 0xE5CCFF;

    /*
     * 梦者开局梦之印记的动态数据：
     * 1 个杀手时 0 个；每多 1 个杀手增加 1 个；最多 3 个。
     * 注意这里控制的是“梦者开局拿到多少个 DreamImprintItem”，
     * 不是目标身上的梦之印记护盾层数；印记物品本身仍然是一次性的一层保护。
     */
    public static final int INITIAL_DREAM_IMPRINT_COUNT = 0;
    public static final int DREAM_IMPRINT_COUNT_PER_EXTRA_KILLER = 1;
    public static final int MAX_DREAM_IMPRINT_COUNT = 3;
    public static final int BECOME_KILLER_REWARD_COINS = 100;
    public static final int REQUIRED_PLAYER_DIVISOR = 4;

    /**
     * 幻觉注剂的目标检测范围。
     *
     * <p>客户端准心、客户端发包和服务端最终校验都必须使用同一个常量，
     * 避免出现“准心已经锁定，但服务端认为超出范围”的不同步。</p>
     */
    public static final float DELUSION_SYRINGE_TARGET_RANGE = 3.0F;

    /**
     * 幻觉注剂正常成功注射后的冷却。
     */
    public static final int DELUSION_SYRINGE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);

    /**
     * 梦者开局拿到幻觉注剂时的初始冷却。
     *
     * <p>它和普通注射冷却的实际时长不同，因此还需要由 DreamerKillerComponent
     * 同步“当前冷却来源”给客户端，供 tooltip 正确显示 30 秒。</p>
     */
    public static final int DELUSION_SYRINGE_START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);

    /**
     * 物品可以长时间保持举起状态。
     *
     * <p>Minecraft 的使用计时仍然需要一个有限值；72000 tick 约等于 1 小时，
     * 对单局游戏来说等同于没有蓄力时限，同时避免使用 Integer.MAX_VALUE 造成计时溢出。</p>
     */
    public static final int DELUSION_SYRINGE_MAX_USE_TICKS = 72000;

    private DreamerConstants() {
    }

    /**
     * 计算梦者分配职业时应该拿到的梦之印记数量。
     *
     * <p>职业分配发生在服务端开局流程中；这里只计算数量，
     * 真正给物品仍放在 {@link DreamerRoleAssignedHandler}，
     * 这样重置组件、设置转化进度和发物品的顺序保持集中可读。</p>
     */
    public static int getInitialDreamImprintCount(@NotNull PlayerEntity player) {
        return KillerCountScaling.scaleFromSingleKiller(
                player,
                INITIAL_DREAM_IMPRINT_COUNT,
                DREAM_IMPRINT_COUNT_PER_EXTRA_KILLER,
                MAX_DREAM_IMPRINT_COUNT
        );
    }
}
