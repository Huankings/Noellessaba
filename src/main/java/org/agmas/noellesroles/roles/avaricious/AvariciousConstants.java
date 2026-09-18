package org.agmas.noellesroles.roles.avaricious;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 扒手全部玩法数值。
 *
 * <p>这些默认值来自 StupidExpress 的实际代码实现，而不是旧 guidebook 文案。
 * 搬运到 NoellesRoles 后统一放在常量类里，避免再读取 StupidExpress config 或散落魔法数字。</p>
 */
public final class AvariciousConstants {
    private AvariciousConstants() {
    }

    public static final int ROLE_COLOR = 0x8f00ff;
    public static final int TIMER_TICKS = GameConstants.getInTicks(1, 0);
    public static final double MAX_DISTANCE = 8.5D;
    public static final int STARTING_BALANCE = 50;
    public static final int PAYOUT_PER_PLAYER = 30;

    /**
     * 给 StarryExpress 绿皮书读取用的运行时入口。
     * 直接跨模组引用 public static final 字段可能被 Java 编译器内联，方法调用能读取当前 NoellesRoles jar 的真实数值。
     */
    public static double guidebookMaxDistance() {
        return MAX_DISTANCE;
    }

    /** 同上，保持绿皮书的每人收益说明与 NoellesRoles 当前逻辑一致。 */
    public static int guidebookPayoutPerPlayer() {
        return PAYOUT_PER_PLAYER;
    }

    /**
     * 扒手服务端结算和客户端预计收益必须使用完全相同的存活定义。
     * Wathe 允许扩展为 spectator/creative 玩家授予“玩法仍存活”的覆盖状态，
     * 因此这里同时使用存活入口和互补的非存活入口，避免 HUD 与实际发钱人数不一致。
     */
    public static boolean isEligibleParticipant(PlayerEntity player) {
        return player != null
                && GameFunctions.isPlayerAliveAndSurvival(player)
                && !GameFunctions.isPlayerSpectatingOrCreative(player);
    }
}
