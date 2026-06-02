package org.agmas.noellesroles.mixin.roles.spiritualist;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 调用玩家内部的原版姿态刷新流程。
 *
 * <p>灵术师附身时如果只手写 {@code setPose(CROUCHING/STANDING)}，
 * 很容易和原版自身的低姿态判定打架：
 * 1. 一格高 / 木板门压低 / 游泳姿态会被硬掰回去；
 * 2. 潜行、起身、跳跃的相机与动画过渡会显得很突兀；
 * 3. 服务端和客户端看到的姿态切换节奏也更容易不同步。
 *
 * <p>因此这里直接借一个 invoker，附身宿主时尽量让原版自己决定当前该是什么姿态。</p>
 */
@Mixin(PlayerEntity.class)
public interface PlayerEntityPoseInvoker {

    @Invoker("updatePose")
    void noellesroles$invokeUpdatePose();
}
