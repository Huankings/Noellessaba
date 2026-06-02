package org.agmas.noellesroles.roles.assassin;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;

/**
 * 刺刀左键击退的统一处理器。
 *
 * <p>这层单独抽出来有两个目的：</p>
 * <p>1. 普通 attack mixin 与自定义 C2S 数据包共用同一套判定，避免两边数值漂移；</p>
 * <p>2. 对真实玩家额外补发一次速度同步包，确保服务端击退一定能体现在客户端视角里。</p>
 */
public final class BayonetKnockbackHandler {

    private static final float BAYONET_KNOCKBACK_STRENGTH = 0.85F;

    private BayonetKnockbackHandler() {
    }

    /**
     * 判断当前是否满足刺刀左键击退条件。
     */
    public static boolean canKnockback(@NotNull PlayerEntity attacker, Entity target) {
        return attacker.getMainHandStack().isOf(ModItems.BAYONET)
                && target instanceof PlayerEntity targetPlayer
                && GameFunctions.isPlayerAliveAndSurvival(attacker)
                && GameFunctions.isPlayerAliveAndSurvival(targetPlayer);
    }

    /**
     * 对目标玩家施加刺刀击退。
     *
     * <p>这里不再单纯依赖 {@link PlayerEntity#takeKnockback(double, double, double)}，
     * 因为测试里已经出现“carpet 假人能退、真实玩家不退”的差异。
     * 因此这里直接按原版击退公式手动写速度，并对真实玩家显式补发速度同步包。</p>
     */
    public static void applyKnockback(@NotNull PlayerEntity attacker, @NotNull PlayerEntity target) {
        /*
         * Wathe 在“玩家掉出列车区域”时，并不会回看是谁最后推了他，
         * 而是直接读取 target.getLastAttacker() 来决定：
         * 1. 击杀归因算不算在别人头上；
         * 2. 回放文本走 ".killed" 还是 ".died"。
         *
         * 原版普通攻击会在伤害链里自动维护这个字段，
         * 但刺刀左键现在是“纯击退、无伤害”的自定义路径，
         * 因此必须手动把攻击者登记进去，否则掉出列车时就会被当成“自己摔下去”。
         */
        target.setAttacker(attacker);

        double x = attacker.getX() - target.getX();
        double z = attacker.getZ() - target.getZ();

        // 两人几乎重合时补一个极小随机偏移，避免击退向量退化为 0。
        if (x * x + z * z < 1.0E-4D) {
            x = (attacker.getRandom().nextDouble() - 0.5D) * 0.01D;
            z = (attacker.getRandom().nextDouble() - 0.5D) * 0.01D;
        }

        Vec3d pushDirection = new Vec3d(x, 0.0D, z).normalize().multiply(BAYONET_KNOCKBACK_STRENGTH);
        Vec3d currentVelocity = target.getVelocity();
        double nextY = target.isOnGround()
                ? Math.min(0.4D, currentVelocity.y * 0.5D + BAYONET_KNOCKBACK_STRENGTH)
                : currentVelocity.y;

        target.setVelocity(
                currentVelocity.x * 0.5D - pushDirection.x,
                nextY,
                currentVelocity.z * 0.5D - pushDirection.z
        );
        target.velocityModified = true;

        /*
         * 关键补丁：
         * 对真实联机玩家，额外发一次速度更新包，避免客户端沿用自己的移动预测，
         * 从而表现成“服务端其实已经改了速度，但屏幕上完全看不出被击退”。
         */
        if (target instanceof ServerPlayerEntity serverTarget) {
            serverTarget.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverTarget));
        }
    }
}
