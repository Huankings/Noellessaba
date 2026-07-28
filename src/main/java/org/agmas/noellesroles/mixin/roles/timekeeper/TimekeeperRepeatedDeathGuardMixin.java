package org.agmas.noellesroles.mixin.roles.timekeeper;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.death.DeathProcessComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperRiftHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 吞掉时间狭缝期间的重复死亡。
 *
 * <p>时间狭缝不是免死判定，而是“已经死亡后的 30 秒回溯窗口”。
 * 第一次死亡必须完整走完 Wathe 流程，之后才由 {@link TimekeeperDeathRiftMixin} 拉入狭缝；
 * 但进入狭缝后，玩家会被 {@code PlayerLifeStateApi} 维持成 Wathe 意义上的特殊存活旁观。
 * 这会让跌出列车、精神崩溃、范围武器旧目标快照等逻辑继续把他当成可击杀目标。</p>
 *
 * <p>因此这里用高优先级 HEAD 注入，在任何尸体、掉落物、金币/时间奖励、回放或语音频道副作用前，
 * 直接取消“已经在狭缝中”的第二次及后续 {@code killPlayer}。
 * 判断只看时停者自己的 {@code inTimeRift} 标记，不碰双重人格休眠人格/双活的特殊存活状态，
 * 避免和双重人格的死亡拦截互相抢逻辑。</p>
 */
@Mixin(value = GameFunctions.class, priority = 10000)
public abstract class TimekeeperRepeatedDeathGuardMixin {
    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void noellesroles$suppressRepeatedDeathInTimeRift(
            PlayerEntity victim,
            boolean spawnBody,
            @Nullable PlayerEntity killer,
            Identifier deathReason,
            CallbackInfo ci
    ) {
        if (!(victim instanceof ServerPlayerEntity serverVictim)
                || !TimekeeperRiftHandler.shouldSuppressRepeatedDeathInRift(serverVictim)) {
            return;
        }

        /*
         * 某些连锁死亡逻辑会在 killPlayer 入口处设置 DeathProcessComponent，
         * 正常情况下由 GameFunctionsDeathProcessMixin 在 RETURN 清掉。
         * 本 guard 会直接取消方法、不会走到 RETURN，所以这里主动清理一次，
         * 避免被吞掉的重复死亡让“正在处理死亡”的标记残留到后续 tick。
         */
        DeathProcessComponent.KEY.get(serverVictim).setProcessing(false);
        ci.cancel();
    }
}
