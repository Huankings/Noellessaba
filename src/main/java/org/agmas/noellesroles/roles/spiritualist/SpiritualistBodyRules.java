package org.agmas.noellesroles.roles.spiritualist;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 灵术师脱体本体的碰撞 / 选中统一规则。
 *
 * <p>Wathe 会在 {@code Entity#collidesWith} 里把局内存活玩家强制当作实体墙处理，
 * 而原版推挤还会走 {@code pushAwayFrom} / {@code LivingEntity#pushAway}。
 * 如果这些入口各自写判断，就很容易出现“碰撞被取消了，但轻微推挤还在”或
 * “服务端能穿过，客户端预测又挡住”的半套状态。因此灵术师本体是否应被当作空气，
 * 统一从这里读取。</p>
 */
public final class SpiritualistBodyRules {
    private SpiritualistBodyRules() {
    }

    public static boolean shouldIgnorePlayerBodyCollision(Entity self, Entity other) {
        return self instanceof PlayerEntity
                && other instanceof PlayerEntity
                && (isDetachedBody(self) || isDetachedBody(other));
    }

    public static boolean isDetachedBody(Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return false;
        }

        if (SpiritualistPlayerComponent.KEY.get(player).hasDetachedBodyState()) {
            return true;
        }

        /*
         * 客户端主动移动时，碰撞预测会在普通玩家自己的客户端先跑一遍。
         * CCA 状态通常会同步给追踪者，但网络时序上仍可能晚于实体 tracked flag；
         * 附身空气壳会同时设置 invisible + noGravity，这两个都是实体同步标记，
         * 用它们做一层窄兜底，可以避免普通玩家客户端把“已经隐藏的灵术师本体”继续当作实体墙。
         */
        return player.isInvisible() && player.hasNoGravity();
    }

    public static boolean isPossessingBody(Entity entity) {
        return entity instanceof PlayerEntity player
                && SpiritualistPlayerComponent.KEY.get(player).isPossessing();
    }
}
