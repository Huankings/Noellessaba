package org.agmas.noellesroles.mixin.roles.magician;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.roles.magician.MagicianReplayActorContext;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    /**
     * 时间回溯期间只是在播放历史快照，不是在现实时间线上发生新事件。
     * 快照恢复会让“发光/镇静/隐身/标记”等组件反复跨过结束边界；如果仍允许它们写回放，
     * 同一条结束事件就会每个历史帧重复记录并实时刷屏。所有 Noelles/Wathe 回放最终都会
     * 汇聚到 GameRecordManager.addEvent，因此在这里统一丢弃回溯期间产生的新事件。
     */
    @Inject(method = "addEvent", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$suppressEventsDuringRewind(
            ServerWorld world,
            String type,
            ServerPlayerEntity actor,
            ServerPlayerEntity target,
            NbtCompound data,
            CallbackInfo ci
    ) {
        /*
         * 回溯最终帧收束时允许 recordRoleCorrection 写入职业时间线屏障；
         * 它不是“历史帧自然结束事件”，不能被抑制，否则回放职业缓存会重新沿用旧未来时间线。
         */
        boolean rewindRoleCorrection = data != null && data.getBoolean("rewind_role_restore");
        boolean timekeeperWatchEvent = data != null && isTimekeeperWatchEvent(data.getString("event"));
        if (TimekeeperWorldComponent.KEY.get(world).isRewinding()
                && !rewindRoleCorrection
                && !timekeeperWatchEvent) {
            ci.cancel();
        }
    }

    private static boolean isTimekeeperWatchEvent(String eventId) {
        return NoellesEventIds.TIMEKEEPER_WATCH_USED_EVENT.toString().equals(eventId)
                || NoellesEventIds.TIMEKEEPER_WATCH_BROKEN_EVENT.toString().equals(eventId)
                || NoellesEventIds.TIMEKEEPER_WATCH_REPAIRED_EVENT.toString().equals(eventId)
                || NoellesEventIds.TIMEKEEPER_WATCH_UPGRADED_EVENT.toString().equals(eventId);
    }

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
