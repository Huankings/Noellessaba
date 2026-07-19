package org.agmas.noellesroles.mixin.roles.waiter;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 访问 Wathe 的私有 PlayerMoodComponent#completeTask。
 *
 * <p>服务员不能只手动删任务，否则会漏掉 Wathe 自带的心情回复、同步、任务完成事件和任务收入链路。
 * 通过 invoker 调原方法可以尽量复用 Wathe 原行为，只在收入处用 WaiterTaskIncomeMixin 做特殊修正。</p>
 */
@Mixin(PlayerMoodComponent.class)
public interface PlayerMoodComponentAccessor {
    @Invoker("completeTask")
    void noellesroles$completeTask(PlayerMoodComponent.Task taskType, boolean rewardMood);
}
