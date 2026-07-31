package org.agmas.noellesroles.roles;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * 按本局杀手人数缩放职业数值的共享工具。
 *
 * <p>狂信者的疯魔护盾和梦者的开局梦之印记都使用同一条平衡规则：
 * “1 个杀手时使用初始值，之后每多 1 个杀手追加固定增量，并受到最大值限制”。</p>
 */
public final class KillerCountScaling {
    private KillerCountScaling() {
    }

    /**
     * 读取本局杀手数量。
     *
     * <p>开局分配期间，Wathe/Harpy 会先把原版杀手位写进 {@link GameWorldComponent}，
     * 再陆续替换成扩展杀手和中立职业；因此只要角色表里已经有杀手，就优先相信角色表。
     * 这里统计的是“本局杀手位总数”，不是当前存活杀手数，因为梦者的开局物品
     * 和狂信者的护盾平衡都应该跟本局配置有关，而不是被中途死亡动态削弱。
     * 如果是在极早期或调试环境里尚未写入杀手，则退回到 Harpy 当前使用的
     * ready player count / killer dividend 公式，保证梦者这类开局发物品逻辑不会因为分配顺序读到 0。</p>
     */
    public static int resolveKillerCount(@NotNull PlayerEntity player) {
        World world = player.getWorld();
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);

        int assignedKillerCount = gameWorld.getAllKillerTeamPlayers().size();
        if (assignedKillerCount > 0) {
            return assignedKillerCount;
        }

        int killerDividend = Math.max(1, gameWorld.getKillerDividend());
        return (int) Math.floor((float) GameFunctions.getReadyPlayerCount(world) / (float) killerDividend);
    }

    /**
     * 按“1 个杀手为基准”计算动态数值。
     *
     * <p>这里把杀手数至少按 1 处理，是为了兼容强制职业、小人数调试局或异常配置：
     * 即使没有读到杀手，也仍然使用“只有 1 个杀手时”的初始值，而不是让护盾/物品数量变成负数。</p>
     *
     * @param initialValue         1 个杀手时的基础数值
     * @param valuePerExtraKiller  每额外增加 1 个杀手追加的数值
     * @param maximumValue         最终允许达到的最大值
     */
    public static int scaleFromSingleKiller(@NotNull PlayerEntity player, int initialValue, int valuePerExtraKiller, int maximumValue) {
        int effectiveKillerCount = Math.max(1, resolveKillerCount(player));
        int extraKillers = Math.max(0, effectiveKillerCount - 1);
        int scaledValue = initialValue + extraKillers * valuePerExtraKiller;
        return Math.max(0, Math.min(maximumValue, scaledValue));
    }
}
