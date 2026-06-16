package org.agmas.noellesroles.mixin.roles.magician;

import net.minecraft.nbt.NbtCompound;
import org.agmas.noellesroles.roles.magician.MagicianReplayActorContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 给 wathe 的通用回放事件统一注入“皮套身份上下文”。
 *
 * <p>这里直接收口在 {@code GameRecordManager.addEvent(...)} 上，而不是分别改
 * recordItemUse / recordItemHit / recordDoorInteraction / recordDeath 等多个方法，
 * 原因是魔术师播放期间触发的所有事件最终都会汇聚到这里。
 *
 * <p>当前处理策略是：
 * 1. 继续保留真实魔术师归属在 {@code magician_owner}；
 * 2. 额外写入 {@code replay_actor}/{@code replay_actor_name} 方便专用 formatter 使用；
 * 3. 把通用 {@code actor} 直接覆写成皮套身份，让 wathe 默认 formatter 也自然显示成皮套。
 */
@Mixin(targets = "dev.doctor4t.wathe.record.GameRecordManager")
public abstract class MagicianReplayRecordMixin {

    @ModifyVariable(
            method = "addEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/record/GameRecordManager$MatchRecord;addEvent(Ljava/lang/String;JJLnet/minecraft/nbt/NbtCompound;)V"
            ),
            ordinal = 0
    )
    private static NbtCompound noellesroles$injectReplayActorIntoPayload(NbtCompound payload) {
        MagicianReplayActorContext.ReplayActorInfo replayActorInfo = MagicianReplayActorContext.current();
        if (replayActorInfo == null) {
            return payload;
        }

        NbtCompound result = payload.copy();
        if (replayActorInfo.magicianOwner() != null) {
            result.putUuid("magician_owner", replayActorInfo.magicianOwner());
        }
        if (replayActorInfo.replayActor() != null) {
            result.putUuid("replay_actor", replayActorInfo.replayActor());
            result.putUuid("actor", replayActorInfo.replayActor());
        }
        if (replayActorInfo.replayActorName() != null && !replayActorInfo.replayActorName().isBlank()) {
            result.putString("replay_actor_name", replayActorInfo.replayActorName());
        }
        return result;
    }
}
