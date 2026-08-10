package org.agmas.noellesroles.roles.spiritualist;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 灵术师脱体本体的碰撞 / 选中统一规则。
 *
 * <p>玩家物理部分已经通过 Wathe {@code PlayerCollisionApi} 接入；
 * 可见、选中、交互和攻击部分通过 Wathe {@code TargetVisibilityApi} 接入。
 * 这些 API 的 handler 都从这里读取同一份“灵术师本体是否处于脱体状态”判断，
 * 避免服务端能穿过、客户端预测又挡住，或看不见但仍能被推挤的半套状态。</p>
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
